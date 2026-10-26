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

/**
 * Тести розділення підакцизних товарів на окремі позиції чека, захисту від дублікатів акцизних марок,
 * та обробки товарів із вільною ціною.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelExciseAndPriceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val wineProduct = Product(
        id = 201L,
        name = "Коньяк 5 зірок 0.5л",
        price = 220.00,
        barcodes = listOf("4820000002011"),
        taxGroup = TaxGroup.GROUP_A_EXCISE,
        uktzed = "2208200000"
    )

    private val freePriceService = Product(
        id = 301L,
        name = "Послуга доставки кур'єром",
        price = 0.00,
        barcodes = listOf("SERV01"),
        taxGroup = TaxGroup.GROUP_A
    )

    @BeforeTest
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        transport.connect("SIMULATED_TEST")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExciseProductMultiQuantity_splitsIntoSingleItems() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(wineProduct)
        )

        // Додаємо 3 пляшки коньяку за раз
        viewModel.onEvent(PosEvent.AddProduct(wineProduct, customQuantity = 3.0))
        advanceUntilIdle()

        val items = viewModel.state.value.items
        // Має бути створено 3 окремі позиції, кожна з кількістю 1.0 для індивідуальної марки
        assertEquals(3, items.size)
        items.forEach { item ->
            assertEquals(201L, item.productId)
            assertEquals(1.0, item.quantity)
            assertTrue(item.requiresExciseStamp)
            assertNull(item.exciseStamp)
        }
        assertEquals(0, viewModel.state.value.activeExcisePromptIndex)
    }

    @Test
    fun testPreventDuplicateExciseStampInReceipt() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(wineProduct)
        )

        // Додаємо 2 пляшки
        viewModel.onEvent(PosEvent.AddProduct(wineProduct, customQuantity = 2.0))
        advanceUntilIdle()

        // Вводимо марку для першої пляшки
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "AA11223344"))
        advanceUntilIdle()
        assertEquals("AA11223344", viewModel.state.value.items[0].exciseStamp)
        assertEquals(1, viewModel.state.value.activeExcisePromptIndex)

        // Намагаємося ввести ту саму марку для другої пляшки
        viewModel.onEvent(PosEvent.EnterExciseStamp(1, "AA11223344"))
        advanceUntilIdle()

        // Друга пляшка не повинна отримати дублікат, а статус має попередити про помилку
        assertNull(viewModel.state.value.items[1].exciseStamp)
        assertTrue(viewModel.state.value.statusMessage?.contains("вже додана") == true)
    }

    @Test
    fun testFreePriceProduct_triggersPricePromptAndAllowsUpdatingPrice() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(freePriceService)
        )

        // Додаємо послугу з нульовою ціною
        viewModel.onEvent(PosEvent.AddProduct(freePriceService))
        advanceUntilIdle()

        // Має автоматично відкритися запит ціни
        assertEquals(0, viewModel.state.value.activePricePromptIndex)

        // Касир вводить ціну 150.00 грн (15000 копійок)
        viewModel.onEvent(PosEvent.UpdateItemPrice(0, 15000L))
        advanceUntilIdle()

        val item = viewModel.state.value.items.first()
        assertEquals(15000L, item.priceKopecks)
        assertEquals(15000L, item.totalKopecks)
        assertEquals(15000L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testToggleNumpadDialog() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = emptyList()
        )

        assertFalse(viewModel.state.value.isNumpadDialogOpen)

        viewModel.onEvent(PosEvent.ToggleNumpadDialog(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isNumpadDialogOpen)

        viewModel.onEvent(PosEvent.ToggleNumpadDialog(false))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isNumpadDialogOpen)
    }

    @Test
    fun testPromptExciseStamp_setsAndClearsActiveIndex() = runTest(testDispatcher) {
        val viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(wineProduct)
        )

        // Спочатку закриваємо автоматичний промпт
        viewModel.onEvent(PosEvent.AddProduct(wineProduct))
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeExcisePromptIndex)

        viewModel.onEvent(PosEvent.DismissExcisePrompt)
        advanceUntilIdle()
        assertNull(viewModel.state.value.activeExcisePromptIndex)

        // Касир натискає "Ввести акциз" на рядку товару або на банері
        viewModel.onEvent(PosEvent.PromptExciseStamp(0))
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeExcisePromptIndex)

        // Введення марки
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "AA99887766"))
        advanceUntilIdle()
        assertNull(viewModel.state.value.activeExcisePromptIndex)
        assertEquals("AA99887766", viewModel.state.value.items[0].exciseStamp)
        assertFalse(viewModel.state.value.hasUnstampedExcise)
    }
}
