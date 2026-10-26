// =================================================================================================
// МОДУЛЬ ВПРОВАДЖЕННЯ БАЗИ ДАНИХ ТА СХОВИЩ (DAGGER HILT DATA MODULE)
//
// Архітектурний шар: Dependency Injection Layer (Singleton Scope).
// Призначення:
// Забезпечує створення та надання синглтон-екземплярів локальної бази даних Room (AppDatabase),
// прошарку абстракції сховищ (TestStorage) та репозиторіїв рівня даних (TestRepository).
// =================================================================================================

package com.nekorom.kasaapp.di

import android.content.Context
import com.nekorom.kasaapp.data.repository.TestRepositoryImp
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.storage.RoomTestStorage
import com.nekorom.kasaapp.data.storage.TestStorage
import com.nekorom.kasaapp.domain.repository.TestRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль постачання залежностей шару бази даних Room та сховищ.
 */
@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    /**
     * Постачає єдиний екземпляр бази даних Room [AppDatabase] для додатку.
     *
     * @param appContext Контекст застосунку [ApplicationContext].
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    /**
     * Постачає екземпляр сховища [TestStorage] на базі Room DB.
     *
     * @param database Екземпляр бази даних [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideTestStorage(database: AppDatabase): TestStorage {
        return RoomTestStorage(database)
    }

    /**
     * Постачає екземпляр репозиторію тестування [TestRepository].
     *
     * @param testStorage Сховище даних [TestStorage].
     */
    @Provides
    @Singleton
    fun provideTestRepository(testStorage: TestStorage): TestRepository {
        return TestRepositoryImp(testStorage)
    }
}