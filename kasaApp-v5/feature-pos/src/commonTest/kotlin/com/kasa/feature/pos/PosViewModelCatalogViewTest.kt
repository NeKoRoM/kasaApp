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
 * Тести режимів відображення каталогу та швидкого оприбуткування товару:
 * - Перемикання режимів відображення каталогу (Таблиця / Сітка)
 * - Швидке модальне вікно оприбуткування товару (Quick Stock)
 * - Фільтрація товарів за наявністю (IN_STOCK, OUT_OF_STOCK)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelCatalogViewTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val inStockProduct = Product(
        id = 601L,
        name = "Кава Арабіка",
        price = 85.00,
        remainder = 12.0,
        minRemainder = 2.0
    )

    private val outOfStockProduct = Product(
        id = 602L,
        name = "Чай Ерл Грей",
        price = 60.00,
        remainder = 0.0,
        minRemainder = 5.0
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
    fun testSetCatalogViewMode_togglesBetweenGridAndTable() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(inStockProduct)
        )

        assertFalse(viewModel.state.value.isCatalogTableView)

        viewModel.onEvent(PosEvent.SetCatalogViewMode(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isCatalogTableView)

        viewModel.onEvent(PosEvent.SetCatalogViewMode(false))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isCatalogTableView)
    }

    @Test
    fun testPromptQuickStock_setsAndClearsQuickStockProduct() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(inStockProduct)
        )

        assertNull(viewModel.state.value.quickStockProduct)

        viewModel.onEvent(PosEvent.PromptQuickStock(inStockProduct))
        advanceUntilIdle()
        assertEquals(inStockProduct.id, viewModel.state.value.quickStockProduct?.id)

        viewModel.onEvent(PosEvent.PromptQuickStock(null))
        advanceUntilIdle()
        assertNull(viewModel.state.value.quickStockProduct)
    }

    @Test
    fun testPerformQuickStock_adjustsRemainderAndClosesPrompt() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(inStockProduct)
        )

        viewModel.onEvent(PosEvent.PromptQuickStock(inStockProduct))
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.quickStockProduct)

        // Додаємо 10 одиниць товару
        viewModel.onEvent(PosEvent.PerformQuickStock(601L, 10.0))
        advanceUntilIdle()

        val updated = viewModel.state.value.catalogProducts.first { it.id == 601L }
        assertEquals(22.0, updated.remainder)
        assertNull(viewModel.state.value.quickStockProduct)
    }

    @Test
    fun testStockAvailabilityFilters_inStockAndOutOfStock() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(inStockProduct, outOfStockProduct)
        )

        assertTrue(inStockProduct.remainder > 0.0)
        assertTrue(outOfStockProduct.isOutOfStock)

        viewModel.onEvent(PosEvent.SetCatalogCategoryFilter("IN_STOCK"))
        advanceUntilIdle()
        assertEquals("IN_STOCK", viewModel.state.value.selectedCatalogCategory)

        viewModel.onEvent(PosEvent.SetCatalogCategoryFilter("OUT_OF_STOCK"))
        advanceUntilIdle()
        assertEquals("OUT_OF_STOCK", viewModel.state.value.selectedCatalogCategory)
    }
}
