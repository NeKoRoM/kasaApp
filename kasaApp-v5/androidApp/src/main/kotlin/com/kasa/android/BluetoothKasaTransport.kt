package com.kasa.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.kasa.android.bluetooth.BluetoothConstants
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import com.kasa.transport.StreamKasaTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Апаратний транспортний адаптер для взаємодії з касою Exellio/Datecs
 * через Android Bluetooth Classic RFCOMM (SPP профіль).
 * Підтримує чотирирівневу стратегію сокета (Insecure/Secure SPP UUID + рефлексивний fallback на канал 1),
 * автоматичний запит на спарювання (PIN 0000/1234) та повну утилізацію дескрипторів сокетів.
 */
class BluetoothKasaTransport(
    private val targetDevice: BluetoothDevice? = null,
    private val adapter: BluetoothAdapter? = null
) : KasaTransport {

    companion object {
        val SPP_UUID: UUID = BluetoothConstants.SPP_UUID
    }

    private var activeDevice: BluetoothDevice? = targetDevice
    private var socket: BluetoothSocket? = null
    private var streamDelegate: StreamKasaTransport? = null

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    override val isConnected: Boolean
        get() = _state.value is TransportState.Connected && socket?.isConnected == true

    override val isScannerRegistrationMode: Boolean = true

    @SuppressLint("MissingPermission")
    override suspend fun connect(target: String): Boolean = withContext(Dispatchers.IO) {
        disconnect()

        val dev = resolveDevice(target)
        if (dev == null) {
            _state.value = TransportState.Error("Не вдалося ідентифікувати Bluetooth-пристрій: $target")
            return@withContext false
        }
        activeDevice = dev

        val devName = runCatching { dev.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: dev.address
        _state.value = TransportState.Connecting(devName)

        // Скасування активного пошуку перед створенням сокета (критично для уникнення тайм-ауту)
        cancelActiveDiscovery()
        Thread.sleep(200)

        // Якщо пристрій ще не спарено - ініціюємо запит на спарювання
        if (dev.bondState == BluetoothDevice.BOND_NONE) {
            try {
                dev.createBond()
            } catch (e: SecurityException) {
                android.util.Log.w("BtKasa", "Bonding initiation failed: ${e.message}")
            }
        }

        val connected = openRfcommSocketWithFallback(dev)
        if (connected == null) {
            _state.value = TransportState.Error(
                "Не вдалося встановити зв'язок з $devName. Переконайтеся, що апарат увімкнено та спарено (PIN: 0000 або 1234)."
            )
            return@withContext false
        }

        socket = connected
        val delegate = StreamKasaTransport(
            inputStream = connected.inputStream,
            outputStream = connected.outputStream,
            targetName = devName
        )
        streamDelegate = delegate
        _state.value = TransportState.Connected(devName)
        true
    }

    @Suppress("DEPRECATION")
    private fun getBluetoothAdapter(): BluetoothAdapter? = adapter ?: BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    private fun resolveDevice(target: String): BluetoothDevice? {
        if (targetDevice != null) return targetDevice
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) return null

        val btAdapter = getBluetoothAdapter() ?: return null
        return runCatching { btAdapter.getRemoteDevice(cleanTarget) }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun cancelActiveDiscovery() {
        try {
            val btAdapter = getBluetoothAdapter()
            if (btAdapter?.isDiscovering == true) {
                btAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            android.util.Log.w("BtKasa", "Cancel discovery failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun openRfcommSocketWithFallback(device: BluetoothDevice): BluetoothSocket? {
        // Рівень 1: Стандартний RFCOMM SPP (як у ConnectThread kasaApp3.0)
        tryConnectSocket { device.createRfcommSocketToServiceRecord(SPP_UUID) }?.let { return it }

        // Рівень 2: Insecure RFCOMM SPP
        tryConnectSocket { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) }?.let { return it }

        // Рівень 3: RFCOMM на канал 1 через рефлексію
        tryConnectSocket {
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            m.invoke(device, 1) as BluetoothSocket
        }?.let { return it }

        // Рівень 4: Insecure RFCOMM на канал 1 через рефлексію
        tryConnectSocket {
            val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
            m.invoke(device, 1) as BluetoothSocket
        }?.let { return it }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun tryConnectSocket(factory: () -> BluetoothSocket): BluetoothSocket? {
        var s: BluetoothSocket? = null
        return try {
            s = factory()
            s.connect()
            s
        } catch (e: Throwable) {
            android.util.Log.d("BtKasa", "Socket connect attempt failed: ${e.message}")
            try {
                s?.close()
            } catch (closeEx: Throwable) {
                android.util.Log.d("BtKasa", "Socket close failed: ${closeEx.message}")
            }
            Thread.sleep(150)
            null
        }
    }

    override suspend fun sendFrame(frame: ByteArray): Boolean {
        val delegate = streamDelegate ?: return false
        android.util.Log.d("BtKasa", "TX (${frame.size}B): ${String(frame, Charsets.ISO_8859_1)}")
        return delegate.sendFrame(frame)
    }

    override suspend fun receiveFrame(timeoutMs: Long): ByteArray? {
        val delegate = streamDelegate ?: return null
        val rx = delegate.receiveFrame(timeoutMs)
        if (rx != null) {
            android.util.Log.d("BtKasa", "RX (${rx.size}B): ${String(rx, Charsets.ISO_8859_1)}")
        } else {
            android.util.Log.w("BtKasa", "RX TIMEOUT (${timeoutMs}ms)")
        }
        return rx
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        streamDelegate?.disconnect()
        streamDelegate = null
        try {
            socket?.close()
        } catch (e: IOException) {
            android.util.Log.w("BtKasa", "Socket disconnect close error: ${e.message}")
        }
        socket = null
        _state.value = TransportState.Disconnected
    }

    fun getDeviceAddress(): String? = activeDevice?.address

    fun isSocketConnected(): Boolean = socket?.isConnected == true
}
