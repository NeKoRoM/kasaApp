package com.kasa.protocol.transport

import kotlinx.coroutines.flow.StateFlow

/**
 * Стан транспортного з'єднання з фіскальним реєстратором.
 */
sealed interface TransportState {
    data object Disconnected : TransportState
    data class Connecting(val target: String) : TransportState
    data class Connected(val target: String) : TransportState
    data class Error(val message: String) : TransportState
}

/**
 * Кросплатформенний інтерфейс обміну байтовими кадрами з РРО.
 * Реалізується через Android Bluetooth RFCOMM SPP або Desktop Serial COM-порт.
 */
interface KasaTransport {

    /** Реактивний потік поточного стану підключення */
    val state: StateFlow<TransportState>

    /** Прапорець наявності активного фізичного з'єднання */
    val isConnected: Boolean
        get() = state.value is TransportState.Connected

    /**
     * Прапорець роботи у режимі реєстрації Exellio (передача товарів чека як штрих-кодів '23<PLU><QTY><CS>\r').
     * Для реальних потокових Bluetooth/Serial каналів увімкнено для прямої сумісності з режимом «Реєстрація» РРО.
     */
    val isScannerRegistrationMode: Boolean
        get() = false

    /**
     * Встановлює фізичне з'єднання з касою за адресою [target]
     * (наприклад, MAC-адреса Bluetooth або назва COM-порту).
     */
    suspend fun connect(target: String): Boolean

    /**
     * Відправляє бінарний кадр команди до касового апарата.
     */
    suspend fun sendFrame(frame: ByteArray): Boolean

    /**
     * Очікує та зчитує повний вхідний кадр відповіді від каси з таймаутом [timeoutMs].
     * Повертає масив байтів кадру або null при таймауті чи обриві зв'язку.
     */
    suspend fun receiveFrame(timeoutMs: Long = 2000L): ByteArray?

    /**
     * Безпечно розриває зв'язок та звільняє системні дескриптори.
     */
    suspend fun disconnect()
}
