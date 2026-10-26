package com.example.exelioshop.classes

/**
 * Низькорівневий драйвер формування бінарних пакетів протоколу Datecs / Exellio для РРО.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/protocol/Protocol.kt
 *
 * Архітектурна роль:
 * 1. Формує валідний фрейм кадру для надсилання в касовий апарат через Bluetooth SPP (Serial Port Profile).
 * 2. Реалізує фірмовий формат Datecs/Exellio:
 *    <0x01 (SOH)> + <LEN (4 байти)> + <SEQ (1 байт)> + <DATA (N байт)> + <0x05 (ENQ)> + <BCC (4 байти)> + <0x03 (ETX)>
 * 3. Особливість кодування шістнадцяткових чисел (Datecs Nibble Encoding):
 *    У протоколі Datecs значення ніблів (0..15) записуються як (nibble + 0x30):
 *    - 0..9  -> ASCII '0'..'9' (0x30..0x39)
 *    - 10..15 -> ASCII ':'.. '?' (0x3A..0x3F), наприклад: 10=':', 11=';', 12='<', 13='=', 14='>', 15='?'
 *    Саме тому в чексумах та довжинах зустрічаються символи '?', '=', '<', ';'.
 */
class Protocol {
    /**
     * Поточний лічильник порядкового номера команди (Sequence Number).
     * Початкове значення: 64 (0x40 в ASCII це символ '@').
     */
    var id = 64

    /**
     * Пакує корисне навантаження (команду з параметрами, розділеними табуляцією)
     * у повний контрольований пакет протоколу Datecs з контрольною сумою.
     *
     * @param byteArray Байт-масив команди у кодуванні Windows-1251.
     * @return Повний байт-масив фрейму, готовий до відправки в сокет.
     */
    fun kasaWrite(byteArray: ByteArray): ByteArray {
        // Отримуємо наступний порядковий номер пакета
        val idL = listOf(idCommand().toByte())

        // 0x01 - SOH (Start of Header) - початок кадру Datecs
        val firstByte = listOf<Byte>(0x01)

        // 0x03 - ETX (End of Text) - завершення кадру Datecs
        val lastByte = listOf<Byte>(0x03)

        // 0x05 - ENQ (Enquiry) - роздільник перед контрольною сумою
        val ENQ = listOf<Byte>(0x05)

        // Збирання структури кадру:
        // [SOH] + sum([LENGTH] + [SEQ] + [DATA] + [ENQ]) + [ETX]
        // Де функція sum(...) обраховує BCC контрольної суми та додає 4 байти суми перед ETX.
        val idC = firstByte.toByteArray()
            .plus(sum(lengthCommand(byteArray).plus(idL.toByteArray().plus(byteArray).plus(ENQ.toByteArray()))))
            .plus(lastByte.toByteArray())

        return idC
    }

    /**
     * Генерація та інкремент порядкового номера пакета (Sequence ID).
     * Відповідно до специфікації Datecs:
     * - Допустимий діапазон значень: від 0x40 (64, '@') до 0x7C (124, '|').
     * - Після досягнення 124 лічильник скидається назад на 64 (wrap-around).
     */
    fun idCommand(): Int {
        var newId = id
        when (id) {
            in 64..123 -> newId = id + 1
            124 -> newId = 64
            else -> newId = 300 // Помилковий стан лічильника
        }
        id = newId
        return newId
    }

    /**
     * Розрахунок поля довжини команди (Length Field).
     * Довжина включає: розмір корисних даних + 4 байти BCC + 1 байт ENQ + службове зміщення протоколу (30 + 3).
     *
     * Результат кодується у 4 байти за алгоритмом Datecs Nibble Encoding (hex_digit + 0x30).
     */
    fun lengthCommand(byteArray: ByteArray): ByteArray {
        // Розрахунок довжини з урахуванням службових байтів Datecs
        val hex = Integer.toHexString(byteArray.size + 5 + 30 + 3)
        val length = mutableListOf<Byte>()

        // Доповнення нулями (0x30 = '0') зліва до 4 байтів
        when (hex.length) {
            2 -> {
                length.add(0x30)
                length.add(0x30)
            }
            3 -> length.add(0x30)
        }

        // Перетворення кожного шістнадцяткового розряду:
        // nibble (0..15) + 48 (0x30) формує фірмовий Datecs ASCII байт
        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48
            length.add(dec.toByte())
        }

        return length.toByteArray()
    }

    /**
     * Розрахунок контрольної суми (BCC - Block Check Character).
     * Алгоритм:
     * 1. Арифметична сума всіх беззнакових байтів блоку від [Length] до [ENQ].
     * 2. Перетворення суми у шістнадцяткове представлення.
     * 3. Доповнення нулями до 4 розрядів.
     * 4. Кодування кожного розряду за схемою (nibble + 0x30).
     * 5. Конкатенація вхідного блоку байтів та отриманих 4 байтів BCC.
     */
    fun sum(byteArray: ByteArray): ByteArray {
        var i = 0
        // Арифметична сума байтів як беззнакових значень (0..255)
        for (b in byteArray.toUByteArray()) {
            i = i + b.toInt()
        }

        // Переведення суми в 16-ковий рядок
        val hex = Integer.toHexString(i)
        val sum = mutableListOf<Byte>()

        // Вирівнювання до 4 розрядів
        when (hex.length) {
            2 -> {
                sum.add(0x30)
                sum.add(0x30)
            }
            3 -> sum.add(0x30)
        }

        // Кодування кожного шістнадцяткового розряду (nibble + 0x30)
        for (c in hex) {
            val dec: Int = c.toString().toInt(16) + 48
            sum.add(dec.toByte())
        }

        // Додаємо 4 байти контрольної суми в кінець пакету
        val commandWithSum = byteArray.plus(sum.toByteArray())
        return commandWithSum
    }
}