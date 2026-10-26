package com.example.exelioshop.bluetooth

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.example.exelioshop.classes.Protocol
import java.nio.charset.Charset

/**
 * =================================================================================================
 * BtConnection — Фасадний контролер Bluetooth-з'єднання для POS-додатку.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Керування циклом з'єднання: ініціалізація підключення за MAC-адресою через ConnectThread.
 * 2. Кодування команд до РРО у Windows-1251 та форматування пакетів через Datecs Protocol (kasaWrite).
 * 3. Підтримка прямої взаємодії з апаратними Bluetooth-сканерами штрихкодів (sendMessegeBar з \u0013).
 * 4. Сповіщення UI-рівня про зміну статусу з'єднання через IsConnectListener.
 * =================================================================================================
 */
class BtConnection(
    private val adapter: BluetoothAdapter,
    val listener: ReceiveThread.Listener,
    val connectListener: IsConnectListener
) {
    /** Екземпляр пакувальника протоколу Datecs (SOH/ETX/BCC) */
    val com = Protocol()
    /** Поточний стан підключення */
    var conect = false
    /** Потік відкриття з'єднання */
    lateinit var cThread: ConnectThread

    /**
     * Ініціалізація підключення до віддаленого Bluetooth-пристрою за MAC-адресою.
     * @param mac MAC-адреса фіскального реєстратора або сканера штрихкодів (наприклад "00:11:22:33:44:55")
     */
    fun connect(mac: String) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            device.let {
                cThread = ConnectThread(it, listener)
                cThread.start()
            }
        }
    }

    /**
     * Форматування та надсилання команди до РРО Datecs у кодуванні Windows-1251.
     * @param message Тіло команди РРО (наприклад, "0045X\t" або запит таблиці)
     */
    fun sendMessage(message: String) {
        val w1251: Charset = charset("Windows-1251")
        try {
            Log.d("MyLog", message)
            var z = "я"
            // Спеціальний випадок: додавання префікса для заголовка чека
            if (message.contains("Header\t")) {
                z += message
                cThread.rThread.sendMessege(com.kasaWrite(z.toByteArray(w1251)))
                conect = true
                connectListener.isConn(true)
            } else {
                cThread.rThread.sendMessege(com.kasaWrite(message.toByteArray(w1251)))
                conect = true
                connectListener.isConn(true)
            }

        } catch (e: Exception) {
            connectListener.isConn(false)
            conect = false
        }
    }

    /**
     * Надсилання керівної послідовності до Bluetooth-сканера штрихкодів.
     * Додає символ DC3 (\u0013) для активації лазерного/фото-модуля сканера.
     * @param messege Текстова команда для сканера
     */
    fun sendMessegeBar(messege: String) {
        val msg = messege + "\u0013"
        try {
            cThread.rThread.sendMessege(msg.toByteArray())
            conect = true
            connectListener.isConn(true)
        } catch (e: Exception) {
            conect = false
            connectListener.isConn(false)
        }
    }

    /**
     * Запит поточного стану підключення з викликом колбеку
     */
    fun isConnect() {
        connectListener.isConn(conect)
    }

    /**
     * Інтерфейс зворотного виклику для сповіщення про зміну стану підключення
     */
    interface IsConnectListener {
        /** Сповіщення: true - з'єднано, false - відключено/помилка */
        fun isConn(isConn: Boolean)
    }
}

