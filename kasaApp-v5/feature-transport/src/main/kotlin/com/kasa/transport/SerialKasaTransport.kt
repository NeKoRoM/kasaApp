package com.kasa.transport

import com.fazecast.jSerialComm.SerialPort
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Апаратний транспорт для зв'язку з касою через послідовний порт
 * (USB-Serial або віртуальний Bluetooth SPP COM-порт на macOS/Windows/Linux).
 */
class SerialKasaTransport(
    private val config: SerialPortConfig = SerialPortConfig()
) : KasaTransport {

    private var serialPort: SerialPort? = null
    private var streamDelegate: StreamKasaTransport? = null

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    companion object {
        /**
         * Сканує та повертає список усіх виявлених в операційній системі портів.
         */
        fun listAvailablePorts(): List<SerialPortInfo> {
            return SerialPort.getCommPorts().map { port ->
                val desc = port.descriptivePortName ?: port.portDescription ?: port.systemPortName
                val isBt = desc.contains("Bluetooth", ignoreCase = true) ||
                        port.systemPortName.contains("Bluetooth", ignoreCase = true)
                SerialPortInfo(
                    systemPortName = port.systemPortName,
                    portDescription = desc,
                    isBluetooth = isBt
                )
            }
        }
    }

    override suspend fun connect(target: String): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        _state.value = TransportState.Connecting(target)

        val port = SerialPort.getCommPort(target)
        port.setComPortParameters(
            config.baudRate,
            config.dataBits,
            config.stopBits,
            config.parity
        )
        // Налаштування напівблокуючого таймауту зчитування (50 мс)
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 50, 0)

        if (!port.openPort()) {
            _state.value = TransportState.Error("Не вдалося відкрити послідовний порт: $target")
            return@withContext false
        }

        serialPort = port
        val delegate = StreamKasaTransport(
            inputStream = port.inputStream,
            outputStream = port.outputStream,
            targetName = target
        )
        streamDelegate = delegate
        _state.value = TransportState.Connected(target)
        true
    }

    override suspend fun sendFrame(frame: ByteArray): Boolean {
        val delegate = streamDelegate ?: return false
        return delegate.sendFrame(frame)
    }

    override suspend fun receiveFrame(timeoutMs: Long): ByteArray? {
        val delegate = streamDelegate ?: return null
        return delegate.receiveFrame(timeoutMs)
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        streamDelegate?.disconnect()
        streamDelegate = null
        serialPort?.closePort()
        serialPort = null
        _state.value = TransportState.Disconnected
    }
}
