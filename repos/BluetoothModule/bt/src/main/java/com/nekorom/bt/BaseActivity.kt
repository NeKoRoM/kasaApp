package com.nekorom.bt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * Базова Activity для розміщення фрагмента керування Bluetooth-пристроями.
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/BaseActivity.kt
 *
 * Архітектурна роль:
 * - Завантажує розмітку activity_base та вбудовує DeviceListFragment у контейнер R.id.placeHolder.
 * - Слугує екраном вибору та сполучення Bluetooth РРО та ваг.
 */
class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // Вбудовування фрагмента керування Bluetooth-пристроями
        supportFragmentManager.beginTransaction().replace(R.id.placeHolder, DeviceListFragment()).commit()
    }
}