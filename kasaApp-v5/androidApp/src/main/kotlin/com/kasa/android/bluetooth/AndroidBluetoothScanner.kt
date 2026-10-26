package com.kasa.android.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Логіка сканування та моніторингу Bluetooth-пристроїв (Android SDK).
 * Керує життєвим циклом адаптера, спареними пристроями та системним BroadcastReceiver.
 */
class AndroidBluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _bondedDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _bondedDevices.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _scanState = MutableStateFlow<BluetoothScanState>(BluetoothScanState.Idle)
    val scanState: StateFlow<BluetoothScanState> = _scanState.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return

                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).takeIf { it != Short.MIN_VALUE }
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                        ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: "РРО / Ваги (${device.address.takeLast(5)})"
                    val address = device.address

                    val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                    val item = DiscoveredBluetoothDevice(name, address, isBonded, rssi)

                    val current = _scannedDevices.value.toMutableList()
                    val existingIdx = current.indexOfFirst { it.address == address }
                    if (existingIdx >= 0) {
                        current[existingIdx] = item
                    } else {
                        current.add(item)
                    }
                    _scannedDevices.value = current
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _scanState.value = BluetoothScanState.Scanning
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _scanState.value = BluetoothScanState.Idle
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshBondedDevices()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                    if (_isBluetoothEnabled.value) {
                        refreshBondedDevices()
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
        refreshBondedDevices()
    }

    fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            return connect == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun hasScanPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            return connect && scan
        }
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission() || !adapter.isEnabled) {
            _bondedDevices.value = emptyList()
            return
        }

        try {
            val bonded = adapter.bondedDevices?.map { dev ->
                val name = dev.name?.takeIf { it.isNotBlank() } ?: "РРО / Ваги"
                DiscoveredBluetoothDevice(name, dev.address, isBonded = true)
            } ?: emptyList()
            _bondedDevices.value = bonded
        } catch (_: SecurityException) {
            _bondedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!hasScanPermissions()) {
            _scanState.value = BluetoothScanState.Error("Необхідно надати дозвіл на доступ до пристроїв поблизу")
            return false
        }
        if (!adapter.isEnabled) {
            _scanState.value = BluetoothScanState.Error("Bluetooth вимкнено на пристрої")
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnabled()) {
            _scanState.value = BluetoothScanState.Error("Увімкніть геолокацію (GPS) для пошуку Bluetooth")
            return false
        }

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
                Thread.sleep(150)
            }
            _scannedDevices.value = emptyList()
            var started = adapter.startDiscovery()
            if (!started) {
                Thread.sleep(200)
                started = adapter.startDiscovery()
            }
            if (started) {
                _scanState.value = BluetoothScanState.Scanning
            } else {
                _scanState.value = BluetoothScanState.Error("Не вдалося запустити сканування ефіру")
            }
            return started
        } catch (e: SecurityException) {
            _scanState.value = BluetoothScanState.Error("Помилка доступу до Bluetooth: ${e.message}")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            android.util.Log.w("BtScanner", "Stop discovery failed: ${e.message}")
        }
        _scanState.value = BluetoothScanState.Idle
    }

    private fun registerReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        isReceiverRegistered = true
    }

    fun release() {
        stopDiscovery()
        if (isReceiverRegistered) {
            runCatching { context.unregisterReceiver(receiver) }
            isReceiverRegistered = false
        }
    }
}
