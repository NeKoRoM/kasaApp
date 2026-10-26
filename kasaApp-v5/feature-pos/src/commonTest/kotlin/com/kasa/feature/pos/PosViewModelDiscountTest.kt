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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelDiscountTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val coffee = Product(
        id = 10L,
        name = "Кава Арабіка",
        price = 100.00,
        taxGroup = TaxGroup.GROUP_A
    )

    private val tea = Product(
        id = 11L,
        name = "Чай Зелений",
        price = 50.00,
        taxGroup = TaxGroup.GROUP_A
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
            initialCatalog = listOf(coffee, tea)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSetItemDiscountCalculatesCorrectTotals() {
        // Додаємо 2 чашки кави по 100 грн = 200 грн (20000 коп)
        viewModel.onEvent(PosEvent.AddProduct(coffee, customQuantity = 2.0))
        assertEquals(20000L, viewModel.state.value.rawTotalKopecks)

        // Встановлюємо знижку на позицію 10%
        viewModel.onEvent(PosEvent.SetItemDiscount(itemIndex = 0, discountPercent = 10.0))

        val state = viewModel.state.value
        assertEquals(10.0, state.items[0].discountPercent)
        assertEquals(20000L, state.items[0].rawTotalKopecks)
        assertEquals(2000L, state.items[0].discountKopecks)
        assertEquals(18000L, state.items[0].totalKopecks)
        assertEquals(18000L, state.rawTotalKopecks)
        assertEquals(18000L, state.totalPayableKopecks)
    }

    @Test
    fun testSetReceiptDiscountCalculatesCorrectTotals() {
        viewModel.onEvent(PosEvent.AddProduct(coffee)) // 100.00 грн
        viewModel.onEvent(PosEvent.AddProduct(tea))    // 50.00 грн
        assertEquals(15000L, viewModel.state.value.rawTotalKopecks)

        // Знижка на весь чек 10% -> 150.00 - 15.00 = 135.00 грн (13500 коп)
        viewModel.onEvent(PosEvent.SetReceiptDiscount(10.0))

        val state = viewModel.state.value
        assertEquals(10.0, state.receiptDiscountPercent)
        assertEquals(15000L, state.rawTotalKopecks)
        assertEquals(13500L, state.totalPayableKopecks)
    }

    @Test
    fun testCombinedDiscountsAndCashRounding() {
        val oddProduct1 = Product(id = 21L, name = "Печиво", price = 33.33, taxGroup = TaxGroup.GROUP_A)
        val oddProduct2 = Product(id = 22L, name = "Цукерки", price = 44.44, taxGroup = TaxGroup.GROUP_A)

        viewModel.onEvent(PosEvent.AddProduct(oddProduct1)) // 3333 коп
        viewModel.onEvent(PosEvent.AddProduct(oddProduct2)) // 4444 коп

        // Знижка 10% на печиво: 3333 - 333 = 3000 коп
        viewModel.onEvent(PosEvent.SetItemDiscount(0, 10.0))
        // Сума без загальної знижки чека: 3000 + 4444 = 7444 коп
        assertEquals(7444L, viewModel.state.value.rawTotalKopecks)

        // Знижка на весь чек 5%: 7444 * 0.05 = 372.2 -> 372 коп знижки
        // До сплати до заокруглення: 7444 - 372 = 7072 коп
        viewModel.onEvent(PosEvent.SetReceiptDiscount(5.0))

        val state = viewModel.state.value
        // Готівкове заокруглення НБУ: 7072 коп закінчується на 2 -> 7070 коп (-2 коп)
        assertEquals(7070L, state.totalPayableKopecks)
        assertEquals(-2L, state.roundingDifferenceKopecks)

        // Оплата карткою: заокруглення не застосовується -> рівно 7072 коп
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        assertEquals(7072L, viewModel.state.value.totalPayableKopecks)
        assertEquals(0L, viewModel.state.value.roundingDifferenceKopecks)
    }

    @Test
    fun testDiscountPromptStateManagement() {
        assertNull(viewModel.state.value.activeDiscountPromptItemIndex)
        assertFalse(viewModel.state.value.isReceiptDiscountPromptOpen)

        viewModel.onEvent(PosEvent.PromptItemDiscount(0))
        assertEquals(0, viewModel.state.value.activeDiscountPromptItemIndex)

        viewModel.onEvent(PosEvent.PromptItemDiscount(null))
        assertNull(viewModel.state.value.activeDiscountPromptItemIndex)

        viewModel.onEvent(PosEvent.PromptReceiptDiscount(true))
        assertTrue(viewModel.state.value.isReceiptDiscountPromptOpen)

        viewModel.onEvent(PosEvent.PromptReceiptDiscount(false))
        assertFalse(viewModel.state.value.isReceiptDiscountPromptOpen)
    }

    @Test
    fun testClearReceiptResetsReceiptDiscount() {
        viewModel.onEvent(PosEvent.AddProduct(coffee))
        viewModel.onEvent(PosEvent.SetReceiptDiscount(15.0))
        assertEquals(15.0, viewModel.state.value.receiptDiscountPercent)

        viewModel.onEvent(PosEvent.ClearReceipt)
        assertEquals(0.0, viewModel.state.value.receiptDiscountPercent)
        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(0L, viewModel.state.value.totalPayableKopecks)
    }
}
