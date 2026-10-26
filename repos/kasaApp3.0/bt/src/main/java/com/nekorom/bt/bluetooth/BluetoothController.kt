package com.nekorom.bt.bluetooth

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.example.exelioshop.classes.Protocol
import java.nio.charset.Charset

/**
 * Контролер Bluetooth-з'єднання з фіскальним реєстратором (РРО Datecs/Exellio) та супутнім обладнанням.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/bluetooth/BluetoothController.kt
 *
 * Архітектурна роль:
 * 1. Фасад над низькорівневим потоком ConnectThread (RFCOMM SPP сокет).
 * 2. Інтеграція з генератором протоколу Datecs (Protocol.kt) для пакування команд.
 * 3. Кодування тексту в однобайтне українське кодування Windows-1251 (CP1251) для коректного друку кирилиці (І, Ї, Є).
 * 4. Розсилка статусних повідомлень та відповідей РРО підписникам через інтерфейс Listener.
 */
class BluetoothController(private val adapter: BluetoothAdapter) {
    /** Потік активного з'єднання через RFCOMM сокет */
    private var connectThread: ConnectThread? = null

    /** Фіскальні реєстратори Exellio/Datecs в Україні працюють виключно з таблицею символів Windows-1251 */
    private val w1251: Charset = charset("Windows-1251")

    /** Список зареєстрованих слухачів подій зв'язку */
    private val listeners = mutableListOf<BluetoothController.Listener>()

    /** Екземпляр драйвера протоколу Datecs для обчислення контрольних сум і довжин */
    val com = Protocol()

    /**
     * Реєстрація слухача подій.
     * Якщо з'єднання вже встановлено — додає слухача до потоку connectThread,
     * інакше зберігає в локальному списку до моменту підключення.
     */
    fun addListener(listener: Listener) {
        connectThread?.addListener(listener) ?: listeners.add(listener)
    }

    /**
     * Отримання MAC-адреси поточного підключеного пристрою.
     */
    fun getMac(): String? {
        return connectThread?.mac
    }

    /**
     * Ініціалізація підключення до Bluetooth пристрою за MAC-адресою із передачею персонального слухача.
     */
    fun connect(mac: String, listener: Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            connectThread = ConnectThread(device, listener)
            connectThread?.start()
        }
    }

    /**
     * Ініціалізація підключення до Bluetooth пристрою за MAC-адресою.
     */
    fun connect(mac: String) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            connectThread = ConnectThread(device)
            connectThread?.start()
        }
    }

    /**
     * Відправка сирого рядка з додаванням кінцевого байта повернення каретки CR (0x0D / '\r').
     * Використовується для роботи зі сканерами штрихкодів та електронними вагами.
     */
    fun sendMessageBar(message: String) {
        val msg = message + "\u000D"
        try {
            connectThread?.sendBarMessage(msg, msg.toByteArray(w1251))
        } catch (e: Exception) {
            Log.d("MyLog", "btconn: error send mess byteArray")
        }
    }

    /**
     * Відправка фіскальної команди в РРО Datecs/Exellio.
     * Послідовність:
     * 1. Текст команди кодується в байти Windows-1251.
     * 2. com.kasaWrite(...) упаковує байти у валідний кадровий фрейм:
     *    <SOH><LEN><SEQ><DATA><ENQ><BCC><ETX>.
     * 3. Байт-масив надсилається у вихідний потік сокета через connectThread.
     * 4. Якщо сокет не підключено — усім слухачам відправляється повідомлення BLUETOOTH_NOT_CONNECTED.
     */
    fun sendMessage(message: String) {
        connectThread?.sendMessage(message, com.kasaWrite(message.toByteArray(w1251))) ?: listeners.forEach {
            it.onReceive(BLUETOOTH_NOT_CONNECTED)
        }
    }

    /**
     * Безпечне закриття з'єднання та звільнення системного Bluetooth-сокета.
     */
    fun closeConnection() {
        connectThread?.closeConnection()
    }

    /**
     * Інтерфейс зворотного виклику для отримання повідомлень з касового апарата або статусу з'єднання.
     */
    interface Listener {
        fun onReceive(message: String)
    }

    /**
     * Системні маркери станів протоколу взаємодії
     */
    companion object {
        /** Маркер успішного встановлення фізичного зв'язку по Bluetooth */
        const val BLUETOOTH_CONNECTED = "\u0001bluetooth_connected\u0003"

        /** Маркер втрати зв'язку або відсутності активного Bluetooth з'єднання */
        const val BLUETOOTH_NOT_CONNECTED = "\u0001bluetooth_not_connected\u0003"

        /** Маркер завершення транзакції обміну пакетами (після таймауту відсутності нових байтів) */
        const val END_TRANSACTION = "END_TRANSACTION"

        /** Маркер відсутності відповіді від РРО після відправки команди (таймаут 700мс) */
        const val NOT_ANSWER = "NOT_ANSWER"
    }
}