package com.kasa.protocol.status

/**
 * Декодер статусних байтів фіскальних реєстраторів лінійки Exellio / Datecs.
 * Підтримує як 8-байтовий протокол Datecs X (Exellio DP-25 / DP-05 / WP-500X після маркера 0x04),
 * так і 6-байтовий класичний формат Datecs / симулятора.
 */
object KasaStatusDecoder {

    /**
     * Вилучає блок статусних байтів із сирого корисного навантаження кадру відповіді (після 0x04 EOT)
     * або з останніх 6 байтів (для симулятора) та декодує їх у [KasaStatus].
     */
    fun extractFromPayload(rawPayloadBytes: ByteArray): KasaStatus {
        if (rawPayloadBytes.size < 6) return KasaStatus()
        for (i in rawPayloadBytes.size - 6 downTo 0) {
            if (rawPayloadBytes[i] == 0x04.toByte()) {
                val statusLen = rawPayloadBytes.size - (i + 1)
                if (statusLen in 6..10) {
                    return decode(rawPayloadBytes.copyOfRange(i + 1, rawPayloadBytes.size))
                }
            }
        }
        val fallback = rawPayloadBytes.copyOfRange(rawPayloadBytes.size - 6, rawPayloadBytes.size)
        return decode(fallback)
    }

    /**
     * Декодує масив із 6 або 8 байтів стану каси у типізований об'єкт [KasaStatus].
     */
    fun decode(statusBytes: ByteArray): KasaStatus {
        if (statusBytes.size < 6) {
            return KasaStatus()
        }

        val b0 = statusBytes[0].toInt() and 0xFF
        val b1 = statusBytes[1].toInt() and 0xFF
        val b2 = statusBytes[2].toInt() and 0xFF
        val b4 = statusBytes[4].toInt() and 0xFF

        // Протокол Datecs X / Exellio DP-25 (8 байтів S0..S7 зі старшим бітом 0x80)
        if (statusBytes.size >= 8 || (b0 and 0x80) != 0) {
            val b5 = statusBytes[5].toInt() and 0xFF
            return KasaStatus(
                hasSyntaxError = (b0 and 0x01) != 0,
                isInvalidCommand = (b0 and 0x02) != 0,
                isRtcError = (b0 and 0x04) != 0,
                isHeadOverheated = (b0 and 0x10) != 0,
                isCoverOpen = (b0 and 0x40) != 0,
                isCommandNotAllowed = (b1 and 0x02) != 0,
                isReceiptPaperOut = (b2 and 0x01) != 0,
                isControlPaperOut = (b2 and 0x04) != 0,
                isFiscalReceiptOpen = (b2 and 0x08) != 0,
                isNonFiscalReceiptOpen = (b2 and 0x20) != 0,
                isFiscalMemoryFull = (b4 and 0x10) != 0,
                isFiscalized = (b5 and 0x08) != 0
            )
        }

        // Класичний 6-байтовий формат Datecs / SimulatedKasaTransport (базовий біт 0x40)
        return KasaStatus(
            hasSyntaxError = (b0 and 0x01) != 0,
            isInvalidCommand = (b0 and 0x02) != 0,
            isRtcError = (b0 and 0x04) != 0,
            isCommandNotAllowed = (b0 and 0x10) != 0,
            hasBccError = (b0 and 0x20) != 0,
            isControlPaperOut = (b1 and 0x01) != 0,
            isReceiptPaperOut = (b1 and 0x02) != 0,
            isCoverOpen = (b1 and 0x04) != 0,
            isHeadOverheated = (b1 and 0x10) != 0,
            isFiscalized = (b2 and 0x01) != 0,
            isFiscalMemoryFull = (b2 and 0x02) != 0,
            is24HoursShiftExpired = (b2 and 0x08) != 0,
            isNonFiscalReceiptOpen = (b4 and 0x01) != 0,
            isFiscalReceiptOpen = (b4 and 0x02) != 0,
            isCustomerDisplayConnected = (b4 and 0x04) != 0
        )
    }
}
