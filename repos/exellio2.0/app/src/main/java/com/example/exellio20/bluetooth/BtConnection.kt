package com.example.exelioshop20.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.example.exelioshop.bluetooth.ConnectThread
import com.example.exelioshop.bluetooth.ReceiveThread
import com.example.exelioshop.classes.Protocol
import java.nio.charset.Charset

/**
 * ====================================================================================================
 * ГОЛОВНИЙ КОНТРОЛЕР BLUETOOTH-З'ЄДНАННЯ (BT CONNECTION CONTROLLER - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Центральний компонент управління бездротовим каналом зв'язку з касовим апаратом або сканером:
 * 1. [connect]: ініціює асинхронне підключення до MAC-адреси через [ConnectThread].
 * 2. [sendMessage]: пакує текстові або байтові команди у протокольні кадри Datecs через [Protocol.kasaWrite]
 *    з кодуванням `Windows-1251` і надсилає у сокет.
 * 3. [sendMessageBar]: надсилає сканований штрихкод із суфіксом повернення каретки `\r` (`\u000D`).
 * 4. [addLisener]: реєструє слухача стану з'єднання [ListenerCS].
 */
class BtConnection(val adapter: BluetoothAdapter, private val context: Context) {

    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    var bluetoothDevice: BluetoothDevice? = null
    val com = Protocol()
    var connectState = false
    lateinit var cThread: ConnectThread
    var bTListener: ListenerCS? = null

    /**
     * Реєструє слухача подій зміни стану Bluetooth каналу.
     */
    fun addLisener(listener: ListenerCS) {
        bTListener = listener
    }

    /**
     * Підключається до віддаленого Bluetooth-пристрою за MAC-адресою [mac].
     */
    fun connect(mac: String, listener: ReceiveThread.Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            bluetoothDevice = device
            device.let {
                cThread = ConnectThread(it, listener)
                cThread.start()
                connectState = true
            }
        }
        btConnCheck()
    }

    /**
     * Перевіряє стан підключення GATT профілю.
     */
    @SuppressLint("MissingPermission")
    fun btConnCheck() {
        btManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
    }

    /**
     * Відправляє текстову команду до РРО, пакуючи її у кадр протоколу Datecs у кодуванні Windows-1251.
     */
    fun sendMessage(message: String) {
        try {
            val w1251: Charset = charset("Windows-1251")
            cThread.rThread.sendMessege(com.kasaWrite(message.toByteArray(w1251)))
            connectState = true
        } catch (e: Exception) {
            Log.e("MyLog", "btconn: помилка відправки рядка команди: ${e.message}")
            bTListener?.onCheckBtState(false)
            connectState = false
        }
    }

    /**
     * Відправляє байтовий масив команди до РРО з автоматичним кадруванням протоколу.
     */
    fun sendMessage(message: ByteArray) {
        try {
            cThread.rThread.sendMessege(com.kasaWrite(message))
            connectState = true
        } catch (e: Exception) {
            Log.e("MyLog", "btconn: помилка відправки байтового масиву: ${e.message}")
            bTListener?.onCheckBtState(false)
            connectState = false
        }
    }

    /**
     * Надсилає штрихкод від сканера з додаванням символу CR (`\u000D` / `\r`).
     */
    fun sendMessageBar(message: String) {
        val msg = message + "\u000D"
        try {
            val w1251: Charset = charset("Windows-1251")
            cThread.rThread.sendMessege(msg.toByteArray(w1251))
            connectState = true
        } catch (e: Exception) {
            Log.e("MyLog", "btconn: помилка відправки штрихкоду: ${e.message}")
            bTListener?.onCheckBtState(false)
            connectState = false
        }
    }

    /**
     * Інтерфейс зворотного зв'язку про зміну працездатності Bluetooth-каналу.
     */
    interface ListenerCS {
        fun onCheckBtState(boolean: Boolean)
    }
}

