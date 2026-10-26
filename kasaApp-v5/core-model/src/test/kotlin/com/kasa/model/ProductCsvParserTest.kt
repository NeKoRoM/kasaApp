package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductCsvParserTest {

    @Test
    fun testExportAndImportCycle() {
        val originalProducts = listOf(
            Product(
                id = 101L,
                name = "Хліб \"Бородинський\", нарізний",
                price = 24.50,
                costPrice = 18.00,
                barcodes = listOf("4820000001015", "4820000001016"),
                categoryName = "Хлібобулочні",
                taxGroup = TaxGroup.GROUP_A,
                unitOfMeasurement = "шт",
                remainder = 35.0,
                minRemainder = 10.0,
                isFavorite = true
            ),
            Product(
                id = 102L,
                name = "Яблука Голден",
                price = 38.00,
                costPrice = 25.50,
                barcodes = listOf("2000102000000"),
                categoryName = "Фрукти/Овочі",
                taxGroup = TaxGroup.GROUP_A,
                unitOfMeasurement = "кг",
                isWeighted = true,
                remainder = 12.5,
                minRemainder = 5.0,
                isFavorite = false
            )
        )

        val csv = ProductCsvParser.exportToCsv(originalProducts)
        assertTrue(csv.contains("PLU,Назва,Ціна"))
        assertTrue(csv.contains("\"Хліб \"\"Бородинський\"\", нарізний\""))

        val imported = ProductCsvParser.importFromCsv(csv)
        assertEquals(2, imported.size)

        val first = imported[0]
        assertEquals(101L, first.id)
        assertEquals("Хліб \"Бородинський\", нарізний", first.name)
        assertEquals(24.50, first.price)
        assertEquals(18.00, first.costPrice)
        assertEquals(listOf("4820000001015", "4820000001016"), first.barcodes)
        assertEquals("Хлібобулочні", first.categoryName)
        assertEquals("шт", first.unitOfMeasurement)
        assertEquals(35.0, first.remainder)
        assertEquals(10.0, first.minRemainder)
        assertTrue(first.isFavorite)

        val second = imported[1]
        assertEquals(102L, second.id)
        assertEquals("Яблука Голден", second.name)
        assertTrue(second.isWeighted)
        assertEquals("кг", second.unitOfMeasurement)
    }

    @Test
    fun testImportWithSemicolonDelimiter() {
        val csvSemicolon = """
            PLU;Назва;Ціна;Собівартість;Штрихкоди;Категорія;Ставка_ПДВ;Одиниця;Ваговий;УКТЗЕД;Залишок;Мін_Залишок;Обране
            201;Кава Еспресо;35.00;12.00;;Напої;GROUP_A;шт;0;;50.0;10.0;1
        """.trimIndent()

        val imported = ProductCsvParser.importFromCsv(csvSemicolon)
        assertEquals(1, imported.size)
        val coffee = imported[0]
        assertEquals(201L, coffee.id)
        assertEquals("Кава Еспресо", coffee.name)
        assertEquals(35.0, coffee.price)
        assertEquals(12.0, coffee.costPrice)
        assertEquals("Напої", coffee.categoryName)
        assertTrue(coffee.isFavorite)
    }
}
