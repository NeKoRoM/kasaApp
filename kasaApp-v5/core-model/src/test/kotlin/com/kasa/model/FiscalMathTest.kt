package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FiscalMathTest {

    @Test
    fun testExciseCalculation() {
        // 105.00 грн -> 5% акциз = 5.00 грн (500 коп)
        assertEquals(500L, FiscalMath.calculateExciseKopecks(10500L))

        // 210.00 грн -> 5% акциз = 10.00 грн (1000 коп)
        assertEquals(1000L, FiscalMath.calculateExciseKopecks(21000L))

        // 0.00 грн
        assertEquals(0L, FiscalMath.calculateExciseKopecks(0L))
    }

    @Test
    fun testVat20WithoutExciseCalculation() {
        // 120.00 грн -> ПДВ 20% = 20.00 грн (2000 коп)
        assertEquals(2000L, FiscalMath.calculateVat20Kopecks(12000L, hasExcise = false))

        // 60.00 грн -> ПДВ 20% = 10.00 грн (1000 коп)
        assertEquals(1000L, FiscalMath.calculateVat20Kopecks(6000L, hasExcise = false))
    }

    @Test
    fun testVat20WithExciseCalculation() {
        // 105.00 грн підакцизного товару:
        // Акциз = 5.00 грн (500 коп)
        // База ПДВ = 105.00 - 5.00 = 100.00 грн (10000 коп)
        // ПДВ = 10000 * 20 / 120 = 1667 коп (16.67 грн)
        val vat = FiscalMath.calculateVat20Kopecks(10500L, hasExcise = true)
        assertEquals(1667L, vat)
    }

    @Test
    fun testVat7MedicalCalculation() {
        // 107.00 грн медикаментів -> ПДВ 7% = 7.00 грн (700 коп)
        assertEquals(700L, FiscalMath.calculateVat7Kopecks(10700L))
    }

    @Test
    fun testNbuCashRounding() {
        // Закінчення 0 залишається без змін
        assertEquals(1000L, FiscalMath.roundCashPayment(1000L))
        assertEquals(0L, FiscalMath.roundCashPayment(0L))

        // 1..4 копійки округлюються донизу (до 0)
        assertEquals(1000L, FiscalMath.roundCashPayment(1001L))
        assertEquals(1000L, FiscalMath.roundCashPayment(1002L))
        assertEquals(1000L, FiscalMath.roundCashPayment(1003L))
        assertEquals(1000L, FiscalMath.roundCashPayment(1004L))

        // 5..9 копійок округлюються догори (до 10)
        assertEquals(1010L, FiscalMath.roundCashPayment(1005L))
        assertEquals(1010L, FiscalMath.roundCashPayment(1006L))
        assertEquals(1010L, FiscalMath.roundCashPayment(1007L))
        assertEquals(1010L, FiscalMath.roundCashPayment(1008L))
        assertEquals(1010L, FiscalMath.roundCashPayment(1009L))
    }

    @Test
    fun testNegativeCashRounding() {
        // Повернення коштів (від'ємні суми)
        assertEquals(-1000L, FiscalMath.roundCashPayment(-1002L))
        assertEquals(-1010L, FiscalMath.roundCashPayment(-1007L))
    }
}
