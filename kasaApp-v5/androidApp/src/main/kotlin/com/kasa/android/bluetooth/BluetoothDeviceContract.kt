package com.kasa.android.bluetooth

import java.util.UUID

/**
 * Модель виявленого або спареного Bluetooth-пристрою (РРО Exellio, ваги, сканери).
 */
data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val rssi: Short? = null
)

/**
 * Стан процесу пошуку нових Bluetooth-пристроїв.
 */
sealed interface BluetoothScanState {
    data object Idle : BluetoothScanState
    data object Scanning : BluetoothScanState
    data class Error(val message: String) : BluetoothScanState
}

/**
 * Константи для роботи з Bluetooth RFCOMM SPP та збереження налаштувань.
 */
object BluetoothConstants {
    /** Стандартний UUID послідовного порту Bluetooth Classic SPP */
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** Назва сховища SharedPreferences для збереження прив'язаного РРО */
    const val PREFS_NAME = "kasa_bluetooth_settings"

    /** Ключ збереженої MAC-адреси фізичного РРО */
    const val KEY_LAST_MAC = "pref_last_kasa_mac"

    /** Ключ імені останнього обраного пристрою */
    const val KEY_LAST_NAME = "pref_last_kasa_name"

    /** Ключ прапорця використання симулятора РРО замість фізичного заліза */
    const val KEY_USE_SIMULATOR = "pref_use_simulator"
}
