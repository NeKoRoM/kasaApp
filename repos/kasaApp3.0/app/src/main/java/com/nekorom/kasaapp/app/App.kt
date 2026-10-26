// =================================================================================================
// КЛАС ДОДАТКА ТА ТОЧКА ІНІЦІАЛІЗАЦІЇ DAGGER HILT (APPLICATION CLASS)
//
// Архітектурний шар: Application Root / Dependency Injection Root.
// Призначення:
// Базовий клас додатку Android, позначений анотацією @HiltAndroidApp.
// Ініціює генерацію кореневого Dagger-компонента (SingletonComponent), який служить
// постачальником залежностей (Bluetooth, Room DB, Firebase, Repositories) для всього життєвого циклу програми.
// =================================================================================================

package com.nekorom.kasaapp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входу в застосунок kasaApp3.0.
 * Анотація [@HiltAndroidApp] генерує базовий Hilt-код для впровадження залежностей на рівні Application.
 */
@HiltAndroidApp
class App: Application() {
}