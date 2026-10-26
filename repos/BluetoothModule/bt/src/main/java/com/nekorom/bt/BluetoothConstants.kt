package com.nekorom.bt

/**
 * Константи ключів SharedPreferences для збереження MAC-адреси пристрою (модуль BluetoothModule).
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/BluetoothConstants.kt
 */
object BluetoothConstants {
    /** Ім'я файлу налаштувань */
    const val PREFERENCES = "main_bt_preferences"

    /** Ключ збереженої MAC-адреси */
    const val MAC = "mac_bt"
}