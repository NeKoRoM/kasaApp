package com.nekorom.bluetoothmodule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nekorom.bluetoothmodule.databinding.ContentStartBinding

/**
 * Екран попереднього перегляду / стартового меню застосунку BluetoothModule.
 * Шлях у системі: BluetoothModule/app/src/main/java/com/nekorom/bluetoothmodule/StartActivity.kt
 *
 * Використовує ViewBinding для зв'язування з розміткою `ContentStartBinding` (content_start.xml).
 */
class StartActivity : AppCompatActivity() {

    /** ViewBinding для розмітки content_start */
    private lateinit var binding: ContentStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація ViewBinding та встановлення кореневого View
        binding = ContentStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}