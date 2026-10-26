package com.kasa.protocol.frame

import java.io.ByteArrayOutputStream

/**
 * Потоковий накопичувач та дефрагментатор байтів кадру Datecs/Exellio.
 *
 * Вирішує проблему фрагментації Bluetooth SPP пакетів та склеювання декількох
 * відповідей в одному транспортному буфері.
 *
 * Шукає кадри формату:
 * <0x01 (SOH)> ... <0x03 (ETX)>
 */
class DatecsByteFramer {

    private val buffer = ByteArrayOutputStream()

    /**
     * Додає новий блок отриманих байтів у внутрішній накопичувач
     * та повертає список усіх повністю зібраних кадрів (якщо такі є).
     *
     * @param incomingBytes Байти, зчитані з сокету або послідовного порту.
     * @return Список завершених кадрів, кожен з яких починається з SOH (0x01) і закінчується ETX (0x03).
     */
    @Synchronized
    fun appendAndExtractFrames(incomingBytes: ByteArray): List<ByteArray> {
        if (incomingBytes.isEmpty()) return emptyList()

        buffer.write(incomingBytes)
        val allBytes = buffer.toByteArray()
        val frames = mutableListOf<ByteArray>()
        var startIndex = 0

        while (startIndex < allBytes.size) {
            // Пошук SOH (0x01)
            var sohIndex = allBytes.indexOfByte(0x01.toByte(), startIndex)
            if (sohIndex == -1) {
                // Якщо SOH взагалі не знайдено, очищаємо сміттєві байти
                buffer.reset()
                break
            }

            // Пошук ETX (0x03) після знайденого SOH
            val etxIndex = allBytes.indexOfByte(0x03.toByte(), sohIndex + 1)
            if (etxIndex == -1) {
                // Якщо в буфері є новіший SOH (попередній кадр був обірваний без ETX), беремо останній SOH
                val latestSoh = allBytes.lastIndexOfByte(0x01.toByte(), sohIndex, allBytes.size - 1)
                buffer.reset()
                buffer.write(allBytes, latestSoh, allBytes.size - latestSoh)
                break
            }

            // Якщо між sohIndex та etxIndex зустрівся новий SOH (0x01), попередній кадр був обірваний
            sohIndex = allBytes.lastIndexOfByte(0x01.toByte(), sohIndex, etxIndex - 1)

            // Повний валідний кадр знайдено
            val frameLength = (etxIndex - sohIndex) + 1
            val completeFrame = ByteArray(frameLength)
            System.arraycopy(allBytes, sohIndex, completeFrame, 0, frameLength)
            frames.add(completeFrame)

            startIndex = etxIndex + 1
            if (startIndex >= allBytes.size) {
                buffer.reset()
            }
        }

        return frames
    }

    /**
     * Перевіряє, чи є в накопичувачі незавершений кадр (починається з SOH 0x01, але без ETX 0x03).
     */
    @Synchronized
    fun hasPendingFrame(): Boolean {
        return buffer.size() > 0
    }

    /**
     * Вилучає та очищує незавершений кадр із буфера (аналог END_TRANSACTION після 300мс тиші в kasaApp3.0),
     * коли Bluetooth-модуль каси загубив кінцевий байт ETX (0x03).
     */
    @Synchronized
    fun flushIncompleteFrame(): ByteArray? {
        if (buffer.size() == 0) return null
        val allBytes = buffer.toByteArray()
        buffer.reset()
        val sohIndex = allBytes.indexOfByte(0x01.toByte(), 0)
        if (sohIndex == -1) return null
        val latestSoh = allBytes.lastIndexOfByte(0x01.toByte(), sohIndex, allBytes.size - 1)
        return allBytes.copyOfRange(latestSoh, allBytes.size)
    }

    /**
     * Примусове очищення внутрішнього буфера накопичувача (наприклад, при скиданні зв'язку).
     */
    @Synchronized
    fun clear() {
        buffer.reset()
    }

    /**
     * Пошук індексу цільового байта у масиві починаючи з заданої позиції.
     */
    private fun ByteArray.indexOfByte(target: Byte, fromIndex: Int): Int {
        for (i in fromIndex until this.size) {
            if (this[i] == target) return i
        }
        return -1
    }

    /**
     * Пошук останнього входження цільового байта у діапазоні [fromIndex..toIndex].
     */
    private fun ByteArray.lastIndexOfByte(target: Byte, fromIndex: Int, toIndex: Int): Int {
        for (i in toIndex downTo fromIndex) {
            if (this[i] == target) return i
        }
        return fromIndex
    }
}
