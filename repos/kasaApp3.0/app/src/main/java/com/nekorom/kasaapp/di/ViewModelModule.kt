// =================================================================================================
// МОДУЛЬ ВПРОВАДЖЕННЯ РЕПОЗИТОРІЇВ ДЛЯ VIEWMODEL (DAGGER HILT VIEWMODEL MODULE)
//
// Архітектурний шар: Dependency Injection Layer (ViewModel Scope).
// Призначення:
// Забезпечує створення та прив'язку репозиторіїв фіскального зв'язку (BtKasaExellioRepository)
// та мережевого Firebase-репозиторію (NetworkDataRepositoryImp) до життєвого циклу ViewModel.
// =================================================================================================

package com.nekorom.kasaapp.di

import com.nekorom.bt.bluetooth.BluetoothController
import com.nekorom.kasaapp.data.repository.BtKasaExellioRepository
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.domain.repository.BtKasaRepository
import com.nekorom.kasaapp.domain.repository.NetworkDataRepository
import com.nekorom.kasaapp.firebase.NetworkDataRepositoryImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt-модуль постачання репозиторіїв для компонентів ViewModel.
 */
@Module
@InstallIn(ViewModelComponent::class)
class ViewModelModule {

    /**
     * Постачає екземпляр хмарного репозиторію Firebase [NetworkDataRepositoryImp].
     */
    @Provides
    fun provideNetworkRepositoryImp(): NetworkDataRepositoryImp {
        return NetworkDataRepositoryImp()
    }

    /**
     * Постачає реалізацію фіскального репозиторію РРО Datecs [BtKasaRepository].
     *
     * @param bluetoothController Контролер Bluetooth-з'єднання з апаратом.
     * @param appDatabase База даних Room для збереження товарів та налаштувань.
     */
    @Provides
    fun provideBtKasaRepository(
        bluetoothController: BluetoothController, 
        appDatabase: AppDatabase
    ): BtKasaRepository {
        return BtKasaExellioRepository(bluetoothController, appDatabase)
    }
}