package com.example.exelioshop.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * =================================================================================================
 * ConnectThread — Робочий потік встановлення Bluetooth RFCOMM-з'єднання з периферією.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Асинхронне відкриття RFCOMM-каналу за стандартним профілем SPP (Serial Port Profile).
 * 2. Запобігання блокуванню UI-потоку під час виконання системного виклику BluetoothSocket.connect().
 * 3. При успішному з'єднанні сповіщає лістенер та запускає ReceiveThread для обміну повідомленнями.
 * =================================================================================================
 */
@SuppressLint("MissingPermission")
class ConnectThread(private val device: BluetoothDevice, val listener: ReceiveThread.Listener) : Thread() {
    /** Стандартний UUID для Bluetooth Serial Port Profile (SPP) */
    val uuid = "00001101-0000-1000-8000-00805F9B34FB"
    /** Клієнтський сокет зв'язку з апаратним РРО / сканером */
    var mSocket: BluetoothSocket? = null
    /** Робочий потік прийому/передачі даних через відкритий сокет */
    lateinit var rThread: ReceiveThread

    init {
        // Створення сокету для підключення до віддаленого Bluetooth-пристрою
        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
        } catch (i: IOException) {
            Log.d("MyLog", "tion...")
        }
    }

    /**
     * Тіло потоку: виконання блокуючого підключення socket.connect()
     */
    override fun run() {
        try {
            Log.d("MyLog", "connection...")
            // Встановлення фізичного з'єднання (блокуюча операція)
            mSocket?.connect()
            Log.d("MyLog", "connected")
            device.address
            // Сповіщення про успішне підключення
            listener.onConnect(true, device.address)
            // Запуск потоку обробки вхідного/вихідного трафіку
            rThread = ReceiveThread(mSocket!!, listener)
            rThread.start()

        } catch (i: IOException) {
            Log.d("MyLog", "can not connect to device")
            // Закриття сокету у разі помилки зв'язку
            closeConnection()
        }
    }

    /**
     * Безпечне закриття Bluetooth-сокету при завершенні сеансу або скиданні зв'язку
     */
    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (i: IOException) {
            // Помилка закриття сокету
        }
    }
}