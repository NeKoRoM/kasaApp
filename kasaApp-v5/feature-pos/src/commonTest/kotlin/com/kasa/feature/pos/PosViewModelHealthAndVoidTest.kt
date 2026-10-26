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

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelHealthAndVoidTest {

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
    fun testHealthCheckUpdatesStatusAndBlocksPaymentWhenPaperOut() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread)
        )

        vm.onEvent(PosEvent.AddProduct(bread))
        advanceUntilIdle()
        assertTrue(vm.state.value.canPay)

        // Симулюємо закінчення паперу
        transport.simulatePaperOut = true
        vm.onEvent(PosEvent.CheckKasaHealth)
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.kasaStatus)
        assertTrue(state.isReceiptPaperOut)
        assertFalse(state.canPay) // Оплата заблокована через відсутність паперу
    }

    @Test
    fun testHealthCheckBlocksPaymentWhenCoverOpenOrShiftExpired() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(bread)
        )

        vm.onEvent(PosEvent.AddProduct(bread))
        advanceUntilIdle()

        // Відкрита кришка
        transport.simulateCoverOpen = true
        vm.onEvent(PosEvent.CheckKasaHealth)
        advanceUntilIdle()

        assertTrue(vm.state.value.isCoverOpen)
        assertFalse(vm.state.value.canPay)

        // Вичерпана 24-годинна зміна
        transport.simulateCoverOpen = false
        transport.simulateShiftExpired = true
        vm.onEvent(PosEvent.CheckKasaHealth)
        advanceUntilIdle()

        assertFalse(vm.state.value.isCoverOpen)
        assertTrue(vm.state.value.is24HoursShiftExpired)
        assertFalse(vm.state.value.canPay)
    }

    @Test
    fun testStuckReceiptDetectionAndVoidRecovery() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport
        )

        // Імітуємо завислий відкритий чек в апараті
        transport.sendFrame(
            com.kasa.protocol.frame.DatecsFrameBuilder().buildFrame(
                com.kasa.protocol.command.DatecsCommand.OpenReceipt()
            )
        )
        assertTrue(transport.isReceiptOpen)

        // Опитуємо апарат
        vm.onEvent(PosEvent.CheckKasaHealth)
        advanceUntilIdle()

        assertTrue(vm.state.value.isStuckReceiptInRro)

        // Касир тисне анулювати чек (Void)
        vm.onEvent(PosEvent.VoidReceipt)
        advanceUntilIdle()

        assertFalse(transport.isReceiptOpen)
        assertFalse(vm.state.value.isStuckReceiptInRro)
        assertEquals("Чек успішно анульовано", vm.state.value.statusMessage)
    }
}
