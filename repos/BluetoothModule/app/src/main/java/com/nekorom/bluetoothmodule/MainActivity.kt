package com.nekorom.bluetoothmodule

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nekorom.bt.BaseActivity

/**
 * Стартова Activity застосунку тестування зв'язку з Bluetooth обладнанням.
 * Шлях у системі: BluetoothModule/app/src/main/java/com/nekorom/bluetoothmodule/MainActivity.kt
 *
 * При створенні ініціалізує розмітку та одразу перенаправляє користувача до `BaseActivity`
 * для перегляду та вибору доступних Bluetooth-пристроїв.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Перехід до екрану налаштувань та вибору пристроїв у модулі 'bt'
        startActivity(Intent(this, BaseActivity::class.java))
    }
}