package com.nekorom.bt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Базова Activity для розміщення фрагмента пошуку та вибору Bluetooth-пристроїв.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/BaseActivity.kt
 *
 * Архітектурна роль:
 * - Слугує контейнером для `DeviceListFragment` через макет `R.layout.activity_base`.
 * - Надає ізольований екран налаштування підключення до РРО або електронних ваг.
 */
class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // Ініціалізація та відображення фрагмента зі списком Bluetooth пристроїв
        // supportFragmentManager.beginTransaction().replace(R.id.placeHolder, DeviceListFragment()).commit()
    }
}