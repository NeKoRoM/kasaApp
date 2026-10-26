package com.example.exelioshop.classes

/**
 * [Protocol] — низькорівневий формувач кадрів зв'язку фіскального протоколу Datecs/Exellio.
 * 
 * Структура переданого пакета (Frame Format):
 * `[SOH: 0x01] [LEN: 4 ASCII hex байти] [SEQ: 1 байт (64..124)] [DATA: N байтів команди] [ENQ: 0x05] [BCC: 4 ASCII hex байти контрольної суми] [ETX: 0x03]`
 * 
 * Особливості протоколу:
 * 1. Циклічний Sequence ID: лічильник інкрементується від 0x40 (64, '@') до 0x7C (124, '|'), після чого повертається до 64.
 * 2. Довжина кадру [lengthCommand]: розраховується з урахуванням службових байтів, переводиться в 16-ріковий формат
 *    та кодується у 4 ASCII символи шляхом додавання зміщення 0x30 (48).
 * 3. Контрольна сума BCC [sum]: сума всіх беззнакових байтів від довжини до ENQ, представлена 4 шістнадцятковими ASCII-символами.
 */
class Protocol {
    /** Поточний номер послідовності команди (початкове значення 64 / 0x40) */
    var id = 64

    /**
     * Пакування довільного масиву байтів фіскальної команди у валідний кадр протоколу Datecs.
     * 
     * @param byteArray корисне навантаження команди (наприклад, "kP\t1\t...")
     * @return сформований готовий до відправки кадр із SOH, довжиною, SEQ, ENQ, BCC та ETX
     */
    fun kasaWrite(byteArray: ByteArray): ByteArray {
        val idL = listOf(idCommand().toByte())

        val firstByte = listOf<Byte>(0x01) // SOH (Start of Header)
        val lastByte = listOf<Byte>(0x03)  // ETX (End of Text)
        val ENQ = listOf<Byte>(0x05)       // ENQ (Enquiry delimiter)

        // Збираємо тіло: довжина + SEQ + команда + ENQ, обчислюємо BCC і обрамляємо в SOH..ETX
        val idC = firstByte.toByteArray()
            .plus(sum(lengthCommand(byteArray).plus(idL.toByteArray().plus(byteArray).plus(ENQ.toByteArray()))))
            .plus(lastByte.toByteArray())

        return idC
    }

    /**
     * Генерація наступного номера послідовності пакета (Sequence ID).
     * Цикл: 64 ('@') -> 124 ('|') -> 64.
     */
    fun idCommand(): Int {
        var newId = id
        when (id) {
            in 64..123 -> newId = id + 1
            124 -> newId = 64
            else -> newId = 300
        }
        id = newId
        return newId
    }

    /**
     * Розрахунок поля довжини кадру у форматі 4 байтів ASCII Hex.
     * Враховує розмір корисного навантаження та службових супутніх полів кадру.
     */
    fun lengthCommand(byteArray: ByteArray): ByteArray {
        val hex = Integer.toHexString(byteArray.size + 5 + 30 + 3)
        val length = mutableListOf<Byte>()

        // Доповнення нулями до 4 байтів
        when (hex.length) {
            2 -> {
                length.add(0x30)
                length.add(0x30)
            }
            3 -> length.add(0x30)
        }

        // Переведення кожного 16-річного символу в ASCII байт (зміщення +48 / 0x30)
        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48
            length.add(dec.toByte())
        }

        return length.toByteArray()
    }

    /**
     * Обчислення контрольної суми (BCC) для масиву байтів.
     * Підсумовує всі байти як беззнакові (0..255) та повертає 4-символьний ASCII Hex блок.
     */
    fun sum(byteArray: ByteArray): ByteArray {
        var i = 0
        for (b in byteArray.toUByteArray()) {
            i += b.toInt()
        }

        val hex = Integer.toHexString(i)
        val sum = mutableListOf<Byte>()

        // Доповнення лідируючими нулями до 4 байтів
        when (hex.length) {
            2 -> {
                sum.add(0x30)
                sum.add(0x30)
            }
            3 -> sum.add(0x30)
        }

        // Кодування символів контрольної суми в ASCII
        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48
            sum.add(dec.toByte())
        }

        // Додавання розрахованої контрольної суми в кінець вхідного масиву
        val commandWithSum = byteArray.plus(sum.toByteArray())
        return commandWithSum
    }
}