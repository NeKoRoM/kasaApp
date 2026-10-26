package com.nekorom.bt

/**
 * Константи для збереження та завантаження налаштувань Bluetooth-модуля.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/BluetoothConstants.kt
 *
 * Використовуються SharedPreferences для збереження вибраного касового апарата або ваг:
 * - PREFERENCES: Ім'я файлу спільних налаштувань ("main_bt_preferences").
 * - MAC: Ключ для збереження MAC-адреси вибраного пристрою ("mac_bt").
 */
object BluetoothConstants {
    /** Назва файлу SharedPreferences */
    const val PREFERENCES = "main_bt_preferences"

    /** Ключ для збереження MAC-адреси обраного РРО або ваг */
    const val MAC = "mac_bt"
}