package com.example.exelioshop.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * =================================================================================================
 * ReceiveThread — Фоновий робочий потік читання та запису даних через Bluetooth-сокет.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Безперервне слухання вхідного потоку InputStream від РРО Datecs / Exellio в кодуванні Windows-1251.
 * 2. Асинхронне надсилання бінарних пакетів у вихідний потік OutputStream.
 * 3. Контроль таймаутів відповіді РРО (Watchdog Coroutine):
 *    - Якщо відповідь не надійшла за 1.0 с -> еміт "try_to_fix" (спроба відновлення зв'язку/повтор).
 *    - Якщо відповідь не надійшла за 2.0 с -> еміт "!!!!!" (повідомлення про критичний збій/таймаут).
 * =================================================================================================
 */
class ReceiveThread(val bSocket: BluetoothSocket, val listener: Listener) : Thread() {
    /** Вхідний потік байтів від РРО */
    var inStream: InputStream? = null
    /** Вихідний потік байтів до РРО */
    var outStream: OutputStream? = null
    /** Прапорець отримання відповіді на поточну команду */
    var isAnswer: Boolean = false
    /** Корутина таймауту другого рівня (2000 мс — критичний таймаут) */
    var job: Job? = null
    /** Корутина таймауту першого рівня (1000 мс — попередній таймаут спроби виправлення) */
    var joberr: Job? = null

    init {
        // Отримання потоків вводу/виводу з відкритого сокету
        try {
            inStream = bSocket.inputStream
        } catch (i: IOException) {
            // Помилка отримання вхідного потоку
        }
        try {
            outStream = bSocket.outputStream
        } catch (i: IOException) {
            // Помилка отримання вихідного потоку
        }
    }

    /**
     * Основний цикл прийому байтів від фіскального реєстратора.
     * Буфер розміром 40 КБ дозволяє приймати об'ємні відповіді (наприклад, дампи таблиць артикулів).
     */
    override fun run() {
        val buf = ByteArray(40960)
        while (true)
            try {
                // Блокуюче читання з Bluetooth-потоку
                val size = inStream?.read(buf)

                // Декодування в українську кодову сторінку Windows-1251 (стандарт Datecs РРО)
                val w1251: Charset = charset("Windows-1251")
                val messege = String(buf, 0, size!!, w1251)

                // Передача сирої відповіді підписнику (ViewModel / Activity)
                listener.onReceive(messege)
                isAnswer = true
                Log.d("MyLog", "msg: $messege")

            } catch (i: IOException) {
                // Переривання циклу при закритті сокету або обриві зв'язку
                break
            }
    }

    /**
     * Надсилання бінарного пакету у фіскальний реєстратор з запуском сторожових таймерів.
     * @param byteArrey Сформований пакет протоколу Datecs (SOH..команда..ETX..контрольна сума)
     */
    fun sendMessege(byteArrey: ByteArray?) {
        try {
            val w1251: Charset = charset("Windows-1251")
            Log.d("MyLog", "start trySM")
            // Фізичний запис байтів у сокет
            outStream?.write(byteArrey)
            isAnswer = false

            Log.d("MyLog", "my ${byteArrey?.let { String(it, w1251) }}")

            // Сторожовий таймер 1: через 1 сек передати сигнал спроби відновлення
            joberr?.cancel()
            joberr = CoroutineScope(Dispatchers.IO).launch {
                delay(1000L)
                listener.onReceive("try_to_fix")
            }

            // Сторожовий таймер 2: через 2 сек зафіксувати критичний таймаут зв'язку
            job?.cancel()
            job = CoroutineScope(Dispatchers.IO).launch {
                delay(2000L)
                listener.onReceive("!!!!!")
            }

        } catch (i: IOException) {
            Log.d("MyLog", "my i: ${i.message}")
        }
    }

    /**
     * Інтерфейс зворотного виклику для подій прийому даних та зміни стану підключення
     */
    interface Listener {
        /** Викликається при отриманні текстового повідомлення від РРО або коду помилки */
        fun onReceive(message: String)
        /** Викликається при встановленні або втраті зв'язку з вказаною MAC-адресою */
        fun onConnect(boolean: Boolean, mac: String)
    }
}