package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FiscalUrlGeneratorTest {

    private val sampleItem = ReceiptItem(
        productId = 1,
        productName = "Кава Арабіка",
        priceKopecks = 6500L,
        quantity = 1.0,
        taxGroup = TaxGroup.GROUP_A
    )

    @Test
    fun testGenerateTaxUrlReturnsNullWhenNoCheckNumber() {
        val nonFiscalReceipt = Receipt(
            id = "test-1",
            createdAt = 1774526400000L, // 2026-03-26
            cashierId = 1L,
            items = listOf(sampleItem),
            fiscalCheckNumber = null
        )

        val url = FiscalUrlGenerator.generateTaxVerificationUrl(nonFiscalReceipt)
        assertNull(url)
    }

    @Test
    fun testGenerateTaxUrlWithDefaultFiscalNumber() {
        // Фіксована мітка часу: 2026-09-25 15:30:45 UTC+3 (Kyiv)
        // 1790339445000L -> 2026-09-25 15:30:45 Kyiv
        val timestamp = 1790339445000L

        val receipt = Receipt(
            id = "test-2",
            createdAt = timestamp,
            cashierId = 1L,
            items = listOf(sampleItem),
            fiscalCheckNumber = "1042"
        )

        val url = FiscalUrlGenerator.generateTaxVerificationUrl(receipt)
        assertNotNull(url)
        assertTrue(url.startsWith("https://cabinet.tax.gov.ua/cashregs/check?"))
        assertTrue(url.contains("fn=3000123456"))
        assertTrue(url.contains("id=1042"))
        assertTrue(url.contains("sm=65.00"))
        assertTrue(url.contains("date=20260925"))
    }

    @Test
    fun testGenerateTaxUrlWithCustomFiscalRegisterNumber() {
        val receipt = Receipt(
            id = "test-3",
            createdAt = 1790339445000L,
            cashierId = 2L,
            items = listOf(sampleItem),
            fiscalCheckNumber = "5099",
            fiscalRegisterNumber = "4000987654"
        )

        val url = FiscalUrlGenerator.generateTaxVerificationUrl(receipt)
        assertNotNull(url)
        assertTrue(url.contains("fn=4000987654"))
        assertTrue(url.contains("id=5099"))
        assertTrue(url.contains("sm=65.00"))
    }
}
