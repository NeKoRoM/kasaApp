package com.kasa.feature.pos

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
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelQuantityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val candy = Product(
        id = 100,
        name = "Цукерки Шоколадні",
        price = 240.00, // 240 грн/кг
        isWeighted = true,
        taxGroup = TaxGroup.GROUP_A
    )

    private val water = Product(
        id = 101,
        name = "Вода Мінеральна 1.5л",
        price = 18.50,
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
    fun testPromptQuantityOpenAndDismiss() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(candy, water)
        )

        vm.onEvent(PosEvent.AddProduct(water))
        assertNull(vm.state.value.activeQuantityPromptIndex)

        vm.onEvent(PosEvent.PromptQuantity(0))
        assertEquals(0, vm.state.value.activeQuantityPromptIndex)

        vm.onEvent(PosEvent.PromptQuantity(null))
        assertNull(vm.state.value.activeQuantityPromptIndex)
    }

    @Test
    fun testWeightedProductAutoPromptsWeight() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(candy, water)
        )

        vm.onEvent(PosEvent.AddProduct(candy))
        assertEquals(0, vm.state.value.activeQuantityPromptIndex)
    }

    @Test
    fun testUpdateQuantityFractionalWeight() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(candy)
        )

        vm.onEvent(PosEvent.AddProduct(candy))
        assertEquals(1.0, vm.state.value.items[0].quantity)
        assertEquals(24000L, vm.state.value.rawTotalKopecks)

        // Встановлюємо вагу 0.350 кг (350 грам)
        vm.onEvent(PosEvent.UpdateQuantity(itemIndex = 0, quantity = 0.350))

        val item = vm.state.value.items[0]
        assertEquals(0.350, item.quantity)
        // 0.350 * 240.00 грн = 84.00 грн = 8400 коп
        assertEquals(8400L, item.totalKopecks)
        assertEquals(8400L, vm.state.value.totalPayableKopecks)
    }

    @Test
    fun testUpdateQuantityZeroRemovesItem() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(water)
        )

        vm.onEvent(PosEvent.AddProduct(water))
        assertEquals(1, vm.state.value.items.size)

        // Встановлення 0 видаляє позицію
        vm.onEvent(PosEvent.UpdateQuantity(itemIndex = 0, quantity = 0.0))
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(0L, vm.state.value.totalPayableKopecks)
    }
}
