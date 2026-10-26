package com.example.exellio20.app

import android.app.Application
import com.example.exellio20.roomDB.MainDataBase
import dagger.hilt.android.HiltAndroidApp

/**
 * ====================================================================================================
 * ТОЧКА ВХОДУ ЗАСТОСУНКУ: КЛАС APPLICATION З DAGGER-HILT (EXELLIO 2.0)
 * ====================================================================================================
 *
 * Кореневий клас Android-додатку для проекту "exellio2.0".
 * Анотація [@HiltAndroidApp] ініціює кодогенерацію графу залежностей Dagger-Hilt
 * на рівні всього життєвого циклу застосунку (SingletonComponent).
 */
@HiltAndroidApp
class App : Application() {
    // Базовий екземпляр Application для Hilt DI
}


