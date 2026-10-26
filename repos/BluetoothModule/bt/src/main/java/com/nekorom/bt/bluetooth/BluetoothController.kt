package com.nekorom.bt.bluetooth

import android.bluetooth.BluetoothAdapter

/**
 * Автономний Bluetooth контролер (модуль BluetoothModule) для з'єднання з вагами та послідовними портами.
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/bluetooth/BluetoothController.kt
 *
 * Архітектурна роль:
 * - Спрощена версія контролера Bluetooth Classic SPP для автономного тестового модуля ваг.
 * - Керує життєвим циклом потоку ConnectThread.
 * - Транслює сирі текстові повідомлення в потік сокета без Datecs-кадрування (використовується для ваг, що передають рядок ваги за протоколом CAS/Vag/DIGI).
 */
class BluetoothController(private val adapter: BluetoothAdapter) {
    /** Потік активного з'єднання через RFCOMM сокет */
    private var connectThread: ConnectThread? = null

    /**
     * Підключення до пристрою за заданою MAC-адресою.
     */
    fun connect(mac: String, listener: Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            connectThread = ConnectThread(device, listener)
            connectThread?.start()
        }
    }

    /**
     * Відправка сирого рядка безпосередньо у сокет.
     */
    fun sendMessage(message: String) {
        connectThread?.sendMessage(message)
    }

    /**
     * Закриття сокета та очищення потоку з'єднання.
     */
    fun closeConnection() {
        connectThread?.closeConnection()
    }

    /**
     * Інтерфейс зворотного виклику для передачі отриманих від ваг даних.
     */
    interface Listener {
        fun onReceive(message: String)
    }

    companion object {
        /** Рядок успішного підключення */
        const val BLUETOOTH_CONNECTED = "bluetooth_connected"

        /** Рядок обриву або відсутності зв'язку */
        const val BLUETOOTH_NOT_CONNECTED = "bluetooth_not_connected"
    }
}