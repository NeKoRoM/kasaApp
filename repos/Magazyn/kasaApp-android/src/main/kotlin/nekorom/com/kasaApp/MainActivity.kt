package nekorom.com.kasaApp

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import nekorom.com.KasaApp
import nekorom.com.SettingsRepository
import com.russhwolf.settings.SharedPreferencesSettings
import nekorom.com.kasaApp.databese.getPeopleDatabase

/**
 * ====================================================================================================
 * ГОЛОВНА АКТИВНІСТЬ ANDROID: МОДУЛЬ СКЛАДСЬКОГО ТЕРМІНАЛУ "MAGAZYN"
 * ====================================================================================================
 *
 * Даний клас є хостовою активністю для Compose Multiplatform інтерфейсу на платформі Android:
 * 1. Ініціалізує повноцінний повноекранний режим Edge-to-Edge ([enableEdgeToEdge]).
 * 2. Створює та інкапсулює репозиторій налаштувань [SettingsRepository], прив'язаний до [SharedPreferencesSettings].
 * 3. Ініціалізує локальну SQLite базу даних Room через функцію [getPeopleDatabase] та передає [Dao] в UI.
 * 4. Монтує мультиплатформний кореневий Composable-екран [KasaApp].
 */
class MainActivity : ComponentActivity() {

    /**
     * Лінива ініціалізація сховища налаштувань через Android SharedPreferences.
     * Забезпечує збереження імені поточного користувача між сесіями запуску додатку.
     */
    private val settingsRepository by lazy {
        @Suppress("DEPRECATION")
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val settings = SharedPreferencesSettings(sharedPrefs)
        SettingsRepository(settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Увімкнення сучасного безрамкового відображення контенту під системними панелями
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Отримання DAO інтерфейсу доступу до локальної бази даних Room
        val dao = getPeopleDatabase(applicationContext).dao()

        // Встановлення Compose інтерфейсу
        setContent {
            KasaApp(
                settings = settingsRepository.mySettings,
                onClearSettings = { settingsRepository.clear() },
                dao = dao
            )
        }
    }
}