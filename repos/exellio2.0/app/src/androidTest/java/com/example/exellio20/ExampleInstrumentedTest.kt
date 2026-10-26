package com.example.exellio20

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * ====================================================================================================
 * ІНСТРУМЕНТАЛЬНИЙ ТЕСТ ДЛЯ ANDROID (EXELLIO 2.0 INSTRUMENTED TEST)
 * ====================================================================================================
 *
 * Виконується на фізичному пристрої або емуляторі Android для перевірки цілісності збірки та пакету.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /**
     * Перевірка контексту Android-додатку під час тестування.
     */
    @Test
    fun useAppContext() {
        // Контекст цільового застосунку
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.exellio20", appContext.packageName)
    }
}