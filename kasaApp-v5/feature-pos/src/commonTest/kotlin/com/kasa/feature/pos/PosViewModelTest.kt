package com.kasa.feature.pos

import com.kasa.model.PaymentType
import com.kasa.model.Product
import com.kasa.model.TaxGroup
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val bread = Product(
        id = 1L,
        name = "Хліб Житній",
        price = 24.53, // 2453 коп
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000001")
    )

    private val vodka = Product(
        id = 2L,
        name = "Горілка 0.5л",
        price = 105.00,
        taxGroup = TaxGroup.GROUP_A_EXCISE,
        uktzed = "2208600000"
    )

    private val apples = Product(
        id = 3L,
        name = "Яблука Чемпіон",
        price = 30.00,
        isWeighted = true,
        taxGroup = TaxGroup.GROUP_A
    )

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        transport = SimulatedKasaTransport()
        transport.connect("SIMULATED_DATECS")
        receiptEngine = FiscalReceiptEngine(transport)
        serviceEngine = FiscalServiceEngine(transport)
        viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread, vodka, apples)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddProductUpdatesTotalsAndCashRounding() {
        viewModel.onEvent(PosEvent.AddProduct(bread))

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals(2453L, state.rawTotalKopecks)
        // 2453 коп при готівці округлюється НБУ до 2450 коп (-3 коп)
        assertEquals(2450L, state.totalPayableKopecks)
        assertEquals(-3L, state.roundingDifferenceKopecks)
        assertTrue(state.canPay)

        // Перемикаємо на картку — округлення вимикається
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        val cardState = viewModel.state.value
        assertEquals(2453L, cardState.totalPayableKopecks)
        assertEquals(0L, cardState.roundingDifferenceKopecks)
    }

    @Test
    fun testWeightedProductUsesWeightFromScale() {
        // Симулюємо потік зі стабільною вагою 1.500 кг
        viewModel.onEvent(PosEvent.ReceiveScaleMessage("ST,GS,+  1.500kg\r\n"))
        assertEquals(1500, viewModel.state.value.scaleWeight?.weightGrams)

        // Додаємо ваговий товар: 30.00 грн * 1.5 кг = 45.00 грн (4500 коп)
        viewModel.onEvent(PosEvent.AddProduct(apples))

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals(1.5, state.items[0].quantity)
        assertEquals(4500L, state.rawTotalKopecks)
    }

    @Test
    fun testExciseProductPromptsForStampAndBlocksPayment() {
        viewModel.onEvent(PosEvent.AddProduct(vodka))

        val state = viewModel.state.value
        assertEquals(0, state.activeExcisePromptIndex)
        assertTrue(state.hasUnstampedExcise)
        assertFalse(state.canPay, "Unstamped excise item must block payment")

        // Вводимо марку акцизного податку
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "EX98765432"))

        val stampedState = viewModel.state.value
        assertNull(stampedState.activeExcisePromptIndex)
        assertFalse(stampedState.hasUnstampedExcise)
        assertTrue(stampedState.canPay)
        assertEquals("EX98765432", stampedState.items[0].exciseStamp)
    }

    @Test
    fun testSuccessfulFiscalizationClearsReceipt() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(bread))

        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.items.isEmpty())
        assertEquals(0L, state.totalPayableKopecks)
        assertFalse(state.isFiscalProcessing)
        assertNotNull(state.lastFiscalCheckNumber)
        assertEquals(1, state.journalReceipts.size)
        assertEquals("3000123456", state.journalReceipts[0].fiscalRegisterNumber)
    }

    @Test
    fun testScanBarcodeSuccessIncrementsQuantity() {
        // Перше сканування хліба
        viewModel.onEvent(PosEvent.ScanBarcode("482000000001"))
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1.0, viewModel.state.value.items[0].quantity)

        // Повторне сканування того самого штрих-коду збільшує кількість
        viewModel.onEvent(PosEvent.ScanBarcode("482000000001"))
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(2.0, viewModel.state.value.items[0].quantity)
        assertEquals(4906L, viewModel.state.value.rawTotalKopecks) // 2 * 24.53 грн = 4906 коп
    }

    @Test
    fun testScanBarcodeNotFoundShowsMessage() {
        viewModel.onEvent(PosEvent.ScanBarcode("999999999999"))
        assertEquals(0, viewModel.state.value.items.size)
        assertNotNull(viewModel.state.value.statusMessage)
        assertTrue(viewModel.state.value.statusMessage!!.contains("не знайдено"))
    }

    @Test
    fun testToggleServiceDialog() {
        assertFalse(viewModel.state.value.isServiceDialogOpen)
        viewModel.onEvent(PosEvent.ToggleServiceDialog)
        assertTrue(viewModel.state.value.isServiceDialogOpen)
        viewModel.onEvent(PosEvent.ToggleServiceDialog)
        assertFalse(viewModel.state.value.isServiceDialogOpen)
    }

    @Test
    fun testReadKasaTime() = runTest {
        viewModel.onEvent(PosEvent.ReadKasaTime)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.kasaRtcTime)
        assertEquals("25-09-26 12:00:00", viewModel.state.value.kasaRtcTime)
    }

    @Test
    fun testSyncKasaTime() = runTest {
        viewModel.onEvent(PosEvent.SyncKasaTime)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.kasaRtcTime)
        assertTrue(viewModel.state.value.statusMessage!!.contains("успішно синхронізовано"))
    }

    @Test
    fun testQueryKasaHardwareInfo() = runTest {
        viewModel.onEvent(PosEvent.QueryKasaHardwareInfo)
        testDispatcher.scheduler.advanceUntilIdle()

        val info = viewModel.state.value.kasaHardwareInfo
        assertNotNull(info)
        assertTrue(info.contains("DP05123456"))
    }

    @Test
    fun testCashInOut() = runTest {
        viewModel.onEvent(PosEvent.PerformCashInOut(50000L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.statusMessage)
        assertTrue(viewModel.state.value.statusMessage!!.contains("успішно"))
    }

    @Test
    fun testOpenCashDrawer() = runTest {
        viewModel.onEvent(PosEvent.OpenCashDrawer)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun testToggleJournalDialog() {
        assertFalse(viewModel.state.value.isJournalDialogOpen)
        viewModel.onEvent(PosEvent.ToggleJournalDialog)
        assertTrue(viewModel.state.value.isJournalDialogOpen)
        viewModel.onEvent(PosEvent.ToggleJournalDialog)
        assertFalse(viewModel.state.value.isJournalDialogOpen)
    }

    @Test
    fun testUpdateCatalog() {
        val newProduct = Product(id = 99L, name = "Цукор", price = 32.00)
        viewModel.onEvent(PosEvent.UpdateCatalog(listOf(newProduct)))
        assertEquals(1, viewModel.state.value.catalogProducts.size)
        assertEquals("Цукор", viewModel.state.value.catalogProducts[0].name)
    }

    @Test
    fun testSetReceiptType() {
        assertEquals(com.kasa.model.ReceiptType.SALE, viewModel.state.value.receiptType)
        viewModel.onEvent(PosEvent.SetReceiptType(com.kasa.model.ReceiptType.RETURN))
        assertEquals(com.kasa.model.ReceiptType.RETURN, viewModel.state.value.receiptType)
    }
}
