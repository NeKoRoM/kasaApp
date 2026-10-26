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
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelHeldReceiptsTest {

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
            section = 1
        ),
        Product(
            id = 2,
            name = "Кава в зернах",
            price = 200.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1
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
    fun testHoldCurrentReceiptAndRestore() = runTest {
        var lastPersistedList: List<HeldReceipt>? = null

        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts,
            onHeldReceiptsUpdated = { lastPersistedList = it }
        )

        // 1. Формуємо чек із 2 товарів та знижкою 10%
        vm.onEvent(PosEvent.AddProduct(sampleProducts[0], customQuantity = 2.0)) // 60.00 грн
        vm.onEvent(PosEvent.AddProduct(sampleProducts[1], customQuantity = 1.0)) // 200.00 грн
        vm.onEvent(PosEvent.SetReceiptDiscount(10.0))                           // 260.00 - 10% = 234.00 грн

        assertEquals(2, vm.state.value.items.size)
        assertEquals(23400L, vm.state.value.totalPayableKopecks)

        // 2. Відкладаємо чек з приміткою
        vm.onEvent(PosEvent.HoldCurrentReceipt(note = "Клієнт пішов за гаманцем"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Перевіряємо: кошик очистився
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(0L, vm.state.value.totalPayableKopecks)

        // Перевіряємо: з'явився 1 відкладений чек
        assertEquals(1, vm.state.value.heldReceipts.size)
        val held = vm.state.value.heldReceipts[0]
        assertEquals("Клієнт пішов за гаманцем", held.note)
        assertEquals(2, held.receipt.items.size)
        assertEquals(23400L, held.receipt.totalPayableKopecks)
        assertEquals(1, lastPersistedList?.size)

        // 3. Обслуговуємо іншого покупця в порожньому чеку
        vm.onEvent(PosEvent.AddProduct(sampleProducts[0], customQuantity = 1.0))
        assertEquals(1, vm.state.value.items.size)

        // 4. Повернення першого покупця: відновлюємо відкладений чек
        vm.onEvent(PosEvent.RestoreHeldReceipt(held.id))
        testDispatcher.scheduler.advanceUntilIdle()

        // Перевіряємо: відновлено початковий кошик та знижку
        assertEquals(2, vm.state.value.items.size)
        assertEquals(23400L, vm.state.value.totalPayableKopecks)
        assertEquals(10.0, vm.state.value.receiptDiscountPercent)

        // Список відкладених чеків став порожнім
        assertTrue(vm.state.value.heldReceipts.isEmpty())
        assertEquals(0, lastPersistedList?.size)
    }

    @Test
    fun testDeleteHeldReceipt() = runTest {
        var lastPersistedList: List<HeldReceipt>? = null

        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts,
            onHeldReceiptsUpdated = { lastPersistedList = it }
        )

        vm.onEvent(PosEvent.AddProduct(sampleProducts[0]))
        vm.onEvent(PosEvent.HoldCurrentReceipt(note = "Відмовний чек"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.state.value.heldReceipts.size)
        val heldId = vm.state.value.heldReceipts[0].id

        // Видаляємо відкладений чек
        vm.onEvent(PosEvent.DeleteHeldReceipt(heldId))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.heldReceipts.isEmpty())
        assertEquals(0, lastPersistedList?.size)
    }

    @Test
    fun testHoldEmptyReceiptDoesNothing() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        vm.onEvent(PosEvent.HoldCurrentReceipt(note = "Порожньо"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.heldReceipts.isEmpty())
    }
}
