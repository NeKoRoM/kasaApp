package com.example.exelioshop.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * ====================================================================================================
 * ПОТІК ВСТАНОВЛЕННЯ З'ЄДНАННЯ BLUETOOTH RFCOMM (CONNECT THREAD - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Окремий потік [Thread], який виконує блокуючу спробу підключення до РРО за стандартним
 * профілем Bluetooth Serial Port Profile (SPP UUID `00001101-0000-1000-8000-00805F9B34FB`).
 *
 * ЖИТТЄВИЙ ЦИКЛ ПІДКЛЮЧЕННЯ:
 * 1. Створення сокета [mSocket] через [device.createRfcommSocketToServiceRecord].
 * 2. Виклик [mSocket.connect] у методі [run].
 * 3. При успіху — створення та запуск [ReceiveThread] для обміну даними та сповіщення [listener.onConnect].
 * 4. При невдачі — закриття сокета [closeConnection] та сповіщення "not_bluetooth".
 */
@SuppressLint("MissingPermission")
class ConnectThread(private val device: BluetoothDevice, val listener: ReceiveThread.Listener) : Thread() {
    val uuid = "00001101-0000-1000-8000-00805F9B34FB"
    var mSocket: BluetoothSocket? = null
    lateinit var rThread: ReceiveThread
    var isConnectedState = false

    init {
        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
        } catch (i: IOException) {
            Log.e("ConnectThread", "Помилка створення сокета: ${i.message}")
        }
    }

    /**
     * Перевіряє, чи активне поточне з'єднання сокета.
     */
    fun isConnect(): Boolean {
        return mSocket?.isConnected ?: false
    }

    override fun run() {
        try {
            Log.d("MyLog", "Підключення до ${device.name} (${device.address})...")
            mSocket?.connect()
            Log.d("MyLog", "З'єднання встановлено успішно")

            rThread = ReceiveThread(mSocket!!, listener)
            rThread.start()
            listener.onConnect(true, device.address)
        } catch (i: IOException) {
            Log.e("MyLog", "Не вдалося підключитися до пристрою: ${i.message}")
            closeConnection()
            listener.onReceive("not_bluetooth")
        }
    }

    /**
     * Безпечно закриває сокет з'єднання.
     */
    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (i: IOException) {
            Log.e("ConnectThread", "Помилка закриття сокета: ${i.message}")
        }
    }
}