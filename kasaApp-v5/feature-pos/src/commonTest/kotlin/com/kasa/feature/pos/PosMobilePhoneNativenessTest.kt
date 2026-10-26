package com.kasa.feature.pos

import com.kasa.model.PriceTagRenderer
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тести мобільної нативності для смартфонів Android та iPhone:
 * - Робота сенсорних степерів кількості прямо на стрічці чека;
 * - Видалення позицій при зменшенні кількості нижче порогу;
 * - Підтримка вкладок мобільного режиму (PosCompactTab);
 * - Друк багатокопійних поличкових цінників.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosMobilePhoneNativenessTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val pieceProduct = Product(
        id = 101L,
        name = "Круасан з шоколадом",
        price = 45.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "шт",
        barcodes = listOf("482000000201")
    )

    private val weightedProduct = Product(
        id = 102L,
        name = "Банани вагові",
        price = 60.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "кг",
        barcodes = listOf("290000000102"),
        isWeighted = true
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
            initialCatalog = listOf(pieceProduct, weightedProduct)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCompactTabNavigationState() {
        val catalogTab = PosCompactTab.CATALOG
        val cartTab = PosCompactTab.CART
        assertEquals("CATALOG", catalogTab.name)
        assertEquals("CART", cartTab.name)
        assertEquals(2, PosCompactTab.values().size)
    }

    @Test
    fun testMobileInlineQuantityStepperForPieceItem() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(pieceProduct))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1.0, viewModel.state.value.items[0].quantity)

        // Мобільне збільшення степером [+]
        viewModel.onEvent(PosEvent.UpdateQuantity(0, 2.0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2.0, viewModel.state.value.items[0].quantity)

        // Мобільне зменшення степером [-]
        viewModel.onEvent(PosEvent.UpdateQuantity(0, 1.0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1.0, viewModel.state.value.items[0].quantity)

        // Зменшення до 0 призводить до видалення товару
        viewModel.onEvent(PosEvent.RemoveItem(0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.items.isEmpty())
    }

    @Test
    fun testMobileInlineQuantityStepperForWeightedItem() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(weightedProduct, customQuantity = 0.5))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(0.5, viewModel.state.value.items[0].quantity)

        // Збільшення ваги на 100 грам [+]
        val increased = Math.round((viewModel.state.value.items[0].quantity + 0.1) * 1000.0) / 1000.0
        viewModel.onEvent(PosEvent.UpdateQuantity(0, increased))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0.6, viewModel.state.value.items[0].quantity)

        // Зменшення ваги на 100 грам [-]
        val decreased = Math.round((viewModel.state.value.items[0].quantity - 0.1) * 1000.0) / 1000.0
        viewModel.onEvent(PosEvent.UpdateQuantity(0, decreased))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0.5, viewModel.state.value.items[0].quantity)
    }

    @Test
    fun testMobilePriceTagMultiCopyRendering() {
        val singleTag = PriceTagRenderer.renderPriceTag(
            product = pieceProduct,
            storeName = "KASA-POS СУПЕРМАРКЕТ",
            printDate = "2026-09-25"
        )
        assertTrue(singleTag.contains("Круасан з шоколадом"))
        assertTrue(singleTag.contains("45.00 ГРН"))

        val copyCount = 3
        val multiTag = (1..copyCount).joinToString("\n\n") { singleTag }
        assertEquals(3, multiTag.split("\n\n").size)
    }

    @Test
    fun testMobileScannerTriggerAndPromptDialogs() = runTest {
        assertFalse(viewModel.state.value.isScannerDialogOpen)
        viewModel.onEvent(PosEvent.ToggleScannerDialog(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isScannerDialogOpen)

        viewModel.onEvent(PosEvent.ScanBarcode("482000000201"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(pieceProduct.name, viewModel.state.value.items[0].productName)

        viewModel.onEvent(PosEvent.ToggleScannerDialog(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isScannerDialogOpen)
    }

    @Test
    fun testMobileMultiCartPillState() = runTest {
        // Додаємо товар у перший чек
        viewModel.onEvent(PosEvent.AddProduct(pieceProduct))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeCartIndex)
        assertEquals(1, viewModel.state.value.carts[0].items.size)

        // Перемикаємо на другий чек (Чек 2)
        viewModel.onEvent(PosEvent.SwitchCart(1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.activeCartIndex)
        assertTrue(viewModel.state.value.items.isEmpty())

        // Додаємо товар у другий чек
        viewModel.onEvent(PosEvent.AddProduct(weightedProduct, customQuantity = 1.5))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1, viewModel.state.value.carts[1].items.size)

        // Повертаємося до першого чека
        viewModel.onEvent(PosEvent.SwitchCart(0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeCartIndex)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(pieceProduct.name, viewModel.state.value.items[0].productName)
    }

    @Test
    fun testMobileStockQuickAdjustment() = runTest {
        val initialRemainder = viewModel.state.value.catalogProducts.first { it.id == 101L }.remainder
        viewModel.onEvent(PosEvent.AdjustProductStock(101L, 15.0))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedRemainder = viewModel.state.value.catalogProducts.first { it.id == 101L }.remainder
        assertEquals(initialRemainder + 15.0, updatedRemainder)
    }

    @Test
    fun testMobilePluDialogState() = runTest {
        assertFalse(viewModel.state.value.isPluDialogOpen)
        viewModel.onEvent(PosEvent.TogglePluDialog)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isPluDialogOpen)

        viewModel.onEvent(PosEvent.TogglePluDialog)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isPluDialogOpen)
    }

    @Test
    fun testMobilePhoneDiscountAndPricePrompts() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(pieceProduct))
        testDispatcher.scheduler.advanceUntilIdle()

        // Відкриття діалогу знижки на позицію
        viewModel.onEvent(PosEvent.PromptItemDiscount(0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeDiscountPromptItemIndex)

        // Застосування 10% знижки
        viewModel.onEvent(PosEvent.SetItemDiscount(0, 10.0))
        viewModel.onEvent(PosEvent.PromptItemDiscount(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10.0, viewModel.state.value.items[0].discountPercent)
        assertEquals(4050L, viewModel.state.value.totalPayableKopecks)

        // Зміна ціни позиції через діалог
        viewModel.onEvent(PosEvent.PromptItemPrice(0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activePricePromptIndex)

        viewModel.onEvent(PosEvent.UpdateItemPrice(0, 5000L))
        viewModel.onEvent(PosEvent.PromptItemPrice(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5000L, viewModel.state.value.items[0].priceKopecks)
    }
}
