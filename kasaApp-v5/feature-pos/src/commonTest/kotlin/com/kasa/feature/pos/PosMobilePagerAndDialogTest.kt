package com.kasa.feature.pos

import com.kasa.model.Product
import com.kasa.model.TaxGroup
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тести мобільних горизонтальних переходів (HorizontalPager),
 * діалогів акцизу та відкладання чеків з примітками для смартфонів Android та iOS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosMobilePagerAndDialogTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val sampleProduct = Product(
        id = 301L,
        name = "Латте великий",
        price = 65.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "порц",
        barcodes = listOf("4820000003011")
    )

    private val alcoholProduct = Product(
        id = 302L,
        name = "Коньяк 0.5л",
        price = 220.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "пляш",
        barcodes = listOf("4820000003022"),
        uktzed = "2208201200"
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transport = SimulatedKasaTransport()
        receiptEngine = FiscalReceiptEngine(transport)
        serviceEngine = FiscalServiceEngine(transport)
        viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(sampleProduct, alcoholProduct)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCompactPagerTabMapping() {
        // Сторінка 0: Каталог, Сторінка 1: Чек
        assertEquals(0, PosCompactTab.CATALOG.ordinal)
        assertEquals(1, PosCompactTab.CART.ordinal)
        assertEquals("CATALOG", PosCompactTab.CATALOG.name)
        assertEquals("CART", PosCompactTab.CART.name)
    }

    @Test
    fun testHoldReceiptWithQuickMobileNote() = runTest {
        // Касир набиває чек на смартфоні
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct, 2.0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(13000L, viewModel.state.value.totalPayableKopecks)

        // Відкриває діалог відкладання чека з приміткою "Стіл №2"
        viewModel.onEvent(PosEvent.HoldCurrentReceipt("Стіл №2"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Поточний кошик очищено
        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(0L, viewModel.state.value.totalPayableKopecks)

        // Чек збережено у відкладених
        assertEquals(1, viewModel.state.value.heldReceipts.size)
        val held = viewModel.state.value.heldReceipts[0]
        assertEquals("Стіл №2", held.note)
        assertEquals(1, held.receipt.items.size)
        assertEquals(301L, held.receipt.items[0].productId)

        // Відновлення відкладеного чека
        viewModel.onEvent(PosEvent.RestoreHeldReceipt(held.id))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value.heldReceipts.size)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(13000L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testExciseDiiaQRAndPaperStampValidation() = runTest {
        // Додаємо алкоголь в чек
        viewModel.onEvent(PosEvent.AddProduct(alcoholProduct, 1.0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        val item = viewModel.state.value.items[0]
        assertTrue(item.requiresExciseStamp)

        // Без акцизу оплата заблокована
        assertEquals(false, viewModel.state.value.canPay)

        // Введення тестової е-марки Дія
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "https://t.gov.ua/AA99887766"))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedItem = viewModel.state.value.items[0]
        assertEquals("https://t.gov.ua/AA99887766", updatedItem.exciseStamp)
        assertTrue(viewModel.state.value.canPay)
    }

    @Test
    fun testBottomCatalogBarDataContract() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct, 3.0))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        // 3 * 65.0 = 195.0 грн = 19500 коп
        assertEquals(19500L, state.totalPayableKopecks)

        val formattedSum = PosCurrencyFormatter.formatKopecks(state.totalPayableKopecks)
        assertEquals("195.00", formattedSum)
    }
}
