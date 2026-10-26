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

/**
 * Модульні тести калькулятора решти та готівкових розрахунків:
 * - Точний розрахунок решти при готівковій оплаті;
 * - Блокування фіскалізації при недостатній сумі готівки (isCashShort);
 * - Скидання введеної готівки при очищенні чека або після успішної фіскалізації.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosCashChangeCalculatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transport: SimulatedKasaTransport
    private lateinit var receiptEngine: FiscalReceiptEngine
    private lateinit var serviceEngine: FiscalServiceEngine
    private lateinit var viewModel: PosViewModel

    private val testProduct = Product(
        id = 201L,
        name = "Кава Американо",
        price = 45.0,
        taxGroup = TaxGroup.GROUP_A,
        unitOfMeasurement = "порц",
        barcodes = listOf("482000000999")
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
            initialCatalog = listOf(testProduct)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExactCashPaymentCalculatesZeroChangeAndAllowsPayment() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4500L, viewModel.state.value.totalPayableKopecks)

        // Вносимо точну суму 45.00 грн (4500 копійок)
        viewModel.onEvent(PosEvent.SetReceivedCash(4500L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4500L, viewModel.state.value.receivedCashKopecks)
        assertEquals(0L, viewModel.state.value.changeKopecks)
        assertFalse(viewModel.state.value.isCashShort)
        assertTrue(viewModel.state.value.canPay)
    }

    @Test
    fun testOverpaymentCalculatesCorrectChangeAndAllowsPayment() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        testDispatcher.scheduler.advanceUntilIdle()

        // Клієнт дає купюру 100 грн (10000 копійок) при чеку 45 грн
        viewModel.onEvent(PosEvent.SetReceivedCash(10000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(10000L, viewModel.state.value.receivedCashKopecks)
        assertEquals(5500L, viewModel.state.value.changeKopecks)
        assertFalse(viewModel.state.value.isCashShort)
        assertTrue(viewModel.state.value.canPay)
    }

    @Test
    fun testUnderpaymentMarksCashShortAndBlocksPayment() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        testDispatcher.scheduler.advanceUntilIdle()

        // Клієнт дав лише 20 грн (2000 копійок) при чеку 45 грн
        viewModel.onEvent(PosEvent.SetReceivedCash(2000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2000L, viewModel.state.value.receivedCashKopecks)
        assertEquals(0L, viewModel.state.value.changeKopecks)
        assertTrue(viewModel.state.value.isCashShort)
        assertFalse(viewModel.state.value.canPay)
    }

    @Test
    fun testClearReceiptResetsReceivedCash() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        viewModel.onEvent(PosEvent.SetReceivedCash(10000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(10000L, viewModel.state.value.receivedCashKopecks)

        viewModel.onEvent(PosEvent.ClearReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.receivedCashKopecks)
        assertEquals(0L, viewModel.state.value.changeKopecks)
        assertFalse(viewModel.state.value.isCashShort)
    }

    @Test
    fun testDismissReceivedCashWithNullEvent() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        viewModel.onEvent(PosEvent.SetReceivedCash(5000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5000L, viewModel.state.value.receivedCashKopecks)

        viewModel.onEvent(PosEvent.SetReceivedCash(null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.receivedCashKopecks)
        assertEquals(0L, viewModel.state.value.changeKopecks)
        assertFalse(viewModel.state.value.isCashShort)
    }

    @Test
    fun testFiscalizationResetsReceivedCashAfterPrint() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CASH))
        viewModel.onEvent(PosEvent.SetReceivedCash(10000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.canPay)

        viewModel.onEvent(PosEvent.FiscalizeReceipt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.items.isEmpty())
        assertNull(viewModel.state.value.receivedCashKopecks)
    }

    @Test
    fun testCardPaymentIgnoresCashShortageRule() = runTest {
        viewModel.onEvent(PosEvent.AddProduct(testProduct))
        viewModel.onEvent(PosEvent.SetPaymentType(PaymentType.CARD))
        testDispatcher.scheduler.advanceUntilIdle()

        // При безготівковій оплаті карткою прапорець canPay активний незалежно від receivedCashKopecks
        assertTrue(viewModel.state.value.canPay)
        assertFalse(viewModel.state.value.isCashShort)
    }
}
