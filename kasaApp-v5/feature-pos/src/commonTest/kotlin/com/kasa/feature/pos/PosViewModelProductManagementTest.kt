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
 * Тести життєвого циклу управління товарами: CRUD, клонування, сканування PLU, мульти-кількість.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelProductManagementTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val existingProduct = Product(
        id = 101L,
        name = "Шоколадний батончик",
        price = 25.50,
        barcodes = listOf("4820000001011"),
        taxGroup = TaxGroup.GROUP_A,
        isWeighted = false,
        unitOfMeasurement = "шт",
        remainder = 40.0
    )

    private val weightedProduct = Product(
        id = 102L,
        name = "Яблука Чемпіон",
        price = 30.00,
        taxGroup = TaxGroup.GROUP_A,
        isWeighted = true,
        unitOfMeasurement = "кг"
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
    fun testSaveNewProduct_addsToCatalogAndNotifiesStorage() = runTest(testDispatcher) {
        var persistedCatalog: List<Product>? = null
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(existingProduct),
            onCatalogUpdated = { persistedCatalog = it }
        )

        val newProduct = Product(id = 202L, name = "Мінеральна вода 1.5л", price = 18.00, taxGroup = TaxGroup.GROUP_A)
        viewModel.onEvent(PosEvent.SaveProduct(newProduct, addToReceiptOnCreate = false))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.catalogProducts.size)
        assertTrue(state.catalogProducts.any { it.id == 202L && it.name == "Мінеральна вода 1.5л" })
        assertEquals(2, persistedCatalog?.size)
        assertFalse(state.isProductEditDialogOpen)
    }

    @Test
    fun testSaveNewProduct_withAddToReceiptOnCreate_addsDirectlyToReceipt() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        val newProduct = Product(id = 303L, name = "Свіжий круасан", price = 35.00, taxGroup = TaxGroup.GROUP_A)

        viewModel.onEvent(PosEvent.SaveProduct(newProduct, addToReceiptOnCreate = true))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.catalogProducts.size)
        assertEquals(1, state.items.size)
        assertEquals("Свіжий круасан", state.items[0].productName)
        assertEquals(3500L, state.totalPayableKopecks)
    }

    @Test
    fun testEditExistingProduct_updatesDetailsInCatalog() = runTest(testDispatcher) {
        var persistedCatalog: List<Product>? = null
        val viewModel = PosViewModel(
            receiptEngine, serviceEngine, transport,
            initialCatalog = listOf(existingProduct),
            onCatalogUpdated = { persistedCatalog = it }
        )

        val updated = existingProduct.copy(price = 29.90, name = "Шоколадний батончик Extra", remainder = 35.0)
        viewModel.onEvent(PosEvent.SaveProduct(updated))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.catalogProducts.size)
        assertEquals(29.90, state.catalogProducts[0].price)
        assertEquals("Шоколадний батончик Extra", state.catalogProducts[0].name)
        assertEquals(29.90, persistedCatalog?.firstOrNull()?.price)
    }

    @Test
    fun testDeleteProduct_removesFromCatalog() = runTest(testDispatcher) {
        var persistedCatalog: List<Product>? = null
        val viewModel = PosViewModel(
            receiptEngine, serviceEngine, transport,
            initialCatalog = listOf(existingProduct),
            onCatalogUpdated = { persistedCatalog = it }
        )

        viewModel.onEvent(PosEvent.DeleteProduct(existingProduct.id))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.catalogProducts.isEmpty())
        assertEquals(0, persistedCatalog?.size)
    }

    @Test
    fun testScanUnknownBarcode_triggersCreateProductPromptWithPrefilledBarcode() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        val unknownBarcode = "4829999999999"

        viewModel.onEvent(PosEvent.ScanBarcode(unknownBarcode))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isProductEditDialogOpen)
        assertNull(state.editingProduct)
        assertEquals(unknownBarcode, state.prefilledBarcode)
    }

    @Test
    fun testDuplicateProduct_createsDraftWithNextPluAndEmptyBarcodes() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        viewModel.onEvent(PosEvent.DuplicateProduct(existingProduct))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isProductEditDialogOpen)
        assertNotNull(state.editingProduct)
        assertEquals(102L, state.editingProduct?.id)
        assertTrue(state.editingProduct?.name?.contains("(копія)") == true)
        assertTrue(state.editingProduct?.barcodes?.isEmpty() == true)
    }

    @Test
    fun testScanDirectPlu_findsAndAddsProduct() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        // 101 - це PLU код, якого немає в barcodes:
        viewModel.onEvent(PosEvent.ScanBarcode("101"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals("Шоколадний батончик", state.items[0].productName)
    }

    @Test
    fun testScanWithQuantityMultiplier_addsCustomQuantity() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        // Сканування "3*101" або "3*4820000001011":
        viewModel.onEvent(PosEvent.ScanBarcode("3*101"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals(3.0, state.items[0].quantity)
        assertEquals(7650L, state.totalPayableKopecks)
    }


    @Test
    fun testSetCatalogCategoryFilter_updatesState() = runTest(testDispatcher) {
        val viewModel = PosViewModel(receiptEngine, serviceEngine, transport, initialCatalog = listOf(existingProduct))
        viewModel.onEvent(PosEvent.SetCatalogCategoryFilter("WEIGHTED"))
        assertEquals("WEIGHTED", viewModel.state.value.selectedCatalogCategory)
        viewModel.onEvent(PosEvent.SetCatalogCategoryFilter("ALL"))
        assertEquals("ALL", viewModel.state.value.selectedCatalogCategory)
    }

    @Test
    fun testStockInventoryAndFavoriteAndCsvImport() = runTest(testDispatcher) {
        var persistedCatalog: List<Product>? = null
        val viewModel = PosViewModel(
            receiptEngine, serviceEngine, transport,
            initialCatalog = listOf(existingProduct),
            onCatalogUpdated = { persistedCatalog = it }
        )

        // Інвентаризація залишку
        viewModel.onEvent(PosEvent.SetProductStockExact(101L, 75.0))
        advanceUntilIdle()
        assertEquals(75.0, viewModel.state.value.catalogProducts.first().remainder)

        // Перемикання Обране (Favorite)
        viewModel.onEvent(PosEvent.ToggleProductFavorite(101L))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.catalogProducts.first().isFavorite)

        // Імпорт з CSV
        val csv = """
            PLU,Назва,Ціна,Собівартість,Штрихкоди,Категорія,Ставка_ПДВ,Одиниця,Ваговий,УКТЗЕД,Залишок,Мін_Залишок,Обране
            101,Шоколадний батончик Оновлений,30.00,20.00,4820000001011,Солодощі,GROUP_A,шт,0,,100.0,15.0,1
            105,Чай Зелений,45.00,28.00,4820000001050,Напої,GROUP_A,шт,0,,50.0,10.0,0
        """.trimIndent()

        viewModel.onEvent(PosEvent.ImportProductsFromCsv(csv))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.catalogProducts.size)
        val p101 = state.catalogProducts.first { it.id == 101L }
        assertEquals("Шоколадний батончик Оновлений", p101.name)
        assertEquals(30.00, p101.price)
        assertEquals(20.00, p101.costPrice)
        assertEquals("Солодощі", p101.categoryName)
        val p105 = state.catalogProducts.first { it.id == 105L }
        assertEquals("Чай Зелений", p105.name)
        assertEquals(2, persistedCatalog?.size)

        // Переоцінка категорії +10%
        viewModel.onEvent(PosEvent.BatchAdjustPrices("Солодощі", percentDelta = 10.0, fixedDelta = 0.0))
        advanceUntilIdle()
        assertEquals(33.00, viewModel.state.value.catalogProducts.first { it.id == 101L }.price)

        // Продаж 2 шт та автоматичне списання залишку зі складу
        val prodToSell = viewModel.state.value.catalogProducts.first { it.id == 101L }
        viewModel.onEvent(PosEvent.AddProduct(prodToSell, 2.0))
        advanceUntilIdle()
        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()
        assertEquals(98.0, viewModel.state.value.catalogProducts.first { it.id == 101L }.remainder)
    }
}
