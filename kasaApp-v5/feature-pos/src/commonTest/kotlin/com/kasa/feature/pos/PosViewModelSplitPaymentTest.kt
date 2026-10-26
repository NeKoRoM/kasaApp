package com.kasa.feature.pos

import com.kasa.model.PaymentType
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

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelSplitPaymentTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val coffee = Product(
        id = 10,
        name = "Кава Арабіка 250г",
        price = 150.00,
        taxGroup = TaxGroup.GROUP_A
    )

    private val tea = Product(
        id = 20,
        name = "Чай Зелений 100г",
        price = 50.00,
        taxGroup = TaxGroup.GROUP_A
    )

    @BeforeTest
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        transport.connect("SIMULATED_DATECS")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSetSplitPaymentUpdatesStateAndTotals() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(coffee, tea)
        )

        // Додаємо каву (150 грн) та чай (50 грн) = 200.00 грн (20000 коп)
        vm.onEvent(PosEvent.AddProduct(coffee))
        vm.onEvent(PosEvent.AddProduct(tea))

        assertEquals(20000L, vm.state.value.rawTotalKopecks)

        // Встановлюємо змішану оплату: 80 грн готівка (8000 коп), 120 грн картка (12000 коп)
        vm.onEvent(PosEvent.SetSplitPayment(cashKopecks = 8000L, cardKopecks = 12000L))

        val state = vm.state.value
        assertEquals(PaymentType.COMBINED, state.paymentType)
        assertEquals(8000L, state.splitCashKopecks)
        assertEquals(12000L, state.splitCardKopecks)
        assertEquals(20000L, state.totalPayableKopecks)
        assertFalse(state.isSplitPaymentDialogOpen)
        assertTrue(state.canPay)
    }

    @Test
    fun testPromptSplitPaymentTogglesDialog() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport
        )

        assertFalse(vm.state.value.isSplitPaymentDialogOpen)
        vm.onEvent(PosEvent.PromptSplitPayment(true))
        assertTrue(vm.state.value.isSplitPaymentDialogOpen)
        vm.onEvent(PosEvent.PromptSplitPayment(false))
        assertFalse(vm.state.value.isSplitPaymentDialogOpen)
    }

    @Test
    fun testFiscalizeSplitPaymentUpdatesOnlyCashPortionInDrawer() = runTest {
        var recordedDrawerBalance: Long? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(coffee),
            initialDrawerBalanceKopecks = 100000L, // 1000.00 грн у скриньці
            onDrawerBalanceUpdated = { recordedDrawerBalance = it }
        )

        vm.onEvent(PosEvent.AddProduct(coffee)) // 150.00 грн (15000 коп)

        // Клієнт платить 50.00 грн готівкою та 100.00 грн карткою
        vm.onEvent(PosEvent.SetSplitPayment(cashKopecks = 5000L, cardKopecks = 10000L))

        vm.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.items.isEmpty())
        assertNull(state.splitCashKopecks)
        assertNull(state.splitCardKopecks)

        // У грошову скриньку має потрапити ТІЛЬКИ готівка (50 грн = 5000 коп)
        // 100000 + 5000 = 105000 коп (1050.00 грн)
        assertEquals(105000L, state.cashDrawerBalanceKopecks)
        assertEquals(105000L, recordedDrawerBalance)

        // Журнал чеків
        assertEquals(1, state.journalReceipts.size)
        val journalReceipt = state.journalReceipts[0]
        assertEquals(PaymentType.COMBINED, journalReceipt.paymentType)
        assertEquals(5000L, journalReceipt.splitCashKopecks)
        assertEquals(10000L, journalReceipt.splitCardKopecks)
    }

    @Test
    fun testSwitchingPaymentTypeClearsSplitAmounts() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(coffee)
        )

        vm.onEvent(PosEvent.AddProduct(coffee))
        vm.onEvent(PosEvent.SetSplitPayment(cashKopecks = 5000L, cardKopecks = 10000L))
        assertEquals(PaymentType.COMBINED, vm.state.value.paymentType)

        // Перемикаємо на звичайну картку
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        assertEquals(PaymentType.CARD, vm.state.value.paymentType)
        assertNull(vm.state.value.splitCashKopecks)
        assertNull(vm.state.value.splitCardKopecks)
    }

    @Test
    fun testRefundSplitPaymentDeductsOnlyCashFromDrawer() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(coffee),
            initialDrawerBalanceKopecks = 50000L // 500.00 грн
        )

        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        vm.onEvent(PosEvent.AddProduct(coffee)) // Повернення 150 грн

        // Повертаємо 50 грн готівкою та 100 грн на картку
        vm.onEvent(PosEvent.SetSplitPayment(cashKopecks = 5000L, cardKopecks = 10000L))

        vm.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        // Зі скриньки має вилучитись тільки готівкова частина (-5000 коп): 50000 - 5000 = 45000 коп
        assertEquals(45000L, vm.state.value.cashDrawerBalanceKopecks)
    }
}
