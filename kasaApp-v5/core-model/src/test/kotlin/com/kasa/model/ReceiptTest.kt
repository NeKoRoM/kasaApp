package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReceiptTest {

    @Test
    fun testReceiptTotalsAndCashRounding() {
        val item1 = ReceiptItem(
            productId = 1L,
            productName = "Хліб Бородинський",
            priceKopecks = 2450L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )
        val item2 = ReceiptItem(
            productId = 2L,
            productName = "Яблука Голден",
            priceKopecks = 3200L,
            quantity = 0.504, // Ваговий товар: 3200 * 0.504 = 1612.8 -> 1613 коп
            taxGroup = TaxGroup.GROUP_A
        )

        assertEquals(1613L, item2.totalKopecks)

        // Чек з готівковою оплатою:
        // rawTotal = 2450 + 1613 = 4063 коп (40.63 грн)
        // cashRounding (3 коп -> донизу) -> 4060 коп (40.60 грн)
        // cashRoundingDifference = -3 коп
        val cashReceipt = Receipt(
            id = "test-uuid-1",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(item1, item2),
            paymentType = PaymentType.CASH
        )

        assertEquals(4063L, cashReceipt.rawTotalKopecks)
        assertEquals(4060L, cashReceipt.totalPayableKopecks)
        assertEquals(-3L, cashReceipt.cashRoundingDifferenceKopecks)
        assertTrue(cashReceipt.canBeFiscalized)

        // Чек з безготівковою оплатою: без округлення
        val cardReceipt = cashReceipt.copy(paymentType = PaymentType.CARD)
        assertEquals(4063L, cardReceipt.totalPayableKopecks)
        assertEquals(0L, cardReceipt.cashRoundingDifferenceKopecks)
    }

    @Test
    fun testExciseValidationInReceipt() {
        val alcoholItemWithoutStamp = ReceiptItem(
            productId = 10L,
            productName = "Коньяк 0.5л",
            priceKopecks = 25000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A_EXCISE,
            exciseStamp = null,
            uktzed = "2208200000"
        )

        val receipt = Receipt(
            id = "test-uuid-2",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(alcoholItemWithoutStamp),
            paymentType = PaymentType.CASH
        )

        // Без акцизної марки чек не може бути фіскалізований
        assertTrue(alcoholItemWithoutStamp.requiresExciseStamp)
        assertTrue(receipt.hasUnstampedExciseItems)
        assertFalse(receipt.canBeFiscalized)

        // Додаємо марку акцизного податку
        val alcoholItemWithStamp = alcoholItemWithoutStamp.copy(exciseStamp = "AB12345678")
        val stampedReceipt = receipt.copy(items = listOf(alcoholItemWithStamp))

        assertFalse(stampedReceipt.hasUnstampedExciseItems)
        assertTrue(stampedReceipt.canBeFiscalized)
        // 250.00 грн -> 5% акциз = 25000 * 5 / 105 = 1190 коп
        assertEquals(1190L, stampedReceipt.totalExciseKopecks)
    }
}
