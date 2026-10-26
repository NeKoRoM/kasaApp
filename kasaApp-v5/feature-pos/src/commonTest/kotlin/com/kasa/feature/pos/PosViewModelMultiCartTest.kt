package com.kasa.feature.pos

import com.kasa.model.*
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelMultiCartTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val sampleProducts = listOf(
        Product(
            id = 1,
            name = "Хліб Тостовий",
            price = 30.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("4820000000011")
        ),
        Product(
            id = 2,
            name = "Сир Твердий",
            price = 150.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("4820000000022")
        ),
        Product(
            id = 3,
            name = "Яблука Голден",
            price = 45.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            isWeighted = true,
            unitOfMeasurement = "кг",
            barcodes = listOf("4820000000033")
        )
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
    fun testSwitchCartPreservesIndividualItemsAndDiscount() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        // 1. У Чек 1 додаємо Хліб та встановлюємо знижку 10%
        assertEquals(0, vm.state.value.activeCartIndex)
        vm.onEvent(PosEvent.AddProduct(sampleProducts[0]))
        vm.onEvent(PosEvent.SetReceiptDiscount(10.0))

        assertEquals(1, vm.state.value.items.size)
        assertEquals("Хліб Тостовий", vm.state.value.items[0].productName)
        assertEquals(10.0, vm.state.value.receiptDiscountPercent)
        assertEquals(2700L, vm.state.value.totalPayableKopecks)

        // 2. Перемикаємося на Чек 2
        vm.onEvent(PosEvent.SwitchCart(1))
        assertEquals(1, vm.state.value.activeCartIndex)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(0.0, vm.state.value.receiptDiscountPercent)
        assertEquals(0L, vm.state.value.totalPayableKopecks)

        // 3. У Чек 2 додаємо Сир
        vm.onEvent(PosEvent.AddProduct(sampleProducts[1]))
        assertEquals(1, vm.state.value.items.size)
        assertEquals("Сир Твердий", vm.state.value.items[0].productName)
        assertEquals(15000L, vm.state.value.totalPayableKopecks)

        // 4. Повертаємося на Чек 1
        vm.onEvent(PosEvent.SwitchCart(0))
        assertEquals(0, vm.state.value.activeCartIndex)
        assertEquals(1, vm.state.value.items.size)
        assertEquals("Хліб Тостовий", vm.state.value.items[0].productName)
        assertEquals(10.0, vm.state.value.receiptDiscountPercent)
        assertEquals(2700L, vm.state.value.totalPayableKopecks)

        // 5. Перевіряємо збереження стану в списку carts
        assertEquals(3, vm.state.value.carts.size)
        assertEquals(1, vm.state.value.carts[0].items.size)
        assertEquals(1, vm.state.value.carts[1].items.size)
        assertEquals(0, vm.state.value.carts[2].items.size)
    }

    @Test
    fun testClearReceiptOnlyClearsActiveCart() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        // Додаємо товари в Чек 1 і Чек 2
        vm.onEvent(PosEvent.AddProduct(sampleProducts[0]))
        vm.onEvent(PosEvent.SwitchCart(1))
        vm.onEvent(PosEvent.AddProduct(sampleProducts[1]))

        // Очищаємо Чек 2
        vm.onEvent(PosEvent.ClearReceipt)
        assertTrue(vm.state.value.items.isEmpty())

        // Перемикаємося на Чек 1 — він має залишитися недоторканим
        vm.onEvent(PosEvent.SwitchCart(0))
        assertEquals(1, vm.state.value.items.size)
        assertEquals("Хліб Тостовий", vm.state.value.items[0].productName)
    }

    @Test
    fun testFiscalizeReceiptOnlyClearsActiveCart() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        // Наповнюємо Чек 1 та Чек 2
        vm.onEvent(PosEvent.AddProduct(sampleProducts[0]))
        vm.onEvent(PosEvent.SwitchCart(1))
        vm.onEvent(PosEvent.AddProduct(sampleProducts[1]))

        // Фіскалізуємо Чек 2
        vm.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        // Чек 2 після фіскалізації очищено
        assertTrue(vm.state.value.items.isEmpty())

        // Перемикаємося на Чек 1 — товар все ще на місці
        vm.onEvent(PosEvent.SwitchCart(0))
        assertEquals(1, vm.state.value.items.size)
        assertEquals("Хліб Тостовий", vm.state.value.items[0].productName)
    }

    @Test
    fun testWeightedProductWithoutScaleAutoPromptsWeight() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        // Додаємо ваговий товар "Яблука Голден" без стабільних ваг
        vm.onEvent(PosEvent.AddProduct(sampleProducts[2]))

        assertEquals(1, vm.state.value.items.size)
        // Має автоматично встановити activeQuantityPromptIndex на доданий товар
        assertNotNull(vm.state.value.activeQuantityPromptIndex)
        assertEquals(0, vm.state.value.activeQuantityPromptIndex)
    }

    @Test
    fun testBarcodeSheetDialogToggle() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        vm.onEvent(PosEvent.ToggleBarcodeSheetDialog(true))
        assertTrue(vm.state.value.isBarcodeSheetDialogOpen)

        vm.onEvent(PosEvent.ToggleBarcodeSheetDialog(false))
        assertTrue(!vm.state.value.isBarcodeSheetDialogOpen)
    }
}
