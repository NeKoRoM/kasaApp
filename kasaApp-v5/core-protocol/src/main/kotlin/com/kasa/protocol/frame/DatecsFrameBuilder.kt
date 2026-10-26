package com.kasa.protocol.frame

import com.kasa.protocol.command.DatecsCommand
import java.nio.charset.Charset

/**
 * Побудова та розбір бінарних кадрів фіскального протоколу Datecs / Exellio.
 *
 * ФОРМАТ КАДРУ:
 * <0x01 (SOH)> <LEN: 4 байти> <SEQ: 1 байт> <CMD_DATA> <0x05 (ENQ)> <BCC: 4 байти> <0x03 (ETX)>
 */
class DatecsFrameBuilder(
    private val charset: Charset = Charset.forName("windows-1251")
) {

    /**
     * Генерує наступний порядковий номер кадру в діапазоні 0x40 ('@') .. 0x7B ('{').
     */
    @Synchronized
    fun nextSequence(): Byte {
        val current = sharedSequenceId
        sharedSequenceId = if (sharedSequenceId >= 0x7B) 0x40 else sharedSequenceId + 1
        return current.toByte()
    }

    /**
     * Скидає лічильник послідовності на початкове значення 0x40.
     */
    @Synchronized
    fun resetSequence() {
        sharedSequenceId = 0x40
    }

    companion object {
        @Volatile
        private var sharedSequenceId: Int = 0x40

        /**
         * Чорний список небезпечних сервісних/заводських команд протоколу Datecs/Exellio,
         * які категорично заборонено формувати та відправляти на РРО:
         * - "0058" (88 / 0x58): обнулення ОЗП (RAM Reset) та запуск верифікації КСЕФ на Exellio DP-25 (FW 684008)
         * - "0059" (89 / 0x59): сервісний тест запису фіскальної пам'яті
         * - "005;" (91 / 0x5B): заводський запис ЗН/ФН у фіскальну пам'ять
         * - "0062" (98 / 0x62): програмування податкового номера у ФП
         * - "00?=" (253 / 0xFD): заводські сервісні операції
         * - "006;D\tA": повне стирання всієї бази товарів PLU
         */
        val BLACKLISTED_COMMAND_PREFIXES: Set<String> = setOf(
            "0058",
            "0059",
            "005;",
            "0062",
            "00?=",
            "006;D\tA"
        )

        fun isCommandBlacklisted(payloadText: String): Boolean {
            val trimmed = payloadText.trimStart()
            return BLACKLISTED_COMMAND_PREFIXES.any { prefix -> trimmed.startsWith(prefix) }
        }
    }

    /**
     * Створює готовий бінарний кадр з типізованої команди [DatecsCommand].
     */
    fun buildFrame(command: DatecsCommand, sequence: Byte = nextSequence()): ByteArray {
        val fullPayload = command.rawCommandCode + command.buildPayload()
        return buildFrame(fullPayload, sequence)
    }

    /**
     * Створює готовий бінарний кадр із текстового корисного навантаження [payloadText].
     */
    fun buildFrame(payloadText: String, sequence: Byte = nextSequence()): ByteArray {
        require(!isCommandBlacklisted(payloadText)) {
            "БЛОКУВАННЯ БЕЗПЕКИ: Команда '$payloadText' знаходиться у чорному списку сервісних команд РРО!"
        }
        val payloadBytes = payloadText.toByteArray(charset)

        // Довжина включає зміщення протоколу Datecs (+38 = +5 +30 +3)
        val rawLen = payloadBytes.size + 38
        val lenBytes = ByteArray(4)
        lenBytes[0] = (((rawLen ushr 12) and 0x0F) + 0x30).toByte()
        lenBytes[1] = (((rawLen ushr 8) and 0x0F) + 0x30).toByte()
        lenBytes[2] = (((rawLen ushr 4) and 0x0F) + 0x30).toByte()
        lenBytes[3] = ((rawLen and 0x0F) + 0x30).toByte()

        val enq = 0x05.toByte()

        // Масив байтів для розрахунку контрольної суми: Len (4) + Seq (1) + Payload (N) + ENQ (1)
        val bytesForBcc = ByteArray(4 + 1 + payloadBytes.size + 1)
        System.arraycopy(lenBytes, 0, bytesForBcc, 0, 4)
        bytesForBcc[4] = sequence
        System.arraycopy(payloadBytes, 0, bytesForBcc, 5, payloadBytes.size)
        bytesForBcc[bytesForBcc.size - 1] = enq

        val bccBytes = DatecsBccCalculator.calculate(bytesForBcc)

        // Повний кадр: SOH (0x01) + bytesForBcc + BCC (4) + ETX (0x03)
        val fullFrame = ByteArray(1 + bytesForBcc.size + 4 + 1)
        fullFrame[0] = 0x01.toByte()
        System.arraycopy(bytesForBcc, 0, fullFrame, 1, bytesForBcc.size)
        System.arraycopy(bccBytes, 0, fullFrame, 1 + bytesForBcc.size, 4)
        fullFrame[fullFrame.size - 1] = 0x03.toByte()

        return fullFrame
    }

    /**
     * Результат декодування вхідного кадру відповіді від каси.
     */
    data class DecodedFrame(
        val sequence: Byte,
        val payloadText: String,
        val rawPayloadBytes: ByteArray,
        val isBccValid: Boolean
    )

    /**
     * Розбирає повний кадр відповіді, перевіряє BCC та вилучає корисне навантаження.
     */
    fun decodeFrame(frameBytes: ByteArray): DecodedFrame? {
        // Мінімальний кадр: SOH (1) + LEN (4) + SEQ (1) + ENQ (1) + BCC (4) + ETX (1) = 12 байтів
        if (frameBytes.size < 12) return null
        if (frameBytes.first() != 0x01.toByte() || frameBytes.last() != 0x03.toByte()) return null

        val bytesFromLenToEnq = ByteArray(frameBytes.size - 6) // Мінус SOH (1), BCC (4), ETX (1)
        System.arraycopy(frameBytes, 1, bytesFromLenToEnq, 0, bytesFromLenToEnq.size)

        val expectedBcc = ByteArray(4)
        System.arraycopy(frameBytes, frameBytes.size - 5, expectedBcc, 0, 4)

        val isBccValid = DatecsBccCalculator.verify(bytesFromLenToEnq, expectedBcc)
        val sequence = bytesFromLenToEnq[4]

        // Корисне навантаження знаходиться між SEQ (індекс 4) та ENQ/EOT (останній байт у bytesFromLenToEnq)
        val payloadLength = bytesFromLenToEnq.size - 5 - 1
        val payloadBytes = ByteArray(maxOf(0, payloadLength))
        if (payloadLength > 0) {
            System.arraycopy(bytesFromLenToEnq, 5, payloadBytes, 0, payloadLength)
        }

        val payloadText = String(payloadBytes, charset)
        return DecodedFrame(
            sequence = sequence,
            payloadText = payloadText,
            rawPayloadBytes = payloadBytes,
            isBccValid = isBccValid
        )
    }

    /**
     * Декодує повний кадр або частковий (обірваний Bluetooth-модулем без 0x03 ETX після тиші END_TRANSACTION).
     * Дозволяє витягти код артикулу для повторного запиту 006;F або повний запис, якщо обірвано лише хвіст BCC/ETX.
     */
    fun decodeFrameOrPartial(frameBytes: ByteArray): DecodedFrame? {
        if (frameBytes.size < 7 || frameBytes.first() != 0x01.toByte()) return null
        if (frameBytes.size >= 12 && frameBytes.last() == 0x03.toByte()) {
            return decodeFrame(frameBytes)
        }

        val sequence = frameBytes[5]
        var endIndex = frameBytes.size
        for (i in 6 until frameBytes.size) {
            if (frameBytes[i] == 0x05.toByte()) {
                endIndex = i
                break
            }
        }
        val payloadLength = endIndex - 6
        if (payloadLength <= 0) return null

        val payloadBytes = frameBytes.copyOfRange(6, 6 + payloadLength)
        val payloadText = String(payloadBytes, charset)
        return DecodedFrame(
            sequence = sequence,
            payloadText = payloadText,
            rawPayloadBytes = payloadBytes,
            isBccValid = false
        )
    }
}
