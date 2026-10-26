package com.example.exelioshop.classes

/**
 * ====================================================================================================
 * НИЗЬКОРІВНЕВИЙ ДРАЙВЕР ПРОТОКОЛУ DATECS / EXELLIO (BINARY PROTOCOL FRAMING)
 * ====================================================================================================
 *
 * Реалізує пакування та кадрування команд згідно з фірмовим протоколом зв'язку Datecs/Exellio:
 *
 * ФОРМАТ ПАКЕТА ЗАПИТУ ДО РРО:
 * `<0x01> <LEN: 4 bytes ASCII-HEX> <SEQ: 1 byte> <CMD_DATA> <0x05> <BCC: 4 bytes ASCII-HEX> <0x03>`
 *
 * ДЕТАЛІ ПОЛІВ:
 * 1. `<0x01>` (SOH) — Початок заголовка кадру.
 * 2. `<LEN>` (4 байти) — Довжина поля даних + зміщення 0x20/0x30, закодована 4 символами ASCII-HEX.
 * 3. `<SEQ>` (1 байт) — Порядковий номер команди в діапазоні 64..123 (0x40..0x7B), що інкрементується.
 * 4. `<CMD_DATA>` — Байти команди та аргументів, розділених табуляцією `\t`.
 * 5. `<0x05>` (ENQ) — Завершувач поля корисного навантаження.
 * 6. `<BCC>` (4 байти) — Контрольна сума всіх байтів від `<LEN>` до `<0x05>` включно, в ASCII-HEX.
 * 7. `<0x03>` (ETX) — Символ кінця пакета.
 */
class Protocol {
    /**
     * Поточний лічильник номера команди (діапазон 64..123).
     */
    var id = 64

    /**
     * Пакує масив байтів команди [byteArray] у валідний кадр протоколу Datecs.
     *
     * @param byteArray Байти команди (наприклад, `0x2A` для відкриття зміни, `0x2E` для закриття).
     * @return Повний бінарний кадр, готовий до відправки через Bluetooth SPP сокет.
     */
    fun kasaWrite(byteArray: ByteArray): ByteArray {
        val idL = listOf(idCommand().toByte())
        val firstByte = listOf<Byte>(0x01)
        val lastByte = listOf<Byte>(0x03)
        val ENQ = listOf<Byte>(0x05)

        val idC = firstByte.toByteArray()
            .plus(sum(lengthCommand(byteArray).plus(idL.toByteArray().plus(byteArray).plus(ENQ.toByteArray()))))
            .plus(lastByte.toByteArray())

        return idC
    }

    /**
     * Генерує наступний ідентифікатор команди із циклічним обертанням у діапазоні 64..123.
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
     * Обчислює довжину команди та повертає 4-байтове ASCII-HEX представлення.
     * Довжина включає зміщення протоколу (+5 +30 +3).
     */
    fun lengthCommand(byteArray: ByteArray): ByteArray {
        val hex = Integer.toHexString(byteArray.size + 5 + 30 + 3)
        val length = mutableListOf<Byte>()
        when (hex.length) {
            2 -> {
                length.add(0x30)
                length.add(0x30)
            }
            3 -> length.add(0x30)
        }

        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48 // Додавання 0x30 для перетворення у символ ASCII
            length.add(dec.toByte())
        }

        return length.toByteArray()
    }

    /**
     * Обчислює контрольну суму (BCC) байтового масиву та додає її як 4 ASCII-HEX байти.
     */
    fun sum(byteArray: ByteArray): ByteArray {
        var i = 0
        for (b in byteArray.toUByteArray()) {
            i += b.toInt()
        }

        val hex = Integer.toHexString(i)
        val sum = mutableListOf<Byte>()

        when (hex.length) {
            2 -> {
                sum.add(0x30)
                sum.add(0x30)
            }
            3 -> sum.add(0x30)
        }

        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48
            sum.add(dec.toByte())
        }

        val commandWithSum = byteArray.plus(sum.toByteArray())
        return commandWithSum
    }
}