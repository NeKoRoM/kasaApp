package com.example.exelioshop

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * [ExampleInstrumentedTest] — базовий інструментальний тест модуля застосунку ExelioShop.
 * 
 * Перевіряє коректність запуску в середовищі Android та відповідність ідентифікатора пакета `com.example.exelioshop`.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Отримання цільового контексту Android під час тестування
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.exelioshop", appContext.packageName)
    }
}