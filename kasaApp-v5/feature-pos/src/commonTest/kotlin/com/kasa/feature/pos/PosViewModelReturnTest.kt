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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelReturnTest {

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
    fun testManualSetReceiptType() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        assertEquals(ReceiptType.SALE, vm.state.value.receiptType)

        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        assertEquals(ReceiptType.RETURN, vm.state.value.receiptType)

        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.SALE))
        assertEquals(ReceiptType.SALE, vm.state.value.receiptType)
    }

    @Test
    fun testLoadReceiptForReturn() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        val pastReceipt = Receipt(
            id = "test-receipt-001",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(
                ReceiptItem.fromProduct(sampleProducts[0], quantity = 2.0), // 60.00 грн
                ReceiptItem.fromProduct(sampleProducts[1], quantity = 1.0)  // 150.00 грн
            ),
            paymentType = PaymentType.CASH,
            receiptType = ReceiptType.SALE,
            fiscalCheckNumber = "000456",
            discountPercent = 10.0 // 210.00 - 10% = 189.00 грн -> заокруглення 189.00 грн
        )

        // Відкриваємо журнал і завантажуємо чек на повернення
        vm.onEvent(PosEvent.ToggleJournalDialog)
        assertTrue(vm.state.value.isJournalDialogOpen)

        vm.onEvent(PosEvent.LoadReceiptForReturn(pastReceipt))

        val state = vm.state.value
        assertFalse(state.isJournalDialogOpen)
        assertEquals(ReceiptType.RETURN, state.receiptType)
        assertEquals(PaymentType.CASH, state.paymentType)
        assertEquals(10.0, state.receiptDiscountPercent)
        assertEquals(2, state.items.size)
        assertEquals(21000L, state.rawTotalKopecks)
        assertEquals(18900L, state.totalPayableKopecks)
        assertTrue(state.statusMessage?.contains("000456") == true)
        assertTrue(state.canPay)
    }

    @Test
    fun testFiscalizeReturnReceiptResetsStateToSale() = runTest {
        var completedReceipt: Receipt? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts,
            onReceiptFiscalized = { completedReceipt = it }
        )

        vm.onEvent(PosEvent.AddProduct(sampleProducts[0], customQuantity = 1.0))
        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        assertEquals(ReceiptType.RETURN, vm.state.value.receiptType)

        vm.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        // Перевіряємо, що чек фіскалізувався як повернення
        assertEquals(ReceiptType.RETURN, completedReceipt?.receiptType)
        assertEquals(3000L, completedReceipt?.totalPayableKopecks)

        // Перевіряємо, що після фіскалізації стан очистився і повернувся в режим SALE
        val state = vm.state.value
        assertEquals(ReceiptType.SALE, state.receiptType)
        assertTrue(state.items.isEmpty())
        assertEquals(0L, state.totalPayableKopecks)
    }

    @Test
    fun testClearReceiptResetsReceiptTypeToSale() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )

        vm.onEvent(PosEvent.AddProduct(sampleProducts[0]))
        vm.onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
        assertEquals(ReceiptType.RETURN, vm.state.value.receiptType)

        vm.onEvent(PosEvent.ClearReceipt)
        assertEquals(ReceiptType.SALE, vm.state.value.receiptType)
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun testScanReceiptBarcode_automaticallyLoadsReceiptForReturn() = runTest {
        val pastReceipt = Receipt(
            id = "REC-999",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = listOf(ReceiptItem.fromProduct(sampleProducts[0], quantity = 1.0)),
            paymentType = PaymentType.CASH,
            receiptType = ReceiptType.SALE,
            fiscalCheckNumber = "008899"
        )
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = sampleProducts
        )
        vm.onEvent(PosEvent.UpdateJournal(listOf(pastReceipt)))

        // Скануємо номер фіскального чека з паперового роздруку
        vm.onEvent(PosEvent.ScanBarcode("008899"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Чек повинен автоматично завантажитись на повернення
        val state = vm.state.value
        assertEquals(ReceiptType.RETURN, state.receiptType)
        assertEquals(1, state.items.size)
        assertEquals("Хліб Тостовий", state.items[0].productName)
    }
}

