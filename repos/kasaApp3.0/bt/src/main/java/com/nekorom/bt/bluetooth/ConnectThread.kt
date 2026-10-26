package com.nekorom.bt.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * Фоновий потік керування Bluetooth RFCOMM (SPP) сокетом для взаємодії з РРО.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/bluetooth/ConnectThread.kt
 *
 * Архітектурна роль:
 * 1. Встановлює клієнтське з'єднання по стандартному Serial Port Profile (SPP UUID: 00001101-0000-1000-8000-00805F9B34FB).
 * 2. Безперервний цикл читання (readMessage) з вхідного потоку InputStream (буфер 4 КБ) з декодуванням у Windows-1251.
 * 3. Асинхронний контроль таймаутів через Kotlin Coroutines:
 *    - ansJob: очікування відповіді від РРО. Якщо РРО не відповідає протягом 700 мс — сповіщає `NOT_ANSWER`.
 *    - endComunication: дебаунс завершення транзакції. Якщо після отримання порції байтів настає пауза 300 мс —
 *      надсилається маркер `END_TRANSACTION`, що сигналізує про завершення відповіді на команду.
 * 4. Обробка помилок вводу-виводу (IOException) та сповіщення про розрив зв'язку (`BLUETOOTH_NOT_CONNECTED`).
 */
class ConnectThread(
    private val device: BluetoothDevice,
) : Thread() {
    /** Список зареєстрованих слухачів зворотного виклику */
    private val listeners = mutableListOf<BluetoothController.Listener>()

    /** Вторинний конструктор для створення потоку з одразу переданим слухачем */
    constructor(device: BluetoothDevice, listener: BluetoothController.Listener) : this(device) {
        listeners.add(listener)
    }

    /** MAC-адреса поточного підключеного пристрою */
    var mac: String? = null

    /** Корутина детекції тиші для визначення завершення транзакції (300 мс) */
    private var endComunication: Job? = null

    /** Корутина контролю таймауту відповіді на відправлену команду (700 мс) */
    private var ansJob: Job? = null

    /** Стандартний UUID для Bluetooth Classic Serial Port Profile (SPP) */
    private val uuid: ParcelUuid = ParcelUuid.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** RFCOMM Bluetooth сокет Android */
    private var mSocket: BluetoothSocket? = null

    /** Таблиця символів Windows-1251 для читання українських фіскальних чеків та статусів */
    private val w1251: Charset = charset("Windows-1251")

    /** Додавання додаткового слухача подій зв'язку */
    fun addListener(listener: BluetoothController.Listener) {
        listeners.add(listener)
    }

    init {
        try {
            // Створення клієнтського RFCOMM сокета за SPP UUID
            mSocket = device.createRfcommSocketToServiceRecord(uuid.uuid)
        } catch (e: IOException) {
            Log.e("btIOConnThread37", e.message.toString())
        } catch (se: SecurityException) {
            Log.e("btSecurity", se.message.toString())
        }
    }

    /** Допоміжний метод отримання константи статусу відсутності з'єднання */
    fun notConn(): String {
        return BluetoothController.BLUETOOTH_NOT_CONNECTED
    }

    /**
     * Головний цикл виконання потоку підключення.
     */
    override fun run() {
        try {
            Log.d("BtMyLog", "connecting...")

            // Блокуючий виклик підключення до сокета
            if (mSocket?.isConnected == false) mSocket?.connect() else mSocket?.connect()

            // Сповіщаємо підписників про успішне фізичне з'єднання
            listeners.forEach { it.onReceive(BluetoothController.BLUETOOTH_CONNECTED) }
            mac = device.address

            // Перехід у нескінченний цикл читання даних
            readMessage()
            Log.d("BtMyLog", "connected:${device.address}")

        } catch (e: IOException) {
            Log.e("bt57", e.message.toString())
            // При помилці підключення або обриві сповіщаємо слухачів
            listeners.forEach { it.onReceive(notConn()) }
        } catch (se: SecurityException) {
            Log.e("btSecurity", se.message.toString())
        }
    }

    /**
     * Безперервне читання байтів із сокета РРО.
     * Працює доки відкритий сокет або не станеться IOException.
     */
    private fun readMessage() {
        val buffer = ByteArray(4096)

        while (true) {
            try {
                // Блокуюче зчитування байтів із вхідного потоку
                val length = mSocket?.inputStream?.read(buffer)
                val message = String(buffer, offset = 0, length ?: 0, w1251)

                // Передача отриманого повідомлення слухачам
                listeners.forEach { it.onReceive(message) }

                // Скидаємо таймер очікування відповіді, оскільки дані вже прибули
                ansJob?.cancel()

                // Перезапуск дебаунс-таймера завершення транзакції (пауза 300 мс)
                endComunication?.cancel()
                endComunication = CoroutineScope(Dispatchers.IO).launch {
                    delay(300L)
                    // Якщо протягом 300 мс більше не прибуло байтів — транзакція завершена
                    listeners.forEach { it.onReceive(BluetoothController.END_TRANSACTION) }
                }

            } catch (e: IOException) {
                Log.e("bt89", e.message.toString())
                listeners.forEach { it.onReceive(notConn()) }
                break
            }
        }
    }

    /**
     * Відправка фіскальної команди в РРО з запуском таймера відповіді (700 мс).
     */
    fun sendMessage(message: String, messageByteArray: ByteArray) {
        try {
            mSocket?.outputStream?.write(messageByteArray)
            Log.d("SendBtMess", message)

            // Запуск таймауту: якщо РРО не надішле відповідь за 700 мс, сповіщаємо NOT_ANSWER
            ansJob = CoroutineScope(Dispatchers.IO).launch {
                delay(700L)
                listeners.forEach { it.onReceive(BluetoothController.NOT_ANSWER) }
            }
        } catch (e: IOException) {
            Log.e("bt102", e.message.toString())
            listeners.forEach { it.onReceive(notConn()) }
        }
    }

    /**
     * Відправка службового повідомлення без запуску таймауту (для сканера / ваг).
     */
    fun sendBarMessage(message: String, messageByteArray: ByteArray) {
        try {
            mSocket?.outputStream?.write(messageByteArray)
            Log.d("SendBtBarMess", message)
        } catch (e: IOException) {
            Log.e("bt121", e.message.toString())
            listeners.forEach { it.onReceive(notConn()) }
        }
    }

    /**
     * Коректне закриття сокета та звільнення системних ресурсів Android.
     */
    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (e: IOException) {
            Log.e("bt111", e.message.toString())
        }
    }
}