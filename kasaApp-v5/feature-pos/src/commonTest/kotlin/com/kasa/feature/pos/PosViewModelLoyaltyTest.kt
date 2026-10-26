package com.kasa.feature.pos

import com.kasa.model.Customer
import com.kasa.model.Product
import com.kasa.model.Receipt
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
 * Модульні тести інтеграції програми лояльності у касове місце (PosViewModel).
 * Перевіряє сканування карток/телефонів, перерахунок знижок та збереження в фіскальному чеку.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelLoyaltyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transport = SimulatedKasaTransport()
    private val receiptEngine = FiscalReceiptEngine(transport)
    private val serviceEngine = FiscalServiceEngine(transport)

    private val sampleCustomer = Customer(
        id = 1L,
        name = "Олена Коваль",
        phone = "+380671112233",
        cardBarcode = "2900000000018",
        discountPercent = 10.0
    )

    private val sampleProduct = Product(
        id = 101,
        name = "Кава в зернах 1кг",
        price = 500.00,
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
    fun testScanCustomerBarcodeAttachesCustomerAndAppliesDiscount() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(sampleProduct),
            initialCustomers = listOf(sampleCustomer)
        )

        // Додаємо товар у кошик: 500.00 грн = 50000 коп
        vm.onEvent(PosEvent.AddProduct(sampleProduct))
        advanceUntilIdle()

        assertEquals(50000L, vm.state.value.rawTotalKopecks)
        assertEquals(0.0, vm.state.value.receiptDiscountPercent)
        assertNull(vm.state.value.selectedCustomer)

        // Скануємо штрихкод картки лояльності
        vm.onEvent(PosEvent.ScanBarcode("2900000000018"))
        advanceUntilIdle()

        assertNotNull(vm.state.value.selectedCustomer)
        assertEquals(1L, vm.state.value.selectedCustomer?.id)
        assertEquals(10.0, vm.state.value.receiptDiscountPercent)
        // 50000 коп - 10% = 45000 коп
        assertEquals(45000L, vm.state.value.totalPayableKopecks)
    }

    @Test
    fun testScanCustomerPhoneAttachesCustomer() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(sampleProduct),
            initialCustomers = listOf(sampleCustomer)
        )

        vm.onEvent(PosEvent.AddProduct(sampleProduct))
        advanceUntilIdle()

        // Скануємо номер телефону клієнта
        vm.onEvent(PosEvent.ScanBarcode("+380671112233"))
        advanceUntilIdle()

        assertEquals("Олена Коваль", vm.state.value.selectedCustomer?.name)
        assertEquals(10.0, vm.state.value.receiptDiscountPercent)
    }

    @Test
    fun testDetachCustomerResetsDiscount() = runTest {
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(sampleProduct),
            initialCustomers = listOf(sampleCustomer)
        )

        vm.onEvent(PosEvent.AddProduct(sampleProduct))
        vm.onEvent(PosEvent.AttachCustomer(sampleCustomer))
        advanceUntilIdle()

        assertEquals(10.0, vm.state.value.receiptDiscountPercent)
        assertEquals(45000L, vm.state.value.totalPayableKopecks)

        // Відв'язуємо клієнта
        vm.onEvent(PosEvent.DetachCustomer)
        advanceUntilIdle()

        assertNull(vm.state.value.selectedCustomer)
        assertEquals(0.0, vm.state.value.receiptDiscountPercent)
        assertEquals(50000L, vm.state.value.totalPayableKopecks)
    }

    @Test
    fun testFiscalizeReceiptRecordsCustomerInJournalAndResetsCustomer() = runTest {
        var recordedReceipt: Receipt? = null
        val vm = PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = transport,
            initialCatalog = listOf(sampleProduct),
            initialCustomers = listOf(sampleCustomer),
            onReceiptFiscalized = { recordedReceipt = it }
        )

        vm.onEvent(PosEvent.AddProduct(sampleProduct))
        vm.onEvent(PosEvent.AttachCustomer(sampleCustomer))
        advanceUntilIdle()

        // Фіскалізуємо чек
        vm.onEvent(PosEvent.FiscalizeReceipt)
        advanceUntilIdle()

        assertNotNull(recordedReceipt)
        assertEquals("Олена Коваль", recordedReceipt?.customer?.name)
        assertEquals("Олена Коваль", recordedReceipt?.customerName)
        assertEquals(10.0, recordedReceipt?.discountPercent)
        assertEquals(45000L, recordedReceipt?.totalPayableKopecks)

        // Після закриття чека картка клієнта має відкріпитися для наступного покупця
        assertNull(vm.state.value.selectedCustomer)
        assertEquals(0.0, vm.state.value.receiptDiscountPercent)
        assertEquals(0, vm.state.value.items.size)
    }
}

