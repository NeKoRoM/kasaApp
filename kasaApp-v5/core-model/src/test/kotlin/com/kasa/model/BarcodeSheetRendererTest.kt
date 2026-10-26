package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Тести для перевірки формування стрічки штрих-кодів для термодруку на РРО / чекових принтерах.
 */
class BarcodeSheetRendererTest {

    @Test
    fun testRenderBarcodeSheetEmptyList() {
        val sheet = BarcodeSheetRenderer.renderBarcodeSheet(
            title = "АРКУШ ШТРИХ-КОДІВ",
            products = emptyList(),
            categoryName = "Свіжа випічка"
        )

        assertTrue(sheet.contains("АРКУШ ШТРИХ-КОДІВ"))
        assertTrue(sheet.contains("Категорія: Свіжа випічка"))
        assertTrue(sheet.contains("(Немає товарів для друку)"))
    }

    @Test
    fun testRenderBarcodeSheetWithMultipleProducts() {
        val p1 = Product(
            id = 101L,
            name = "Батон нарізний",
            price = 22.50,
            barcodes = listOf("4820000001015"),
            unitOfMeasurement = "шт"
        )
        val p2 = Product(
            id = 102L,
            name = "Круасан з шоколадом",
            price = 35.00,
            barcodes = listOf("4820000001022"),
            unitOfMeasurement = "шт"
        )

        val sheet = BarcodeSheetRenderer.renderBarcodeSheet(
            products = listOf(p1, p2),
            storeName = "СВІЖИЙ ХЛІБ"
        )

        assertTrue(sheet.contains("СВІЖИЙ ХЛІБ"))
        assertTrue(sheet.contains("АРКУШ ШТРИХ-КОДІВ"))
        assertTrue(sheet.contains("[#101] Батон нарізний"))
        assertTrue(sheet.contains("4820000001015"))
        assertTrue(sheet.contains("22.50 грн"))
        assertTrue(sheet.contains("[#102] Круасан з шоколадом"))
        assertTrue(sheet.contains("4820000001022"))
        assertTrue(sheet.contains("35.00 грн"))
        assertTrue(sheet.contains("Позицій: 2"))
    }

    @Test
    fun testRenderBarcodeSheetGeneratesFallbackBarcodeWhenNoneProvided() {
        val pNoBarcode = Product(
            id = 105L,
            name = "Булочка ванільна",
            price = 15.00,
            barcodes = emptyList(),
            unitOfMeasurement = "шт"
        )

        val sheet = BarcodeSheetRenderer.renderBarcodeSheet(
            products = listOf(pNoBarcode)
        )

        assertTrue(sheet.contains("[#105] Булочка ванільна"))
        // Має згенерувати внутрішній EAN-13 з префіксом 25
        assertTrue(sheet.contains("250010500000"))
        assertTrue(sheet.contains("15.00 грн"))
        assertTrue(sheet.contains("Позицій: 1"))
    }

    @Test
    fun testRenderBarcodeSheetCustomWidth() {
        val p = Product(id = 101L, name = "Кава Амерікано", price = 40.00, barcodes = listOf("4821111111111"))
        val sheet48 = BarcodeSheetRenderer.renderBarcodeSheet(products = listOf(p), lineWidth = 48)

        assertTrue(sheet48.contains("=".repeat(48)))
        assertTrue(sheet48.contains("Кава Амерікано"))
    }
}
