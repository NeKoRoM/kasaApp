package com.kasa.feature.pos

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
import kotlin.test.*

/**
 * Тести сканування штрих-кодів, розпізнавання е-акцизу та взаємодії зі сканером при відкритому редагуванні.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelBarcodeScanningTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val standardProduct = Product(
        id = 101L,
        name = "Батончик горіховий",
        price = 20.00,
        barcodes = listOf("4820000001011"),
        taxGroup = TaxGroup.GROUP_A,
        isWeighted = false,
        unitOfMeasurement = "шт"
    )

    private val exciseProduct = Product(
        id = 202L,
        name = "Вино сухе 0.75л",
        price = 150.00,
        barcodes = listOf("4820000002025"),
        taxGroup = TaxGroup.GROUP_A_EXCISE,
        uktzed = "2204210000"
    )

    @BeforeTest
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        transport.connect("SIMULATED_TEST")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testScanningWhileProductEditOpen_updatesPrefilledBarcode() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(standardProduct)
        )

        // Відкриваємо діалог створення нового товару
        viewModel.onEvent(PosEvent.PromptProductEdit(null, null))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isProductEditDialogOpen)
        assertNull(viewModel.state.value.prefilledBarcode)

        // Касир сканує штрих-код фізичним сканером
        viewModel.onEvent(PosEvent.ScanBarcode("4829999991234"))
        advanceUntilIdle()

        // Діалог залишається відкритим, а штрих-код передається у поле
        assertTrue(viewModel.state.value.isProductEditDialogOpen)
        assertEquals("4829999991234", viewModel.state.value.prefilledBarcode)
        assertTrue(viewModel.state.value.statusMessage?.contains("4829999991234") == true)
    }

    @Test
    fun testSpaceSeparatedMultiplierShortcut() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(standardProduct)
        )

        // Сканування формату: "кількість код" через пробіл (наприклад: "5 101" або "3 4820000001011")
        viewModel.onEvent(PosEvent.ScanBarcode("5 101"))
        advanceUntilIdle()

        val items = viewModel.state.value.items
        assertEquals(1, items.size)
        assertEquals(101L, items.first().productId)
        assertEquals(5.0, items.first().quantity)
        assertEquals(10000L, items.first().totalKopecks) // 5 * 20.00 = 100.00 грн
    }

    @Test
    fun testScanningElectronicExciseStamp_autoAttachesToReceiptItem() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(exciseProduct)
        )

        // Додаємо підакцизний товар у чек
        viewModel.onEvent(PosEvent.AddProduct(exciseProduct))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertTrue(viewModel.state.value.items.first().requiresExciseStamp)
        assertNull(viewModel.state.value.items.first().exciseStamp)

        // Скануємо електронну акцизну марку (QR-код ДПС/Дія)
        viewModel.onEvent(PosEvent.ScanBarcode("https://t.gov.ua/AA12345678"))
        advanceUntilIdle()

        val updatedItem = viewModel.state.value.items.first()
        assertEquals("AA12345678", updatedItem.exciseStamp)
        assertNull(viewModel.state.value.activeExcisePromptIndex)
    }

    @Test
    fun testScanningExciseStamp_fulfillsActivePrompt() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(exciseProduct)
        )

        viewModel.onEvent(PosEvent.AddProduct(exciseProduct))
        advanceUntilIdle()

        // Маркер запиту марки активний
        assertEquals(0, viewModel.state.value.activeExcisePromptIndex)

        // Касир сканує класичний штрих-код марки (наприклад 10-символьний літерно-цифровий)
        viewModel.onEvent(PosEvent.ScanBarcode("ABBA998877"))
        advanceUntilIdle()

        assertEquals("ABBA998877", viewModel.state.value.items.first().exciseStamp)
        assertNull(viewModel.state.value.activeExcisePromptIndex)
    }

    @Test
    fun testUnknownBarcode_opensRegistrationDialogWithPrefilledCode() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(standardProduct)
        )

        // Сканування невідомого штрих-коду
        viewModel.onEvent(PosEvent.ScanBarcode("4820000009999"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isProductEditDialogOpen)
        assertNull(state.editingProduct)
        assertEquals("4820000009999", state.prefilledBarcode)
        assertTrue(state.statusMessage?.contains("не знайдено") == true)
    }
}
