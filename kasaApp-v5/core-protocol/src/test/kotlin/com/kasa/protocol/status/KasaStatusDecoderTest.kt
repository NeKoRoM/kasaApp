package com.kasa.protocol.status

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KasaStatusDecoderTest {

    @Test
    fun testNormalReadyStatus() {
        // Каса готова: фіскалізована, без помилок, папір є, кришка закрита
        val statusBytes = byteArrayOf(
            0x00, // Байт 0: помилок немає
            0x00, // Байт 1: принтер ОК
            0x01, // Байт 2: фіскалізовано
            0x00, // Байт 3
            0x00, // Байт 4: чеків не відкрито
            0x00  // Байт 5
        )

        val status = KasaStatusDecoder.decode(statusBytes)

        assertTrue(status.isFiscalized)
        assertFalse(status.hasSyntaxError)
        assertFalse(status.isReceiptPaperOut)
        assertFalse(status.isCoverOpen)
        assertFalse(status.is24HoursShiftExpired)
        assertTrue(status.canPrintReceipt)
    }

    @Test
    fun testPaperOutBlocksPrinting() {
        val statusBytes = byteArrayOf(
            0x00,
            0x02, // Bit 1: Закінчився чековий папір
            0x01,
            0x00,
            0x00,
            0x00
        )

        val status = KasaStatusDecoder.decode(statusBytes)

        assertTrue(status.isReceiptPaperOut)
        assertFalse(status.canPrintReceipt)
    }

    @Test
    fun test24HoursShiftExpiredBlocksPrinting() {
        val statusBytes = byteArrayOf(
            0x00,
            0x00,
            0x09, // Bit 0: фіскалізовано (0x01) + Bit 3: 24 години зміни минули (0x08)
            0x00,
            0x00,
            0x00
        )

        val status = KasaStatusDecoder.decode(statusBytes)

        assertTrue(status.isFiscalized)
        assertTrue(status.is24HoursShiftExpired)
        assertFalse(status.canPrintReceipt)
    }

    @Test
    fun testCoverOpenBlocksPrinting() {
        val statusBytes = byteArrayOf(
            0x00,
            0x04, // Bit 2: Відкрита кришка
            0x01,
            0x00,
            0x00,
            0x00
        )

        val status = KasaStatusDecoder.decode(statusBytes)

        assertTrue(status.isCoverOpen)
        assertFalse(status.canPrintReceipt)
    }
}
