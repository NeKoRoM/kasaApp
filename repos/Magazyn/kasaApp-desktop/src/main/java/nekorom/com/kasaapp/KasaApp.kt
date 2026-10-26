@file:JvmName("KasaApp")

package nekorom.com.kasaapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.russhwolf.settings.PreferencesSettings
import nekorom.com.KasaApp
import nekorom.com.SettingsRepository
import nekorom.com.kasaapp.database.getPeopleDatabase
import java.util.prefs.Preferences

/**
 * ====================================================================================================
 * ТОЧКА ВХОДУ ДЕСКТОПНОГО ЗАСТОСУНКУ: МОДУЛЬ СКЛАДСЬКОГО ТЕРМІНАЛУ "MAGAZYN" (JVM DESKTOP)
 * ====================================================================================================
 *
 * Головна функція [main], що запускає віконний інтерфейс Compose for Desktop на ОС Windows, macOS або Linux:
 * 1. Ініціалізує локальну SQLite базу даних Room для настільної ОС через [getPeopleDatabase].
 * 2. Налаштовує персистентне збереження користувацьких параметрів через стандартний механізм [Preferences.userRoot].
 * 3. Створює та відображає вікно застосунку [Window] із завершенням процесу при закритті ([exitApplication]).
 * 4. Запускає уніфікований Composable-екран [KasaApp].
 */
fun main() = application {
    // Ініціалізація доступу до бази даних SQLite
    val dao = getPeopleDatabase().dao()

    // Налаштування збереження конфігурації у реєстрі / системних преференсах ОС
    val settingsRepository: SettingsRepository by lazy {
        val preferences = Preferences.userRoot()
        val settings = PreferencesSettings(preferences)
        SettingsRepository(settings)
    }

    // Головне десктопне вікно застосунку
    Window(
        onCloseRequest = ::exitApplication,
        title = "kmpFirst",
    ) {
        KasaApp(
            settings = settingsRepository.mySettings,
            onClearSettings = { settingsRepository.clear() },
            dao = dao
        )
    }
}