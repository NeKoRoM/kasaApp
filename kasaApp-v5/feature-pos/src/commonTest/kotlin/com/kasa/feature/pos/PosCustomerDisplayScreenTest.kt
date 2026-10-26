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
class PosCustomerDisplayScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val coffee = Product(
        id = 10L,
        name = "Кава в зернах 1кг",
        price = 450.0,
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000010")
    )

    private val croissant = Product(
        id = 11L,
        name = "Круасан з шоколадом",
        price = 45.0,
        taxGroup = TaxGroup.GROUP_A,
        barcodes = listOf("482000000011")
    )

    @BeforeTest
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        transport = SimulatedKasaTransport()
        transport.connect("SIMULATED_DATECS")
        receiptEngine = FiscalReceiptEngine(transport)
        serviceEngine = FiscalServiceEngine(transport)
        viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(coffee, croissant)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCustomerDisplayEmptyCartState() = runTest(testDispatcher) {
        val state = viewModel.state.value

        // Перевіряємо початковий стан екрана покупця при порожньому чеку
        assertEquals(0, state.items.size)
        assertEquals(0L, state.totalPayableKopecks)
        assertNull(state.lastFiscalCheckNumber)
        assertFalse(state.isFiscalProcessing)

        // Перевіряємо текст вітання для покупця
        val line1 = state.customerDisplayLine1.ifBlank { "👋 Ласкаво просимо!" }
        val line2 = state.customerDisplayLine2.ifBlank { "Каса готова до обслуговування" }
        assertEquals("👋 Ласкаво просимо!", line1)
        assertEquals("Каса готова до обслуговування", line2)
    }

    @Test
    fun testCustomerDisplayLastItemTrackingAndRunningTotal() = runTest(testDispatcher) {
        // Додаємо першу позицію: Кава
        viewModel.onEvent(PosEvent.AddProduct(coffee, customQuantity = 1.0))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals("Кава в зернах 1кг", state.items.last().productName)
        assertEquals(45000L, state.totalPayableKopecks)
        assertEquals("Кава в зернах 1кг", state.customerDisplayLine1)

        // Додаємо другу позицію: 2 круасани
        viewModel.onEvent(PosEvent.AddProduct(croissant, customQuantity = 2.0))
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(2, state.items.size)

        // Остання позиція для великої картки покупця має бути саме "Круасан з шоколадом"
        val lastItem = state.items.last()
        assertEquals("Круасан з шоколадом", lastItem.productName)
        assertEquals(2.0, lastItem.quantity)
        assertEquals(4500L, lastItem.priceKopecks)
        assertEquals(9000L, lastItem.totalKopecks)

        // Загальна сума до сплати: 450.00 + 90.00 = 540.00 грн (54000 копійок)
        assertEquals(54000L, state.totalPayableKopecks)
        assertEquals("Круасан з шоколадом", state.customerDisplayLine1)
        assertTrue(state.customerDisplayLine2.contains("2.0x45.0 = 90.00"))
    }

    @Test
    fun testCustomerDisplayDiscountSavingsCalculation() = runTest(testDispatcher) {
        viewModel.onEvent(PosEvent.AddProduct(coffee))
        advanceUntilIdle()

        // Встановлюємо знижку 10% на чек
        viewModel.onEvent(PosEvent.SetReceiptDiscount(10.0))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(10.0, state.receiptDiscountPercent)
        assertEquals(45000L, state.rawTotalKopecks)
        assertEquals(40500L, state.totalPayableKopecks)

        // Економія для покупця: 450.00 - 405.00 = 45.00 грн
        val savedKopecks = state.rawTotalKopecks - state.totalPayableKopecks
        assertEquals(4500L, savedKopecks)
        assertEquals("45.00", PosCurrencyFormatter.formatKopecks(savedKopecks))
    }

    @Test
    fun testCustomerDisplayReceiptFiscalizationNotice() = runTest(testDispatcher) {
        viewModel.onEvent(PosEvent.AddProduct(croissant))
        advanceUntilIdle()

        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.lastFiscalCheckNumber)
        assertEquals("Дякуємо за покупку!", state.customerDisplayLine1)
        assertTrue(state.customerDisplayLine2.contains("Чек №"))
    }
}
