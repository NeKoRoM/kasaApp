package com.nekorom.bt

import android.bluetooth.BluetoothDevice

/**
 * Модель представлення Bluetooth пристрою для списку RecyclerView (модуль BluetoothModule).
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/ListItem.kt
 *
 * @property device Системний об'єкт BluetoothDevice Android SDK.
 * @property isChecked Статус позначення пристрою як обраного.
 */
data class ListItem(
    val device: BluetoothDevice,
    val isChecked: Boolean
)

