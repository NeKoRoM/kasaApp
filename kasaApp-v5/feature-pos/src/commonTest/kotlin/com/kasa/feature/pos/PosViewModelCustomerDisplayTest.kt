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
class PosViewModelCustomerDisplayTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val milk = Product(
        id = 2,
        name = "Молоко 2.5% 900г",
        price = 38.00,
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
    fun testManualCustomerDisplayAndClear() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport
        )

        vm.onEvent(PosEvent.DisplayCustomerText("Добрий день!", "Знижка 10%"))
        advanceUntilIdle()

        assertEquals("Добрий день!", vm.state.value.customerDisplayLine1)
        assertEquals("Знижка 10%", vm.state.value.customerDisplayLine2)
        assertEquals("Добрий день!", transport.simulatedDisplayLines[1])
        assertEquals("Знижка 10%", transport.simulatedDisplayLines[2])

        vm.onEvent(PosEvent.ClearCustomerDisplay)
        advanceUntilIdle()

        assertEquals("", vm.state.value.customerDisplayLine1)
        assertEquals("", vm.state.value.customerDisplayLine2)
        assertTrue(transport.simulatedDisplayLines.isEmpty())
    }

    @Test
    fun testAddProductUpdatesCustomerDisplay() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(milk)
        )

        vm.onEvent(PosEvent.AddProduct(milk, customQuantity = 2.0))
        advanceUntilIdle()

        assertEquals("Молоко 2.5% 900г", vm.state.value.customerDisplayLine1)
        assertEquals("2.0x38.0 = 76.00", vm.state.value.customerDisplayLine2)
        assertEquals("Молоко 2.5% 900г", transport.simulatedDisplayLines[1])
        assertEquals("2.0x38.0 = 76.00", transport.simulatedDisplayLines[2])
    }

    @Test
    fun testClearReceiptSendsWelcomeGreeting() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(milk)
        )

        vm.onEvent(PosEvent.AddProduct(milk))
        advanceUntilIdle()

        vm.onEvent(PosEvent.ClearReceipt)
        advanceUntilIdle()

        assertEquals("Ласкаво просимо!", vm.state.value.customerDisplayLine1)
        assertEquals("Каса вільна", vm.state.value.customerDisplayLine2)
        assertEquals("Ласкаво просимо!", transport.simulatedDisplayLines[1])
        assertEquals("Каса вільна", transport.simulatedDisplayLines[2])
    }

    @Test
    fun testFiscalizeReceiptSendsThankYouMessage() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(milk)
        )

        vm.onEvent(PosEvent.AddProduct(milk))
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        assertEquals("Дякуємо за покупку!", vm.state.value.customerDisplayLine1)
        assertTrue(vm.state.value.customerDisplayLine2.contains("Чек №"))
        assertEquals("Дякуємо за покупку!", transport.simulatedDisplayLines[1])
        assertTrue(transport.simulatedDisplayLines[2]?.contains("Чек №") == true)
    }
}
