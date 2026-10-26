package com.kasa.feature.pos

import com.kasa.model.PaymentType
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

/**
 * Тести мобільної ергономіки та швидких розрахунків mPOS:
 * - Розумні рекомендації купюр готівки (найближчі округлені суми >= суми чека);
 * - Службові операції з готівкою (внесення, розмін, інкасація);
 * - Відкладення та відновлення чеків одним дотиком.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosMobileErgonomicsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val sampleCoffee = Product(
        id = 101L,
        name = "Капучино 250мл",
        price = 45.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "порц",
        barcodes = listOf("482000000101")
    )

    private val sampleCake = Product(
        id = 102L,
        name = "Торт Наполеон",
        price = 139.50,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "шт",
        barcodes = listOf("482000000102")
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
            initialCatalog = listOf(sampleCoffee, sampleCake)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSmartBanknoteRecommendationsPrioritizesNearestRoundAmounts() {
        // 1. Для чека 45.00 грн першою рекомендованою купюрою має бути 50 ₴
        val bills45 = calculateSuggestedBills(4500L)
        assertTrue(bills45.isNotEmpty())
        assertEquals(50L, bills45.first())
        assertTrue(bills45.contains(100L))
        assertTrue(bills45.contains(200L))

        // 2. Для чека 184.50 грн першою рекомендованою купюрою має бути 200 ₴
        val bills185 = calculateSuggestedBills(18450L)
        assertEquals(200L, bills185.first())
        assertTrue(bills185.contains(500L))
        assertTrue(bills185.contains(1000L))

        // 3. Для чека 680.00 грн першою має бути 700 ₴ (або 800 / 1000)
        val bills680 = calculateSuggestedBills(68000L)
        assertEquals(700L, bills680.first())
        assertTrue(bills680.contains(800L))
        assertTrue(bills680.contains(1000L))

        // 4. Для 0 грн повертаються стандартні купюри НБУ
        val bills0 = calculateSuggestedBills(0L)
        assertEquals(listOf(20L, 50L, 100L, 200L, 500L, 1000L), bills0)
    }

    @Test
    fun testServiceCashFloatDepositAndCollectionFlow() = runTest(testDispatcher) {
        // Початковий баланс скриньки 0
        assertEquals(0L, viewModel.state.value.cashDrawerBalanceKopecks)

        // Службове внесення розмінної монети: +500 грн (50000 копійок)
        viewModel.onEvent(PosEvent.PerformCashInOut(50000L))
        testScheduler.advanceUntilIdle()
        assertEquals(50000L, viewModel.state.value.cashDrawerBalanceKopecks)

        // Додаткове внесення: +200 грн (20000 копійок)
        viewModel.onEvent(PosEvent.PerformCashInOut(20000L))
        testScheduler.advanceUntilIdle()
        assertEquals(70000L, viewModel.state.value.cashDrawerBalanceKopecks)

        // Службова видача / інкасація: -700 грн (-70000 копійок)
        viewModel.onEvent(PosEvent.PerformCashInOut(-70000L))
        testScheduler.advanceUntilIdle()
        assertEquals(0L, viewModel.state.value.cashDrawerBalanceKopecks)
    }

    @Test
    fun testHoldAndRestoreReceiptFlow() = runTest(testDispatcher) {
        // Додаємо товар у кошик
        viewModel.onEvent(PosEvent.AddProduct(sampleCoffee))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(0, viewModel.state.value.heldReceipts.size)

        // Відкладаємо чек із коментарем "Стіл 4"
        viewModel.onEvent(PosEvent.HoldCurrentReceipt("Стіл 4"))
        testScheduler.advanceUntilIdle()

        // Кошик став порожнім, у списку відкладених з'явився 1 чек
        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(1, viewModel.state.value.heldReceipts.size)
        val held = viewModel.state.value.heldReceipts.first()
        assertEquals("Стіл 4", held.note)
        assertEquals(1, held.receipt.items.size)
        assertEquals("Капучино 250мл", held.receipt.items.first().productName)

        // Відновлюємо чек в активний кошик
        viewModel.onEvent(PosEvent.RestoreHeldReceipt(held.id))
        testScheduler.advanceUntilIdle()

        // Чек повернувся в кошик, а список відкладених спорожнів
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Капучино 250мл", viewModel.state.value.items.first().productName)
        assertEquals(0, viewModel.state.value.heldReceipts.size)
    }

    @Test
    fun testMultiCartSwitchingPreservesIndividualCartsState() = runTest(testDispatcher) {
        // Чек 1: додаємо каву
        assertEquals(0, viewModel.state.value.activeCartIndex)
        viewModel.onEvent(PosEvent.AddProduct(sampleCoffee))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Капучино 250мл", viewModel.state.value.items.first().productName)

        // Перемикаємося на Чек 2 (індекс 1)
        viewModel.onEvent(PosEvent.SwitchCart(1))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.activeCartIndex)
        assertTrue(viewModel.state.value.items.isEmpty())

        // Чек 2: додаємо торт
        viewModel.onEvent(PosEvent.AddProduct(sampleCake))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Торт Наполеон", viewModel.state.value.items.first().productName)

        // Перемикаємося назад на Чек 1 (індекс 0)
        viewModel.onEvent(PosEvent.SwitchCart(0))
        testScheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeCartIndex)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Капучино 250мл", viewModel.state.value.items.first().productName)

        // Перемикаємося назад на Чек 2 (індекс 1)
        viewModel.onEvent(PosEvent.SwitchCart(1))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.activeCartIndex)
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Торт Наполеон", viewModel.state.value.items.first().productName)
    }

    @Test
    fun testQuickStockAdjustmentUpdatesProductRemainder() = runTest(testDispatcher) {
        val initialRemainder = viewModel.state.value.catalogProducts.find { it.id == sampleCoffee.id }?.remainder ?: 0.0

        // Оприбуткування +25 од.
        viewModel.onEvent(PosEvent.AdjustProductStock(sampleCoffee.id, 25.0))
        testScheduler.advanceUntilIdle()

        val updatedRemainder = viewModel.state.value.catalogProducts.find { it.id == sampleCoffee.id }?.remainder ?: 0.0
        assertEquals(initialRemainder + 25.0, updatedRemainder, 0.001)

        // Списання -10 од.
        viewModel.onEvent(PosEvent.AdjustProductStock(sampleCoffee.id, -10.0))
        testScheduler.advanceUntilIdle()

        val finalRemainder = viewModel.state.value.catalogProducts.find { it.id == sampleCoffee.id }?.remainder ?: 0.0
        assertEquals(initialRemainder + 15.0, finalRemainder, 0.001)
    }

    @Test
    fun testShiftReportCashOperationsDepositAndWithdrawal() = runTest(testDispatcher) {
        if (viewModel.state.value.currentShift == null || !viewModel.state.value.currentShift!!.isOpen) {
            viewModel.onEvent(PosEvent.OpenShift())
            testScheduler.advanceUntilIdle()
        }

        val initialBalance = viewModel.state.value.cashDrawerBalanceKopecks

        // Службове внесення розмінної монети (+300 грн)
        viewModel.onEvent(PosEvent.PerformCashInOut(30000L))
        testScheduler.advanceUntilIdle()

        assertEquals(initialBalance + 30000L, viewModel.state.value.cashDrawerBalanceKopecks)
        assertEquals(30000L, viewModel.state.value.currentShift?.totalCashInKopecks)

        // Службова видача / часткова інкасація (-200 грн)
        viewModel.onEvent(PosEvent.PerformCashInOut(-20000L))
        testScheduler.advanceUntilIdle()

        assertEquals(initialBalance + 10000L, viewModel.state.value.cashDrawerBalanceKopecks)
        assertEquals(20000L, viewModel.state.value.currentShift?.totalCashOutKopecks)
    }
}
