// =================================================================================================
// МОДУЛЬ ВПРОВАДЖЕННЯ ЗАЛЕЖНОСТЕЙ BLUETOOTH (DAGGER HILT BT MODULE)
//
// Архітектурний шар: Dependency Injection Layer (Singleton Scope).
// Призначення:
// Забезпечує створення та надання синглтон-екземплярів системного BluetoothAdapter
// та обгортки BluetoothController для взаємодії з апаратним РРО Datecs та торговими вагами.
// =================================================================================================

package com.nekorom.kasaapp.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.runtime.remember
import com.nekorom.bt.bluetooth.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль постачання Bluetooth-залежностей на рівні всього життєвого циклу програми.
 */
@Module
@InstallIn(SingletonComponent::class)
class BtModule {

    /**
     * Постачає єдиний екземпляр контролера Bluetooth зв'язку [BluetoothController].
     *
     * @param btAdapter Системний BluetoothAdapter пристрою.
     */
    @Provides
    @Singleton
    fun provideBtController(btAdapter: BluetoothAdapter): BluetoothController {
        return BluetoothController(btAdapter)
    }

    /**
     * Отримує системний [BluetoothAdapter] через Android [BluetoothManager].
     *
     * @param context Глобальний контекст додатка [ApplicationContext].
     */
    @Provides
    fun provideBtAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return btManager.adapter
    }
}