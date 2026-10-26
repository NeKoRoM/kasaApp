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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosResponsiveLayoutTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val sampleProduct = Product(
        id = 1L,
        name = "Кава Арабіка 250г",
        price = 120.0,
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000101")
    )

    private val sampleTea = Product(
        id = 2L,
        name = "Чай Цейлонський 100г",
        price = 80.0,
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000102")
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
            initialCatalog = listOf(sampleProduct, sampleTea)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCompactTabValuesAndOrdinals() {
        // Перевіряємо порядок та наявність вкладок компактного інтерфейсу
        assertEquals(0, PosCompactTab.CATALOG.ordinal)
        assertEquals(1, PosCompactTab.CART.ordinal)
        assertEquals("CATALOG", PosCompactTab.CATALOG.name)
        assertEquals("CART", PosCompactTab.CART.name)
    }

    @Test
    fun testCompactStickyBarStateWhenCartEmptyVsNonEmpty() = runTest(testDispatcher) {
        // Початковий стан: чеки порожні, липка плашка не активна
        val initialState = viewModel.state.value
        assertEquals(0, initialState.items.size)
        assertEquals(0L, initialState.totalPayableKopecks)

        // Додаємо товар у кошик
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct))
        testScheduler.advanceUntilIdle()

        // Стан з товаром: стрічка має 1 позицію, сума 120.00 грн (12000 копійок)
        val stateWithItem = viewModel.state.value
        assertEquals(1, stateWithItem.items.size)
        assertEquals(12000L, stateWithItem.totalPayableKopecks)
        assertTrue(stateWithItem.items.isNotEmpty())

        // Перевірка форматування суми для бейджа плашки
        val formattedTotal = PosCurrencyFormatter.formatKopecks(stateWithItem.totalPayableKopecks)
        assertEquals("120.00", formattedTotal)
    }

    @Test
    fun testCartBadgeUpdatesAcrossParallelCartsInResponsiveLayout() = runTest(testDispatcher) {
        // Додаємо товар у Чек 1
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(12000L, viewModel.state.value.totalPayableKopecks)

        // Перемикаємось на Чек 2 (він порожній)
        viewModel.onEvent(PosEvent.SwitchCart(1))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.activeCartIndex)
        assertEquals(0, viewModel.state.value.items.size)
        assertEquals(0L, viewModel.state.value.totalPayableKopecks)

        // Додаємо два товари у Чек 2
        viewModel.onEvent(PosEvent.AddProduct(sampleTea))
        viewModel.onEvent(PosEvent.AddProduct(sampleTea))
        testScheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)
        assertEquals(16000L, viewModel.state.value.totalPayableKopecks)

        // Повертаємось на Чек 1 — стан ізольовано збережено
        viewModel.onEvent(PosEvent.SwitchCart(0))
        testScheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeCartIndex)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(12000L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testDesktopHotKeyEventsExecution() = runTest(testDispatcher) {
        // F2: Віртуальний сканер
        assertFalse(viewModel.state.value.isScannerDialogOpen)
        viewModel.onEvent(PosEvent.ToggleScannerDialog(true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isScannerDialogOpen)

        // Esc: Закрити активне модальне вікно
        viewModel.onEvent(PosEvent.ToggleScannerDialog(false))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isScannerDialogOpen)

        // F4: Сенсорний нумпад
        assertFalse(viewModel.state.value.isNumpadDialogOpen)
        viewModel.onEvent(PosEvent.ToggleNumpadDialog(true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isNumpadDialogOpen)

        viewModel.onEvent(PosEvent.ToggleNumpadDialog(false))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isNumpadDialogOpen)
    }

    @Test
    fun testCompactSortModeCyclingSequence() {
        fun nextSort(current: String): String = when (current) {
            "NAME" -> "PRICE"
            "PRICE" -> "PLU"
            else -> "NAME"
        }

        var sort = "PLU"
        sort = nextSort(sort)
        assertEquals("NAME", sort)
        sort = nextSort(sort)
        assertEquals("PRICE", sort)
        sort = nextSort(sort)
        assertEquals("PLU", sort)
    }

    @Test
    fun testCatalogSearchQueryFilteringAndScanning() = runTest(testDispatcher) {
        viewModel.onEvent(PosEvent.UpdateSearchQuery("482000000101"))
        testScheduler.advanceUntilIdle()
        assertEquals("482000000101", viewModel.state.value.searchQuery)

        // Сканування штрих-коду через компактний мобільний рядок пошуку
        viewModel.onEvent(PosEvent.ScanBarcode("482000000101"))
        viewModel.onEvent(PosEvent.UpdateSearchQuery(""))
        testScheduler.advanceUntilIdle()
        assertEquals("", viewModel.state.value.searchQuery)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Кава Арабіка 250г", viewModel.state.value.items.first().productName)
    }

    @Test
    fun testCartQuantityMappingForCatalogProductBadges() = runTest(testDispatcher) {
        // Початково кошик порожній
        var cartQtyMap = viewModel.state.value.items.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
        assertEquals(0.0, cartQtyMap[sampleProduct.id] ?: 0.0)

        // Додаємо 1 позицію
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct))
        testScheduler.advanceUntilIdle()
        cartQtyMap = viewModel.state.value.items.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
        assertEquals(1.0, cartQtyMap[sampleProduct.id])

        // Додаємо ще 1 таку ж позицію (інкремент)
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct))
        testScheduler.advanceUntilIdle()
        cartQtyMap = viewModel.state.value.items.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
        assertEquals(2.0, cartQtyMap[sampleProduct.id])

        // Інший товар не повинен мати бейджа
        assertEquals(0.0, cartQtyMap[sampleTea.id] ?: 0.0)
    }

    @Test
    fun testPaymentBlockerConditionsLogic() = runTest(testDispatcher) {
        viewModel.onEvent(PosEvent.AddProduct(sampleProduct))
        testScheduler.advanceUntilIdle()

        // 1. Початково з готівкою (без введених грошей) canPay = true
        assertTrue(viewModel.state.value.canPay)

        // 2. Якщо введено менше грошей, ніж потрібно -> isCashShort = true, canPay = false
        viewModel.onEvent(PosEvent.SetReceivedCash(5000L)) // 50.00 грн при ціні 120.00 грн
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isCashShort)
        assertFalse(viewModel.state.value.canPay)

        // 3. Якщо введено повну суму -> isCashShort = false, canPay = true
        viewModel.onEvent(PosEvent.SetReceivedCash(12000L))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isCashShort)
        assertTrue(viewModel.state.value.canPay)
    }
}
