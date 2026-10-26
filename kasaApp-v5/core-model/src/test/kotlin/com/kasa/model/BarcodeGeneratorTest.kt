package com.kasa.model

import kotlin.test.*

/**
 * Тести алгоритмів генерації та валідації штрих-кодів EAN-13 роздрібної торгівлі.
 */
class BarcodeGeneratorTest {

    @Test
    fun testGenerateInternalPieceEan13() {
        val barcode = BarcodeGenerator.generateInternalPieceEan13(101L)
        assertEquals(13, barcode.length)
        assertTrue(barcode.startsWith("250010100000"))
        assertTrue(BarcodeGenerator.isValidEan13(barcode))

        val parsed = BarcodeParser.parse(barcode)
        assertTrue(parsed is ParsedBarcode.Standard)
    }

    @Test
    fun testGenerateWeightedEan13_parsesBackCorrectly() {
        // Генеруємо ваговий штрих-код: PLU 55, вага 1.450 кг (1450 г)
        val barcode = BarcodeGenerator.generateWeightedEan13(prefix = 21, plu = 55L, weightGrams = 1450)
        assertEquals(13, barcode.length)
        assertTrue(BarcodeGenerator.isValidEan13(barcode))

        // Перевіряємо зворотний парсинг через BarcodeParser
        val parsed = BarcodeParser.parse(barcode)
        assertTrue(parsed is ParsedBarcode.Weighted)
        assertEquals(55L, parsed.plu)
        assertEquals(1.450, parsed.weightKilograms)
    }

    @Test
    fun testGenerateRetailEan13() {
        val barcode = BarcodeGenerator.generateRetailEan13(101L)
        assertEquals(13, barcode.length)
        assertTrue(barcode.startsWith("482000000101"))
        assertTrue(BarcodeGenerator.isValidEan13(barcode))
    }

    @Test
    fun testIsValidEan13_validation() {
        assertFalse(BarcodeGenerator.isValidEan13("123"))
        assertFalse(BarcodeGenerator.isValidEan13("4820000001019")) // невірна контрольна сума
        assertFalse(BarcodeGenerator.isValidEan13("482000000101A")) // не цифри
        assertTrue(BarcodeGenerator.isValidEan13("4820000001014"))  // вірний EAN-13
    }
}
