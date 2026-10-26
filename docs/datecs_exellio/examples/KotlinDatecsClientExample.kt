package docs.datecs_exellio.examples

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Еталонна реалізація клієнта протоколу Datecs v2 / Exellio мовою Kotlin.
 *
 * Особливості:
 * 1. Формування кадру: <SOH (0x01)> + <LEN (4 нібли)> + <SEQ> + <CMD (4 нібли)> + <DATA> + <ENQ (0x05)> + <BCC (4 нібли)> + <ETX (0x03)>.
 * 2. Кодування ніблів (Datecs Nibble Encoding): nibble + 0x30.
 * 3. Підтримка очікування байтів SYN (0x16) кожні 60 мс для тривалих операцій (Z-звіт).
 * 4. Побітовий аналіз 8 статусних байтів у відповіді (папір, кришка, 24-годинне блокування).
 */
class DatecsFiscalClient(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val cp1251: Charset = Charset.forName("windows-1251")
) {
    private var sequenceNumber: Int = 0x40 // '@' початковий SEQ

    /**
     * Генерує наступний порядковий номер кадру (0x40..0x7C).
     */
    private fun nextSeq(): Byte {
        val seq = sequenceNumber.toByte()
        sequenceNumber = if (sequenceNumber >= 0x7C) 0x40 else sequenceNumber + 1
        return seq
    }

    /**
     * Кодує ціле число у 4 байти за стандартом Datecs Nibble Encoding (hex_digit + 0x30).
     */
    private fun encodeNibbles(value: Int): ByteArray {
        val hex = String.format("%04X", value and 0xFFFF)
        val result = ByteArray(4)
        for (i in 0 until 4) {
            val nibble = hex[i].digitToInt(16)
            result[i] = (nibble + 0x30).toByte()
        }
        return result
    }

    /**
     * Складає повний фрейм запиту та відправляє його в потік (Bluetooth SPP / USB / TCP).
     */
    fun sendCommand(cmdCode: Int, dataParams: List<String> = emptyList()): DatecsResponse {
        val seq = nextSeq()
        val cmdBytes = encodeNibbles(cmdCode)

        // Формуємо корисні дані через розділювач табуляції '\t'
        val dataStr = if (dataParams.isNotEmpty()) dataParams.joinToString("\t") + "\t" else ""
        val dataBytes = dataStr.toByteArray(cp1251)

        // Довжина: від LEN (виключаючи) до ENQ (включаючи) + зміщення 0x20
        // LEN (4B) + SEQ (1B) + CMD (4B) + DATA (NB) + ENQ (1B) = 10 + N байт
        val lengthValue = (1 + 4 + dataBytes.size + 1) + 0x20
        val lenBytes = encodeNibbles(lengthValue)

        // Тіло для розрахунку BCC: LEN + SEQ + CMD + DATA + ENQ
        val body = lenBytes + byteArrayOf(seq) + cmdBytes + dataBytes + byteArrayOf(0x05)

        // Розрахунок BCC (сума байтів body)
        var bccSum = 0
        for (b in body) {
            bccSum += (b.toInt() and 0xFF)
        }
        val bccBytes = encodeNibbles(bccSum and 0xFFFF)

        // Повний пакет: SOH + body + BCC + ETX
        val fullPacket = byteArrayOf(0x01) + body + bccBytes + byteArrayOf(0x03)

        outputStream.write(fullPacket)
        outputStream.flush()

        return readResponse(seq, cmdCode)
    }

    /**
     * Зчитує відповідь від фіскального пристрою з обробкою квитанцій SYN (0x16) та NAK (0x15).
     */
    private fun readResponse(expectedSeq: Byte, expectedCmd: Int): DatecsResponse {
        while (true) {
            val firstByte = inputStream.read()
            if (firstByte == -1) throw IllegalStateException("З'єднання розірвано")

            when (firstByte) {
                0x16 -> {
                    // SYN: апарат зайнятий тривалою операцією (Z-звіт), продовжуємо чекати
                    continue
                }
                0x15 -> {
                    // NAK: помилка контрольної суми, потрібен повтор
                    throw IllegalStateException("РРО повернув NAK (помилка контрольної суми кадру)")
                }
                0x01 -> {
                    // SOH: початок валідного кадру відповіді
                    return parseResponsePayload()
                }
            }
        }
    }

    private fun parseResponsePayload(): DatecsResponse {
        // Зчитуємо 4 байти LEN
        val lenBuf = readExact(4)
        // SEQ (1B)
        val seq = inputStream.read().toByte()
        // CMD (4B)
        val cmdBuf = readExact(4)

        // Зчитуємо дані до розділювача 0x04 (SEP перед статусом)
        val dataBytes = mutableListOf<Byte>()
        while (true) {
            val b = inputStream.read()
            if (b == 0x04 || b == -1) break
            dataBytes.add(b.toByte())
        }

        // Зчитуємо 8 байтів статусу (0x80..0xFF)
        val statusBytes = readExact(8)

        // ENQ (0x05)
        val enq = inputStream.read()
        // BCC (4B)
        val bcc = readExact(4)
        // ETX (0x03)
        val etx = inputStream.read()

        val dataString = String(dataBytes.toByteArray(), cp1251)
        val tokens = dataString.split('\t').filter { it.isNotEmpty() }
        val errorCode = tokens.firstOrNull()?.toIntOrNull() ?: 0

        return DatecsResponse(
            errorCode = errorCode,
            dataTokens = tokens.drop(1),
            rawStatus = statusBytes,
            hasPaperEnd = (statusBytes[2].toInt() and 0x01) != 0,
            isCoverOpen = (statusBytes[0].toInt() and 0x40) != 0,
            isBlocked24Hours = (statusBytes[1].toInt() and 0x01) != 0,
            isFiscalized = (statusBytes[5].toInt() and 0x08) != 0
        )
    }

    private fun readExact(count: Int): ByteArray {
        val buf = ByteArray(count)
        var read = 0
        while (read < count) {
            val r = inputStream.read(buf, read, count - read)
            if (r == -1) throw IllegalStateException("Передчасний кінець потоку")
            read += r
        }
        return buf
    }
}

data class DatecsResponse(
    val errorCode: Int,
    val dataTokens: List<String>,
    val rawStatus: ByteArray,
    val hasPaperEnd: Boolean,
    val isCoverOpen: Boolean,
    val isBlocked24Hours: Boolean,
    val isFiscalized: Boolean
)
