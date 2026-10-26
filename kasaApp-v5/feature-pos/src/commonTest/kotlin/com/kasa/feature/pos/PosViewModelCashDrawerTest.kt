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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelCashDrawerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val bread = Product(
        id = 1,
        name = "Хліб Бородинський",
        price = 25.00,
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
    fun testInitialBalanceAndManualUpdate() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialDrawerBalanceKopecks = 150000L // 1500.00 грн
        )

        assertEquals(150000L, vm.state.value.cashDrawerBalanceKopecks)

        vm.onEvent(PosEvent.UpdateCashDrawerBalance(200000L))
        assertEquals(200000L, vm.state.value.cashDrawerBalanceKopecks)
    }

    @Test
    fun testCashInOutUpdatesDrawerBalanceAndCallback() = runTest {
        var lastSavedBalance: Long? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialDrawerBalanceKopecks = 50000L, // 500.00 грн
            onDrawerBalanceUpdated = { newBal -> lastSavedBalance = newBal }
        )

        // Службове внесення +200 грн (20000 коп)
        vm.onEvent(PosEvent.PerformCashInOut(20000L))
        advanceUntilIdle()

        assertEquals(70000L, vm.state.value.cashDrawerBalanceKopecks)
        assertEquals(70000L, lastSavedBalance)

        // Службова видача -300 грн (-30000 коп)
        vm.onEvent(PosEvent.PerformCashInOut(-30000L))
        advanceUntilIdle()

        assertEquals(40000L, vm.state.value.cashDrawerBalanceKopecks)
        assertEquals(40000L, lastSavedBalance)
    }

    @Test
    fun testFiscalizeCashSaleIncreasesDrawerBalance() = runTest {
        var lastSavedBalance: Long? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread),
            initialDrawerBalanceKopecks = 10000L, // 100 грн
            onDrawerBalanceUpdated = { lastSavedBalance = it }
        )

        vm.onEvent(PosEvent.AddProduct(bread)) // 25.00 грн (2500 коп)
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // 10000 + 2500 = 12500 коп
        assertEquals(12500L, vm.state.value.cashDrawerBalanceKopecks)
        assertEquals(12500L, lastSavedBalance)
    }

    @Test
    fun testFiscalizeCardSaleDoesNotChangeDrawerBalance() = runTest {
        var callbackTriggered = false
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread),
            initialDrawerBalanceKopecks = 10000L,
            onDrawerBalanceUpdated = { callbackTriggered = true }
        )

        vm.onEvent(PosEvent.AddProduct(bread))
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // Баланс скриньки залишається незмінним
        assertEquals(10000L, vm.state.value.cashDrawerBalanceKopecks)
        assertFalse(callbackTriggered)
    }

    @Test
    fun testFiscalizeCashReturnDecreasesDrawerBalance() = runTest {
        var lastSavedBalance: Long? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread),
            initialDrawerBalanceKopecks = 10000L, // 100 грн
            onDrawerBalanceUpdated = { lastSavedBalance = it }
        )

        vm.onEvent(PosEvent.AddProduct(bread)) // 25.00 грн
        vm.onEvent(PosEvent.SetReceiptType(com.kasa.model.ReceiptType.RETURN))
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // 10000 - 2500 = 7500 коп
        assertEquals(7500L, vm.state.value.cashDrawerBalanceKopecks)
        assertEquals(7500L, lastSavedBalance)
    }
}
