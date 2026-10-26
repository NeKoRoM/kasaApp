package com.nekorom.bt

import android.bluetooth.BluetoothDevice

/**
 * Елемент моделі списку Bluetooth-пристроїв.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/ListItem.kt
 *
 * @property device Нативний системний об'єкт BluetoothDevice Android SDK (містить ім'я, MAC, тип підключення).
 * @property isChecked Чи позначено цей пристрій як поточний вибраний касовий апарат або електронні ваги.
 */
data class ListItem(
    val device: BluetoothDevice,
    val isChecked: Boolean
)

