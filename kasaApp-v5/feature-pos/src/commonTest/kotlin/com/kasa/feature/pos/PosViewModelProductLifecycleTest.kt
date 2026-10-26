package com.kasa.feature.pos

import com.kasa.model.Product
import com.kasa.model.ReceiptType
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
 * Тести життєвого циклу товарів:
 * - Списання залишків для мульти-рядків та розбитих підакцизних товарів
 * - Відновлення залишків при чеку повернення
 * - Детекція колізій штрих-кодів при збереженні
 * - Фільтрація товарів із низьким залишком (low stock)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelProductLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val excisableWine = Product(
        id = 501L,
        name = "Вино Шабо Каберне",
        price = 150.00,
        taxGroup = TaxGroup.GROUP_B,
        uktzed = "2204218000",
        remainder = 20.0,
        minRemainder = 5.0,
        barcodes = listOf("4820005010015")
    )

    private val chocolateBar = Product(
        id = 502L,
        name = "Шоколад Світоч",
        price = 40.00,
        taxGroup = TaxGroup.GROUP_A,
        remainder = 50.0,
        minRemainder = 10.0,
        barcodes = listOf("4820005020014")
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
    fun testExciseSplitItems_deductsTotalStockRemainderAccurately() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(excisableWine, chocolateBar)
        )

        // Продаж 3 пляшок підакцизного вина (автоматично розбивається на 3 окремі рядки по 1.0 шт)
        viewModel.onEvent(PosEvent.AddProduct(excisableWine, 3.0))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.items.size)
        assertTrue(state.items.all { it.productId == 501L && it.quantity == 1.0 })

        // Вносимо унікальні акцизні марки для кожної з 3 пляшок
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "AA1111111101"))
        viewModel.onEvent(PosEvent.EnterExciseStamp(1, "AA1111111102"))
        viewModel.onEvent(PosEvent.EnterExciseStamp(2, "AA1111111103"))
        advanceUntilIdle()

        // Фіскалізуємо чек
        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // Початковий залишок був 20.0, має стати 17.0 (списано рівно 3.0 шт, а не 1.0)
        val updatedWine = viewModel.state.value.catalogProducts.first { it.id == 501L }
        assertEquals(17.0, updatedWine.remainder)
    }

    @Test
    fun testReturnReceipt_restoresTotalStockForMultipleItems() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(chocolateBar)
        )

        // Чек повернення
        viewModel.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        advanceUntilIdle()
        assertEquals(ReceiptType.RETURN, viewModel.state.value.receiptType)

        // Додаємо 2 рядки того самого товару (наприклад повернення 2 шт + 3 шт)
        viewModel.onEvent(PosEvent.AddProduct(chocolateBar, 2.0))
        viewModel.onEvent(PosEvent.AddProduct(chocolateBar, 3.0))
        advanceUntilIdle()

        // Фіскалізація чека повернення
        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // Початковий залишок був 50.0, після повернення 2+3 має стати 55.0
        val updated = viewModel.state.value.catalogProducts.first { it.id == 502L }
        assertEquals(55.0, updated.remainder)
    }

    @Test
    fun testSaveProductWithConflictingBarcode_updatesStatusWithWarning() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(chocolateBar)
        )

        // Створюємо новий товар з штрих-кодом, який вже належить chocolateBar (4820005020014)
        val conflictingProduct = Product(
            id = 503L,
            name = "Шоколадний батончик інший",
            price = 35.00,
            barcodes = listOf("4820005020014")
        )

        viewModel.onEvent(PosEvent.SaveProduct(conflictingProduct))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.catalogProducts.size)
        assertTrue(state.statusMessage?.contains("⚠️ спільний код з #502") == true)
    }

    @Test
    fun testLowStockProduct_detectsThresholdCorrectly() = runTest(testDispatcher) {
        val lowStockItem = chocolateBar.copy(remainder = 8.0, minRemainder = 10.0)
        val normalItem = excisableWine.copy(remainder = 15.0, minRemainder = 5.0)

        assertTrue(lowStockItem.isLowStock)
        assertFalse(lowStockItem.isOutOfStock)

        assertFalse(normalItem.isLowStock)
        assertFalse(normalItem.isOutOfStock)

        val outOfStockItem = chocolateBar.copy(remainder = 0.0)
        assertTrue(outOfStockItem.isOutOfStock)
    }

    @Test
    fun testScanByProductNameOrCyrillicQuery_findsAndAddsItem() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(chocolateBar)
        )

        // Скануємо/вводимо назву товару (або пошук)
        viewModel.onEvent(PosEvent.ScanBarcode("Шоколад Світоч"))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Шоколад Світоч", viewModel.state.value.items[0].productName)
    }

    @Test
    fun testScanUnknownNamedQuery_opensProductEditDialogWithPrefilledName() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(chocolateBar)
        )

        // Введено текстову назву, якої немає в каталозі
        viewModel.onEvent(PosEvent.ScanBarcode("Печиво До кави"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isProductEditDialogOpen)
        assertNotNull(state.editingProduct)
        assertEquals("Печиво До кави", state.editingProduct?.name)
        assertNull(state.prefilledBarcode)
    }

    @Test
    fun testScanUnknownNumericBarcode_opensProductEditDialogWithPrefilledBarcode() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(chocolateBar)
        )

        // Введено цифровий штрих-код, якого немає в каталозі
        viewModel.onEvent(PosEvent.ScanBarcode("4829999999999"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isProductEditDialogOpen)
        assertNull(state.editingProduct)
        assertEquals("4829999999999", state.prefilledBarcode)
    }
}
