package com.nekorom.bt.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * Потік керування клієнтським RFCOMM SPP з'єднанням (модуль BluetoothModule).
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/bluetooth/ConnectThread.kt
 *
 * Архітектурна роль:
 * 1. Встановлює зв'язок із Bluetooth вагами / послідовними адаптерами за стандартним SPP UUID:
 *    "00001101-0000-1000-8000-00805F9B34FB".
 * 2. У безперервному циклі вичитує буфер розміром 4096 байтів та перетворює його у текст згідно з Windows-1251.
 * 3. Транслює отримані фрагменти рядків (потік значень ваги) слухачеві `listener.onReceive(message)`.
 * 4. Забезпечує відправку команд у ваги (наприклад, команду тарування 'T\r\n').
 */
class ConnectThread(
    private val device: BluetoothDevice,
    val listener: BluetoothController.Listener
) : Thread() {
    /** Стандартний UUID для послідовного порту Bluetooth Classic SPP */
    private val uuid = "00001101-0000-1000-8000-00805F9B34FB"

    /** Системний RFCOMM сокет */
    private var mSocket: BluetoothSocket? = null

    /** Таблиця символів Windows-1251 */
    private val w1251: Charset = charset("Windows-1251")

    init {
        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
        } catch (e: IOException) {
            Log.e("BtConnectThread", "Помилка створення сокета: ${e.message}")
        } catch (se: SecurityException) {
            Log.e("BtConnectThread", "Відсутній дозвіл BLUETOOTH_CONNECT: ${se.message}")
        }
    }

    override fun run() {
        try {
            // Блокуюче встановлення з'єднання з вагами
            mSocket?.connect()
            listener.onReceive(BluetoothController.BLUETOOTH_CONNECTED)
            // Запуск безперервного циклу вичитування даних ваги
            readMessage()
        } catch (e: IOException) {
            listener.onReceive(BluetoothController.BLUETOOTH_NOT_CONNECTED)
        } catch (se: SecurityException) {
            listener.onReceive(BluetoothController.BLUETOOTH_NOT_CONNECTED)
        }
    }

    /**
     * Цикл зчитування даних із потоку InputStream.
     * При кожному надходженні байтів створюється рядок та передається слухачу.
     */
    private fun readMessage() {
        val buffer = ByteArray(4096)

        while (true) {
            try {
                val length = mSocket?.inputStream?.read(buffer)
                val message = String(buffer, offset = 0, length ?: 0, w1251)
                listener.onReceive(message)
                Log.d("btMess", message)
            } catch (e: IOException) {
                listener.onReceive(BluetoothController.BLUETOOTH_NOT_CONNECTED)
                break
            }
        }
    }

    /**
     * Відправка команди в пристрій (наприклад, тарування або запит статусу).
     */
    fun sendMessage(message: String) {
        try {
            mSocket?.outputStream?.write(message.toByteArray(w1251))
            Log.d("SendBtMess", message)
        } catch (e: IOException) {
            listener.onReceive(BluetoothController.BLUETOOTH_NOT_CONNECTED)
        }
    }

    /**
     * Закриття з'єднання та очищення сокета.
     */
    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (e: IOException) {
            Log.e("BtConnectThread", "Помилка закриття сокета: ${e.message}")
        }
    }
}