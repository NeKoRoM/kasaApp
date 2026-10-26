package nekorom.com.kasaApp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * ====================================================================================================
 * ІНСТРУМЕНТАЛЬНИЙ ТЕСТ ДЛЯ ПЛАТФОРМИ ANDROID (ANDROID INSTRUMENTED TEST)
 * ====================================================================================================
 *
 * Виконується на фізичному пристрої Android або в емуляторі під управлінням JUnit4 runner.
 * Перевіряє інтеграцію з Android SDK та правильність ідентифікатора пакету додатку.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /**
     * Перевірка доступності контексту цільового застосунку та валідності назви пакета.
     */
    @Test
    fun useAppContext() {
        // Контекст додатку під час виконання інструментального тесту
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("nekorom.com.kasaApp.test", appContext.packageName)
    }
}