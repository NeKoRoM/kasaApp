package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeParserTest {

    @Test
    fun testEan13ChecksumCalculationAndValidation() {
        // Тестовий 12-значний префікс
        val twelve = "482000000001"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(twelve)
        val validEan13 = "$twelve$checkDigit"

        assertTrue(BarcodeParser.isValidEan13Checksum(validEan13))

        // Зіпсована контрольна цифра
        val invalidCheckDigit = (checkDigit + 1) % 10
        val invalidEan13 = "$twelve$invalidCheckDigit"
        assertFalse(BarcodeParser.isValidEan13Checksum(invalidEan13))

        // Невірна довжина
        assertFalse(BarcodeParser.isValidEan13Checksum("12345"))
        assertFalse(BarcodeParser.isValidEan13Checksum("12345678901234"))
    }

    @Test
    fun testParseWeightedBarcodeFromStoreScale() {
        // PLU = 42, Вага = 1.750 кг (1750 г)
        val prefix = "21"
        val pluPart = "00042"
        val weightPart = "01750"
        val first12 = "$prefix$pluPart$weightPart"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(first12)
        val barcode = "$first12$checkDigit"

        val parsed = BarcodeParser.parse(barcode)
        assertTrue(parsed is ParsedBarcode.Weighted)
        assertEquals(barcode, parsed.rawBarcode)
        assertEquals(42L, parsed.plu)
        assertEquals(1.750, parsed.weightKilograms)
    }

    @Test
    fun testParsePriceEmbeddedBarcode() {
        // PLU = 105, Вартість = 125.50 грн (12550 коп)
        val prefix = "28"
        val pluPart = "00105"
        val pricePart = "12550"
        val first12 = "$prefix$pluPart$pricePart"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(first12)
        val barcode = "$first12$checkDigit"

        val parsed = BarcodeParser.parse(barcode)
        assertTrue(parsed is ParsedBarcode.PriceEmbedded)
        assertEquals(barcode, parsed.rawBarcode)
        assertEquals(105L, parsed.plu)
        assertEquals(12550L, parsed.totalPriceKopecks)
    }

    @Test
    fun testParseStandardPieceProductBarcode() {
        val standardBarcode = "482000000001"
        val parsed = BarcodeParser.parse(standardBarcode)

        assertTrue(parsed is ParsedBarcode.Standard)
        assertEquals("482000000001", parsed.barcode)
    }
}
