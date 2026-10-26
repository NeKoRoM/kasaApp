package com.example.exelioshop.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.example.exelioshop.entities.ArticleList

/**
 * [MainDataBase] — локальна база даних Room для зберігання каталогу товарів (артикулів) у ExelioShop.
 * 
 * Характеристики БД:
 * - Сутність: [ArticleList] (таблиця `list_name`)
 * - Версія схеми: 7
 * - Підтримка авто-міграції з версії 6 на 7 ([AutoMigration]) зі специфікацією [SpecMigration]
 *   для безпечного видалення застарілої колонки `code`.
 * - Файл бази даних на диску: `"article_list.db"`.
 */
@Database(
    entities = [ArticleList::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 6, to = 7)]
)
abstract class MainDataBase : RoomDatabase() {

    /**
     * Специфікація міграції: декларативне видалення застарілого стовпця `code` із таблиці `list_name`.
     */
    @DeleteColumn("list_name", "code")
    class SpecMigration : AutoMigrationSpec

    /** Отримання інтерфейсу доступу до даних [Dao] */
    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null

        /**
         * Потокобезпечний метод отримання єдиного екземпляра бази даних (Double-Checked Locking).
         * 
         * @param context контекст застосунку Android
         * @return екземпляр [MainDataBase]
         */
        fun getDatabase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "article_list.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}