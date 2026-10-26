package com.kasa.feature.pos

import com.kasa.model.CashierShift
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

/**
 * Модульні тести контролю касової зміни та примусового закриття доби Z-звітом.
 * Перевіряє автоматичне відкриття зміни, накопичення оборотів, блокування при перевищенні 24 годин
 * та коректну фіналізацію зміни з номером Z-звіту.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelShiftTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val productCoffee = Product(
        id = 10,
        name = "Кава Арабіка",
        price = 100.00, // 10000 коп
        taxGroup = TaxGroup.GROUP_A
    )

    private val productTea = Product(
        id = 20,
        name = "Чай Ерл Грей",
        price = 50.00, // 5000 коп
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
    fun testAutoOpensShiftOnFirstFiscalSale() = runTest {
        var updatedShift: CashierShift? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(productCoffee),
            initialShift = null,
            onShiftUpdated = { updatedShift = it }
        )

        assertNull(vm.state.value.currentShift)

        vm.onEvent(PosEvent.AddProduct(productCoffee))
        advanceUntilIdle()

        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        val shift = vm.state.value.currentShift
        assertNotNull(shift)
        assertTrue(shift.isOpen)
        assertEquals(1L, shift.shiftNumber)
        assertEquals(10000L, shift.totalSalesKopecks)
        assertEquals(10000L, shift.totalSalesCashKopecks)
        assertEquals(1, shift.receiptsCount)
        assertNotNull(updatedShift)
    }

    @Test
    fun testAccumulatesSalesAndReturnsAndCashOperations() = runTest {
        val initialShift = CashierShift(
            id = "shift-init",
            shiftNumber = 1L,
            cashierId = 1L,
            cashierName = "Касир 1",
            openedAt = System.currentTimeMillis(),
            initialDrawerBalanceKopecks = 20000L // 200 грн
        )

        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(productCoffee, productTea),
            initialShift = initialShift,
            initialDrawerBalanceKopecks = 20000L
        )

        // 1. Продаж кави карткою: 100 грн = 10000 коп
        vm.onEvent(PosEvent.AddProduct(productCoffee))
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        var current = vm.state.value.currentShift
        assertNotNull(current)
        assertEquals(10000L, current.totalSalesKopecks)
        assertEquals(0L, current.totalSalesCashKopecks)
        assertEquals(10000L, current.totalSalesCardKopecks)
        assertEquals(1, current.receiptsCount)

        // 2. Службове внесення готівки: +500 грн = +50000 коп
        vm.onEvent(PosEvent.PerformCashInOut(50000L))
        advanceUntilIdle()

        current = vm.state.value.currentShift
        assertNotNull(current)
        assertEquals(50000L, current.totalCashInKopecks)

        // 3. Повернення товару готівкою: -50 грн = -5000 коп
        vm.onEvent(PosEvent.AddProduct(productTea))
        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        vm.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        current = vm.state.value.currentShift
        assertNotNull(current)
        assertEquals(5000L, current.totalReturnsKopecks)
        assertEquals(5000L, current.totalReturnsCashKopecks)
        assertEquals(2, current.receiptsCount)

        // Очікуваний залишок у скриньці: 20000 + 0 - 5000 + 50000 - 0 = 65000 коп
        assertEquals(65000L, current.expectedDrawerCashKopecks)
    }

    @Test
    fun test24HoursExpiredShiftBlocksCanPayAndFiscalization() = runTest {
        val now = System.currentTimeMillis()
        val expiredShift = CashierShift(
            id = "shift-expired",
            shiftNumber = 2L,
            cashierId = 1L,
            cashierName = "Олена",
            openedAt = now - (25 * 60 * 60 * 1000L) // 25 годин тому!
        )

        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(productCoffee),
            initialShift = expiredShift
        )

        vm.onEvent(PosEvent.AddProduct(productCoffee))
        advanceUntilIdle()

        assertTrue(vm.state.value.is24HoursShiftExpired)
        assertFalse(vm.state.value.canPay, "canPay must be false when shift exceeds 24 hours")

        // Спроба фіскалізації повинна бути відхилена
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        // Товари залишаються у кошику, фіскалізація не відбулася
        assertEquals(1, vm.state.value.items.size)
    }

    @Test
    fun testPrintZReportClosesShiftAndResetsShiftState() = runTest {
        var closedShiftRecorded: CashierShift? = null
        val activeShift = CashierShift(
            id = "shift-to-close",
            shiftNumber = 3L,
            cashierId = 1L,
            cashierName = "Касир Зміни",
            openedAt = System.currentTimeMillis() - 3600000L,
            totalSalesKopecks = 75000L
        )

        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialShift = activeShift,
            onShiftClosed = { closedShiftRecorded = it }
        )

        // Виконуємо денний фіскальний Z-звіт
        vm.onEvent(PosEvent.PrintZReport)
        advanceUntilIdle()

        // Активна зміна повинна бути скинута в null
        assertNull(vm.state.value.currentShift)

        // В lastClosedShift зберігається завершена зміна з міткою закриття
        val lastClosed = vm.state.value.lastClosedShift
        assertNotNull(lastClosed)
        assertTrue(lastClosed.isClosed)
        assertNotNull(lastClosed.closedAt)
        assertNotNull(lastClosed.zReportNumber)
        assertEquals(75000L, lastClosed.totalSalesKopecks)

        // Сховище отримало закриту зміну
        assertNotNull(closedShiftRecorded)
        assertEquals("shift-to-close", closedShiftRecorded?.id)

        // Відкрито діалог підсумків зміни
        assertTrue(vm.state.value.isShiftReportDialogOpen)
    }
}
