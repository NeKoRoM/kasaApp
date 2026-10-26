package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertTrue

class PriceTagRendererTest {

    @Test
    fun testRenderPriceTagContainsEssentialRetailFields() {
        val product = Product(
            id = 101L,
            name = "Хліб Бородинський нарізний",
            price = 24.50,
            barcodes = listOf("4820000001015"),
            categoryName = "Випічка",
            taxGroup = TaxGroup.GROUP_A,
            unitOfMeasurement = "шт"
        )

        val tag = PriceTagRenderer.renderPriceTag(product, storeName = "ЕКО МАРКЕТ", printDate = "25.09.2026")
        assertTrue(tag.contains("ЦІННИК ТОВАРУ"))
        assertTrue(tag.contains("ЕКО МАРКЕТ"))
        assertTrue(tag.contains("Хліб Бородинський"))
        assertTrue(tag.contains("24.50 ГРН"))
        assertTrue(tag.contains("PLU: #101"))
        assertTrue(tag.contains("4820000001015"))
        assertTrue(tag.contains("Дата: 25.09.2026"))
    }
}
