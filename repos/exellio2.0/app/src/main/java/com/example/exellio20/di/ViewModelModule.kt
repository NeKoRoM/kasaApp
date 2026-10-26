package com.example.exellio20.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.exelioshop20.bluetooth.BtConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ====================================================================================================
 * МОДУЛЬ ВПРОВАДЖЕННЯ ЗАЛЕЖНОСТЕЙ ОБЛАДНАННЯ ТА BLUETOOTH (HILT MODULE - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Відповідає за надання системного контролера Bluetooth-з'єднання [BtConnection]
 * для комунікації з касовими апаратами та фіскальними реєстраторами Datecs/Exellio.
 */
@Module
@InstallIn(SingletonComponent::class)
class ViewModelModule {

    /**
     * Створює та надає об'єкт управління з'єднанням Bluetooth Classic SPP.
     * Отримує системний [BluetoothManager] через контекст додатку.
     */
    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BtConnection {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BtConnection(btManager.adapter, context)
    }
}