package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тести канонічної моделі касової зміни CashierShift.
 * Перевіряє розрахунок тривалості, правила 24 годин та очікуваного балансу готівки.
 */
class CashierShiftTest {

    @Test
    fun testOpenShiftCalculatesDurationAndExpiry() {
        val now = System.currentTimeMillis()
        val openShift = CashierShift(
            id = "shift-01",
            shiftNumber = 1L,
            cashierId = 1L,
            cashierName = "Марія Шевченко",
            openedAt = now - (2 * 60 * 60 * 1000L), // 2 години тому
            initialDrawerBalanceKopecks = 100000L // 1000.00 грн
        )

        assertTrue(openShift.isOpen)
        assertFalse(openShift.isClosed)
        assertFalse(openShift.isExpired24h)
        assertEquals("2г 0хв", openShift.formattedDuration)
    }

    @Test
    fun testShiftExpiredAfter24Hours() {
        val now = System.currentTimeMillis()
        val expiredShift = CashierShift(
            id = "shift-02",
            shiftNumber = 2L,
            cashierId = 1L,
            cashierName = "Марія Шевченко",
            openedAt = now - (25 * 60 * 60 * 1000L), // 25 годин тому
            initialDrawerBalanceKopecks = 50000L
        )

        assertTrue(expiredShift.isOpen)
        assertTrue(expiredShift.isExpired24h)
    }

    @Test
    fun testClosedShiftIsNotExpiredEvenIfOver24Hours() {
        val opened = 1700000000000L
        val closed = opened + (26 * 60 * 60 * 1000L) // 26 годин тривала
        val closedShift = CashierShift(
            id = "shift-03",
            shiftNumber = 3L,
            cashierId = 1L,
            cashierName = "Олег Бондар",
            openedAt = opened,
            closedAt = closed,
            isClosed = true,
            zReportNumber = "00014"
        )

        assertFalse(closedShift.isOpen)
        assertTrue(closedShift.isClosed)
        assertFalse(closedShift.isExpired24h) // Закрита зміна більше не блокує систему
        assertEquals("26г 0хв", closedShift.formattedDuration)
    }

    @Test
    fun testExpectedDrawerCashCalculation() {
        val shift = CashierShift(
            id = "shift-04",
            shiftNumber = 4L,
            cashierId = 1L,
            cashierName = "Олена Коваль",
            openedAt = System.currentTimeMillis(),
            initialDrawerBalanceKopecks = 100000L, // 1000 грн розмінна монета
            totalSalesCashKopecks = 50000L,       // +500 грн продажів готівкою
            totalSalesCardKopecks = 30000L,       // +300 грн карткою (НЕ йде в шухляду)
            totalReturnsCashKopecks = 10000L,     // -100 грн повернення готівкою
            totalReturnsCardKopecks = 5000L,      // -50 грн повернення карткою
            totalCashInKopecks = 20000L,          // +200 грн службове внесення
            totalCashOutKopecks = 40000L          // -400 грн службова видача (інкасація)
        )

        // 100000 + 50000 - 10000 + 20000 - 40000 = 120000 коп (1200 грн)
        assertEquals(120000L, shift.expectedDrawerCashKopecks)
    }
}
