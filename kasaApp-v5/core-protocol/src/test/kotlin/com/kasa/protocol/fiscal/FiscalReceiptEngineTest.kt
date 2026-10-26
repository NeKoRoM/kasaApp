package com.kasa.protocol.fiscal

import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import com.kasa.model.ReceiptItem
import com.kasa.model.TaxGroup
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FiscalReceiptEngineTest {

    private lateinit var transport: SimulatedKasaTransport
    private lateinit var engine: FiscalReceiptEngine

    @BeforeTest
    fun setUp() = runTest {
        transport = SimulatedKasaTransport()
        transport.connect("SIMULATED_DATECS_01")
        engine = FiscalReceiptEngine(transport)
    }

    @Test
    fun testSuccessfulFiscalReceiptExecution() = runTest {
        val item1 = ReceiptItem(
            productId = 101L,
            productName = "Хліб Житній",
            priceKopecks = 2200L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )
        val item2 = ReceiptItem(
            productId = 102L,
            productName = "Вино Сухе 0.75л",
            priceKopecks = 15000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A_EXCISE,
            exciseStamp = "EX12345678",
            uktzed = "2204100000"
        )

        val receipt = Receipt(
            id = "test-receipt-001",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = listOf(item1, item2),
            paymentType = PaymentType.CASH
        )

        val result = engine.executeReceipt(receipt)

        assertTrue(result is FiscalReceiptResult.Success)
        assertEquals("test-receipt-001", result.receiptId)
        assertNotNull(result.fiscalCheckNumber)
        assertFalse(transport.isReceiptOpen, "Receipt must be closed at the end of execution")
    }

    @Test
    fun testUnstampedExciseItemFailsValidation() = runTest {
        val unstampedAlcohol = ReceiptItem(
            productId = 103L,
            productName = "Коньяк 0.5л",
            priceKopecks = 28000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A_EXCISE,
            exciseStamp = null,
            uktzed = "2208200000"
        )

        val invalidReceipt = Receipt(
            id = "test-receipt-002",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = listOf(unstampedAlcohol),
            paymentType = PaymentType.CASH
        )

        val result = engine.executeReceipt(invalidReceipt)

        assertTrue(result is FiscalReceiptResult.Failure)
        assertEquals(FiscalError.INVALID_RECEIPT, result.error)
        assertEquals(FiscalStep.VALIDATION, result.step)
        assertFalse(transport.isReceiptOpen)
    }

    @Test
    fun testPaperOutFailsHardwareCheck() = runTest {
        transport.simulatePaperOut = true

        val item = ReceiptItem(1L, "Товар", 1000L, 1.0, TaxGroup.GROUP_A)
        val receipt = Receipt("test-receipt-003", System.currentTimeMillis(), 1L, listOf(item))

        val result = engine.executeReceipt(receipt)

        assertTrue(result is FiscalReceiptResult.Failure)
        assertEquals(FiscalError.HARDWARE_ERROR, result.error)
        assertEquals(FiscalStep.HARDWARE_CHECK, result.step)
    }

    @Test
    fun test24HoursShiftExpiredBlocksReceipt() = runTest {
        transport.simulateShiftExpired = true

        val item = ReceiptItem(1L, "Товар", 1000L, 1.0, TaxGroup.GROUP_A)
        val receipt = Receipt("test-receipt-004", System.currentTimeMillis(), 1L, listOf(item))

        val result = engine.executeReceipt(receipt)

        assertTrue(result is FiscalReceiptResult.Failure)
        assertEquals(FiscalError.SHIFT_EXPIRED_24H, result.error)
        assertEquals(FiscalStep.HARDWARE_CHECK, result.step)
    }

    @Test
    fun testDisconnectedTransportFailsHardwareCheck() = runTest {
        transport.disconnect()

        val item = ReceiptItem(1L, "Товар", 1000L, 1.0, TaxGroup.GROUP_A)
        val receipt = Receipt("test-receipt-005", System.currentTimeMillis(), 1L, listOf(item))

        val result = engine.executeReceipt(receipt)

        assertTrue(result is FiscalReceiptResult.Failure)
        assertEquals(FiscalError.TRANSPORT_DISCONNECTED, result.error)
    }

    @Test
    fun testCombinedSplitPaymentExecution() = runTest {
        val item = ReceiptItem(
            productId = 104L,
            productName = "Смартфон",
            priceKopecks = 100000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )
        val receipt = Receipt(
            id = "test-receipt-split",
            createdAt = System.currentTimeMillis(),
            cashierId = 1L,
            items = listOf(item),
            paymentType = PaymentType.COMBINED,
            splitCardKopecks = 60000L,
            splitCashKopecks = 40000L
        )

        val result = engine.executeReceipt(receipt)

        assertTrue(result is FiscalReceiptResult.Success)
        assertFalse(transport.isReceiptOpen)
    }
}
