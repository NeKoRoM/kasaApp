package com.kasa.protocol.frame

/**
 * Розрахунок та верифікація контрольної суми кадру Datecs/Exellio.
 *
 * Алгоритм контрольної суми (BCC):
 * 1. Підсумовуються всі беззнакові байти від початку поля Len до розділювача ENQ (0x05) включно: sum = sum & 0xFFFF.
 * 2. 16-бітне значення переводиться у 4 напівбайти (nibbles: 0..15).
 * 3. Кожен напівбайт кодується символом із кодом (nibble + 0x30) за схемою Datecs Nibble Encoding:
 *    - 0..9   -> '0'..'9' (0x30..0x39)
 *    - 10..15 -> ':'..'?' (0x3A..0x3F)
 */
object DatecsBccCalculator {

    /**
     * Обчислює 4-байтову контрольну суму (BCC) для послідовності байтів від Len до ENQ включно.
     */
    fun calculate(bytesFromLenToEnq: ByteArray): ByteArray {
        var sum = 0
        for (b in bytesFromLenToEnq) {
            sum += (b.toInt() and 0xFF)
        }
        val bcc = sum and 0xFFFF
        val bccBytes = ByteArray(4)
        bccBytes[0] = (((bcc ushr 12) and 0x0F) + 0x30).toByte()
        bccBytes[1] = (((bcc ushr 8) and 0x0F) + 0x30).toByte()
        bccBytes[2] = (((bcc ushr 4) and 0x0F) + 0x30).toByte()
        bccBytes[3] = ((bcc and 0x0F) + 0x30).toByte()
        return bccBytes
    }

    /**
     * Перевіряє валідність контрольної суми отриманого кадру.
     */
    fun verify(bytesFromLenToEnq: ByteArray, expectedBccBytes: ByteArray): Boolean {
        if (expectedBccBytes.size != 4) return false
        val calculated = calculate(bytesFromLenToEnq)
        return calculated.contentEquals(expectedBccBytes)
    }
}
