package com.example.exellio20.di

import android.content.Context
import androidx.room.Room
import com.example.exellio20.roomDB.MainDataBase
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ====================================================================================================
 * МОДУЛЬ ВПРОВАДЖЕННЯ ЗАЛЕЖНОСТЕЙ БАЗИ ДАНИХ (HILT DB MODULE - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Відповідає за надання екземплярів локальної бази даних Room [MainDataBase] та її DAO:
 * - Створює базу даних `article_list.db`.
 * - Налаштовує [allowMainThreadQueries] для швидких синхронних операцій у протоколі обміну.
 * - [fallbackToDestructiveMigration]: скидає схему при оновленні версій без ручних міграцій.
 * - Надає екземпляр [Dao] та фабричні сутності моделей.
 */
@Module
@InstallIn(SingletonComponent::class)
class DBModule {

    /**
     * Створює та надає єдиний екземпляр бази даних Room на рівні застосунку.
     */
    @Provides
    @Singleton
    fun provide(@ApplicationContext context: Context) = Room.databaseBuilder(
        context, MainDataBase::class.java, "article_list.db"
    )
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build()

    /**
     * Надає DAO для виконання SQL-запитів до товарів, груп, ПДВ та кодів УКТЗЕД.
     */
    @Provides
    @Singleton
    fun provideDao(db: MainDataBase) = db.getDao()

    @Provides
    fun provideArticleEntity() = ArticleItem()

    @Provides
    fun provideUktzEntity() = UktzItem()

    @Provides
    fun providePdvEntity() = PdvItem()

    @Provides
    fun provideGroupEntity() = GroupItem()
}