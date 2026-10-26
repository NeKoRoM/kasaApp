package com.kasa.feature.pos

import com.kasa.model.BarcodeParser
import com.kasa.model.Product
import com.kasa.model.TaxGroup
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelBarcodeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val bread = Product(
        id = 1L,
        name = "Хліб Житній",
        price = 25.00,
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000001")
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
            initialCatalog = listOf(bread, apples)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testScanStandardBarcodeIncrementsQuantity() {
        viewModel.onEvent(PosEvent.ScanBarcode("482000000001"))
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1.0, viewModel.state.value.items[0].quantity)
        assertEquals(2500L, viewModel.state.value.rawTotalKopecks)

        // Повторне сканування штучного товару збільшує кількість
        viewModel.onEvent(PosEvent.ScanBarcode("482000000001"))
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(2.0, viewModel.state.value.items[0].quantity)
        assertEquals(5000L, viewModel.state.value.rawTotalKopecks)
    }

    @Test
    fun testScanWeightedScaleBarcodeExtractsPluAndWeight() {
        // Формуємо етикетку торгових ваг:
        // Префікс 21, PLU 00003 (Яблука), вага 01450 (1.450 кг)
        val first12 = "210000301450"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(first12)
        val scaleBarcode = "$first12$checkDigit"

        viewModel.onEvent(PosEvent.ScanBarcode(scaleBarcode))

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        val item = state.items[0]
        assertEquals("Яблука Чемпіон", item.productName)
        assertEquals(1.450, item.quantity)
        // 30.00 грн * 1.450 кг = 43.50 грн (4350 коп)
        assertEquals(4350L, item.totalKopecks)
        assertEquals(4350L, state.rawTotalKopecks)
    }

    @Test
    fun testScanUnknownScaleBarcodeShowsStatusMessage() {
        val first12 = "210999901000"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(first12)
        val unknownBarcode = "$first12$checkDigit"

        viewModel.onEvent(PosEvent.ScanBarcode(unknownBarcode))

        assertEquals(0, viewModel.state.value.items.size)
        assertNotNull(viewModel.state.value.statusMessage)
        assertTrue(viewModel.state.value.statusMessage!!.contains("не знайдено"))
    }
}
