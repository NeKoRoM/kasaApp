// =================================================================================================
// СИСТЕМНА АКТИВНІСТЬ ПОШУКУ ТА ВИБОРУ BLUETOOTH РРО / ВАГ (BLUETOOTH SELECTION ACTIVITY)
//
// Архітектурний шар: Presentation / Hardware Pairing Layer (Android Activity).
// Призначення:
// 1. Надає повноцінне діалогове вікно сканування Bluetooth-оточення та вибору спареного РРО Datecs або ваг.
// 2. Інтегрує класичний фрагмент DeviceListFragment у сучасний Compose-додаток.
// 3. Після вибору пристрою повертає його MAC-адресу через setResult(RESULT_OK, intent)
//    до батьківського лаунчера ActivityResultLauncher.
// =================================================================================================

package com.nekorom.kasaapp.btactivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.utils.Constants.ResultCode.MAC_RESULT

/**
 * Activity для вибору Bluetooth-пристрою (РРО Datecs або електронних ваг).
 */
class BtActivity : AppCompatActivity() {

    // ViewModel для передачі вибраної MAC-адреси з фрагмента до активності
    val viewModel: BtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bt)

        // Підписка на вибір MAC-адреси у списку пристроїв: повертає результат та закриває екран
        viewModel.mac.observe(this) { macAddress ->
            val i = Intent().apply {
                putExtra(MAC_RESULT, macAddress)
            }
            setResult(RESULT_OK, i)
            finish()
        }

        // Кнопка "Назад" на панелі дій
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Вбудовування фрагмента списку пристроїв Bluetooth
        supportFragmentManager.beginTransaction().replace(
            com.nekorom.bt.R.id.placeHolder,
            DeviceListFragment()
        ).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Обробка натискання стрілки "Назад" у заголовку
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}