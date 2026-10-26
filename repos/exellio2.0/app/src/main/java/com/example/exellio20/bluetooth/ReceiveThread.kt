package com.example.exelioshop.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * ====================================================================================================
 * ПОТІК ЧИТАННЯ ТА ЗАПИСУ BLUETOOTH SPP (RECEIVE THREAD - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Окремий фоновий потік [Thread], що забезпечує двосторонній потоковий обмін байтами через сокет [bSocket]:
 * 1. Читання: нескінченний цикл [run], що блокується на [inStream.read] до надходження байтів від РРО.
 * 2. Декодування: перетворення байтів у рядок за допомогою фіскального кодування `Windows-1251` (СР1251).
 * 3. Контроль таймауту відповіді: корутини [job] та [joberr] генерують подію відновлення зв'язку "try_to_fix"
 *    або сповіщення "!!!!!", якщо каса не відповідає понад 500 мс.
 * 4. Запис: метод [sendMessege] для надсилання спакованих протокольних кадрів у сокет РРО.
 */
class ReceiveThread(val bSocket: BluetoothSocket, val listener: Listener) : Thread() {
    var inStream: InputStream? = null
    var outStream: OutputStream? = null
    var isAnswer: Boolean = false
    var job: Job? = null
    var joberr: Job? = null



    init {
        try {
                inStream = bSocket.inputStream
        }catch (i: IOException)
        {

        }
        try {
            outStream = bSocket.outputStream
        }catch (i: IOException)
        {

        }
    }



    /**
     * Головний цикл читання потоку байтів від касового апарата.
     */
    override fun run() {
        val buffer = ByteArray(40960)
        val bufInputStream = BufferedInputStream(inStream, 20480)
        while (true)
            try {
                val size = inStream?.read(buffer) ?: break

                // Декодування сирих байтів у текстовий рядок з кодуванням Windows-1251
                val w1251: Charset = charset("Windows-1251")
                val messege = String(buffer, 0, size, w1251)

                // Передача отриманого повідомлення слухачеві
                listener.onReceive(messege)
                isAnswer = true

                // Скидання та перезапуск сторожового таймера (500 мс)
                job?.cancel()
                joberr?.cancel()
                joberr = CoroutineScope(Dispatchers.IO).launch {
                    delay(500L)
                    listener.onReceive("try_to_fix")
                }
            } catch (i: IOException) {
                break
            }
    }

    /**
     * Відправляє масив байтів команди до касового апарата через Bluetooth сокет.
     */
    fun sendMessege(byteArrey: ByteArray?) {
        try {
            val w1251: Charset = charset("Windows-1251")
            outStream?.write(byteArrey)
            isAnswer = false

            Log.d("MyLog", "my ${byteArrey?.let { String(it, w1251) }}")
            joberr?.cancel()
            joberr = CoroutineScope(Dispatchers.IO).launch {
                delay(500L)
                listener.onReceive("try_to_fix")
            }
            job?.cancel()
            job = CoroutineScope(Dispatchers.IO).launch {
                delay(500L)
                listener.onReceive("!!!!!")
            }
        } catch (i: IOException) {
            job?.cancel()
            Log.d("MyLog", "my i: ${i.message}")
            listener.onReceive("Broken pipe")
        }
    }

    /**
     * Інтерфейс зворотного виклику для обробки подій отримання даних та підключення.
     */
    interface Listener {
        fun onReceive(message: String)
        fun onConnect(boolean: Boolean, mac: String)
    }
}