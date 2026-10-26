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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тести мобільних жестів змахування (Swipe-To-Dismiss),
 * пакетного сканування штрих-кодів та аудіо-зворотного зв'язку для Android та iOS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosMobileSwipeAndScanTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val product1 = Product(
        id = 201L,
        name = "Кава Амерікано",
        price = 35.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "порц",
        barcodes = listOf("4820000002011")
    )

    private val product2 = Product(
        id = 202L,
        name = "Круасан мигдалевий",
        price = 50.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "шт",
        barcodes = listOf("4820000002022")
    )

    private val exciseProduct = Product(
        id = 203L,
        name = "Вино сухе 0.75л",
        price = 180.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "пляш",
        barcodes = listOf("4820000002033"),
        uktzed = "2204101100"
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transport = SimulatedKasaTransport()
        receiptEngine = FiscalReceiptEngine(transport)
        serviceEngine = FiscalServiceEngine(transport)
        viewModel = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(product1, product2, exciseProduct)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPosAudioFeedbackExecutionAndMuteToggle() {
        val originalState = PosAudioFeedback.isSoundEnabled

        // Перевірка роботи аудіо сигналів без викидання винятків
        PosAudioFeedback.isSoundEnabled = true
        PosAudioFeedback.playScanBeep()
        PosAudioFeedback.playSuccessBeep()
        PosAudioFeedback.playErrorBeep()

        // Перевірка режиму "без звуку"
        PosAudioFeedback.isSoundEnabled = false
        assertFalse(PosAudioFeedback.isSoundEnabled)
        PosAudioFeedback.playScanBeep()
        PosAudioFeedback.playSuccessBeep()
        PosAudioFeedback.playErrorBeep()

        PosAudioFeedback.isSoundEnabled = originalState
    }

    @Test
    fun testContinuousBatchScanningWorkflow() = runTest {
        // Касир на телефоні увімкнув пакетний режим і сканує 3 товари поспіль
        viewModel.onEvent(PosEvent.ScanBarcode("4820000002011"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(201L, viewModel.state.value.items[0].productId)

        viewModel.onEvent(PosEvent.ScanBarcode("4820000002022"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)
        assertEquals(202L, viewModel.state.value.items[1].productId)

        // Повторне сканування штучного товару збільшує його кількість
        viewModel.onEvent(PosEvent.ScanBarcode("4820000002011"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)
        assertEquals(2.0, viewModel.state.value.items[0].quantity)

        // Сума: (35.0 * 2) + 50.0 = 120.0 грн = 12000 коп
        assertEquals(12000L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testSwipeToRemoveItemFromCart() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(product1))
        viewModel.onEvent(PosEvent.AddProduct(product2))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)

        // Симуляція завершення змахування вліво (SwipeToDismissBox confirmed)
        // Видалення першого товару
        viewModel.onEvent(PosEvent.RemoveItem(0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(202L, viewModel.state.value.items[0].productId)
        assertEquals(5000L, viewModel.state.value.totalPayableKopecks)

        // Видалення останнього товару
        viewModel.onEvent(PosEvent.RemoveItem(0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(0L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testBatchMultiplierScanOnMobile() = runTest {
        // Касир сканує шорткат кількості через сканер '4*202'
        viewModel.onEvent(PosEvent.ScanBarcode("4*202"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(202L, viewModel.state.value.items[0].productId)
        assertEquals(4.0, viewModel.state.value.items[0].quantity)
        assertEquals(20000L, viewModel.state.value.totalPayableKopecks)
    }

    @Test
    fun testExciseProductHandlingInMobileFlow() = runTest {
        // Сканування підакцизного товару з акцизною маркою
        viewModel.onEvent(PosEvent.ScanBarcode("4820000002033"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        val item = viewModel.state.value.items[0]
        assertTrue(item.requiresExciseStamp)

        // Введення е-акцизу Дія
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "https://t.gov.ua/AB12345678"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://t.gov.ua/AB12345678", viewModel.state.value.items[0].exciseStamp)
        assertTrue(viewModel.state.value.canPay)
    }
}
