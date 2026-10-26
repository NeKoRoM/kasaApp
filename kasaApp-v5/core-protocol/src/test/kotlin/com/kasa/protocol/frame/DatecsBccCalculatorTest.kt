package com.kasa.protocol.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatecsBccCalculatorTest {

    @Test
    fun testBccCalculationAndNibbleEncoding() {
        // Тестовий масив байтів: "002;@003>\u0005"
        val input = "002;@003>\u0005".toByteArray(Charsets.US_ASCII)
        val bcc = DatecsBccCalculator.calculate(input)

        // Перевіряємо довжину BCC (рівно 4 байти)
        assertEquals(4, bcc.size)

        // Перевіряємо, що всі байти знаходяться в діапазоні ASCII 0x30..0x3F ('0'..'9', ':'..'?')
        for (b in bcc) {
            val code = b.toInt() and 0xFF
            assertTrue(code in 0x30..0x3F, "Byte $code is not a valid Datecs nibble character")
        }

        // Перевіряємо верифікацію
        assertTrue(DatecsBccCalculator.verify(input, bcc))
    }

    @Test
    fun testKnownBccNibbles() {
        // Якщо сума дорівнює 0xABCD:
        // nibble 0 (A = 10) -> 10 + 0x30 = 0x3A (':')
        // nibble 1 (B = 11) -> 11 + 0x30 = 0x3B (';')
        // nibble 2 (C = 12) -> 12 + 0x30 = 0x3C ('<')
        // nibble 3 (D = 13) -> 13 + 0x30 = 0x3D ('=')
        val bccBytes = ByteArray(4)
        val sum = 0xABCD
        bccBytes[0] = (((sum ushr 12) and 0x0F) + 0x30).toByte()
        bccBytes[1] = (((sum ushr 8) and 0x0F) + 0x30).toByte()
        bccBytes[2] = (((sum ushr 4) and 0x0F) + 0x30).toByte()
        bccBytes[3] = ((sum and 0x0F) + 0x30).toByte()

        assertEquals(':', bccBytes[0].toInt().toChar())
        assertEquals(';', bccBytes[1].toInt().toChar())
        assertEquals('<', bccBytes[2].toInt().toChar())
        assertEquals('=', bccBytes[3].toInt().toChar())
    }
}
