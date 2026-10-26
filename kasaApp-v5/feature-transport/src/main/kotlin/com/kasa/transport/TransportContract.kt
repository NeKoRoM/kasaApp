package com.kasa.transport

/**
 * Конфігурація послідовного COM/USB/Bluetooth порту.
 */
data class SerialPortConfig(
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0 // 0 = NO_PARITY
)

/**
 * Опис знайденого в системі послідовного або віртуального порту.
 */
data class SerialPortInfo(
    val systemPortName: String,
    val portDescription: String,
    val isBluetooth: Boolean = false
)

/**
 * Фабрика створення апаратних адаптерів підключення.
 */
interface PlatformTransportFactory {
    fun listSerialPorts(): List<SerialPortInfo>
}
