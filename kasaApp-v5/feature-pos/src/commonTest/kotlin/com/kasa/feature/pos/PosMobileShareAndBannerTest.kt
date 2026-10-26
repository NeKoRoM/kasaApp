package com.kasa.feature.pos

import com.kasa.model.FiscalUrlGenerator
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.model.ReceiptTextRenderer
import com.kasa.model.ReceiptType
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тести мобільних статусних банерів (PosStatusBanner) та генерації
 * тексту електронних чеків для надсилання клієнтам (Viber, Telegram, SMS).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosMobileShareAndBannerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val alcoholProduct1 = Product(
        id = 401L,
        name = "Горілка Преміум 0.5л",
        price = 120.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "пляш",
        barcodes = listOf("4820000004011"),
        uktzed = "2208601100"
    )

    private val alcoholProduct2 = Product(
        id = 402L,
        name = "Вино Каберне 0.75л",
        price = 160.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "пляш",
        barcodes = listOf("4820000004022"),
        uktzed = "2204211100"
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
            initialCatalog = listOf(alcoholProduct1, alcoholProduct2)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStatusBannerDismissEvent() = runTest {
        // Симуляція події оновлення пошуку неіснуючого товару
        viewModel.onEvent(PosEvent.ScanBarcode("9999999999999"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.statusMessage)
        assertTrue(viewModel.state.value.statusMessage!!.contains("не знайдено"))

        // Закриття банера (користувач тапнув [✕] або таймер 3.5с)
        viewModel.onEvent(PosEvent.DismissStatusMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.statusMessage)
    }

    @Test
    fun testDuplicateExciseStampTriggersWarningBanner() = runTest {
        // Додаємо дві підакцизні пляшки
        viewModel.onEvent(PosEvent.AddProduct(alcoholProduct1))
        viewModel.onEvent(PosEvent.AddProduct(alcoholProduct2))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)

        // Встановлюємо акцизну марку на першу позицію
        viewModel.onEvent(PosEvent.EnterExciseStamp(0, "https://t.gov.ua/AA11223344"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Спроба використати ту ж саму акцизну марку для другої позиції
        viewModel.onEvent(PosEvent.EnterExciseStamp(1, "https://t.gov.ua/AA11223344"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Перевіряємо, що з'явився попереджувальний банер для касира
        val message = viewModel.state.value.statusMessage
        assertNotNull(message)
        assertTrue(message.contains("УВАГА"))
        assertTrue(message.contains("вже додана до позиції"))
    }

    @Test
    fun testReceiptTextRenderingForMobileShare() {
        viewModel.onEvent(PosEvent.AddProduct(alcoholProduct1))
        testDispatcher.scheduler.advanceUntilIdle()

        val receipt = Receipt(
            id = "test-check-001",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = viewModel.state.value.items,
            receiptType = ReceiptType.SALE,
            fiscalCheckNumber = "000123"
        )

        val text = ReceiptTextRenderer.render(
            receipt = receipt,
            headerLines = listOf("КАФЕ 'КИЇВ'", "вул. Хрещатик, 1")
        )

        assertNotNull(text)
        assertTrue(text.contains("КАФЕ 'КИЇВ'"))
        assertTrue(text.contains("Горілка Преміум 0.5л"))
        assertTrue(text.contains("120.00"))
        assertTrue(text.contains("ДО ОПЛАТИ"))
    }

    @Test
    fun testTaxUrlGenerationForMobileCustomerVerification() {
        val receipt = Receipt(
            id = "receipt-dps-001",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = emptyList(),
            fiscalCheckNumber = "456789",
            fiscalRegisterNumber = "3000123456"
        )

        val taxUrl = FiscalUrlGenerator.generateTaxVerificationUrl(receipt)
        assertNotNull(taxUrl)
        assertTrue(taxUrl.contains("cabinet.tax.gov.ua"))
        assertTrue(taxUrl.contains("fn=3000123456"))
        assertTrue(taxUrl.contains("id=456789"))
    }
}
