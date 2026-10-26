package com.example.exelioshop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.exelioshop.bluetooth.SelectDevice
import com.example.exelioshop.databinding.ActivityBarCodBinding

/**
 * [BarCod] — допоміжна активність для швидкого переходу до вибору та підключення Bluetooth-сканера або каси.
 * 
 * Надає користувачеві кнопку підключення `connectButton`, яка ініціює відкриття вікна вибору пристрою [SelectDevice].
 */
class BarCod : AppCompatActivity() {

    private lateinit var binding: ActivityBarCodBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityBarCodBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Запуск діалогового вікна вибору Bluetooth пристрою
        binding.connectButton.setOnClickListener {
            val intent = Intent(this, SelectDevice::class.java)
            startActivity(intent)
        }
    }
}