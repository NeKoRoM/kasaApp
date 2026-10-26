package com.nekorom.kasaapp.data.roomDB

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.models.receipt.entites.ProductForReceipt
import com.nekorom.kasaapp.domain.models.receipt.room.ReceiptDao
import com.nekorom.kasaapp.domain.models.receipt.room.ReceiptDbEntity

/**
 * Головний клас локальної бази даних Room для застосунку kasaApp3.0.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/AppDatabase.kt
 *
 * Архітектурна роль:
 * 1. Центральний хаб локального збереження даних для забезпечення 100% автономної роботи офлайн.
 * 2. Реєстрація сутностей (Entities):
 *    - `ProductEntity`: каталог товарів РРО.
 *    - `UserEntity`: касири та прив'язані каси (MAC адреси).
 *    - `ProductEnabledEntity`: конфігурація видимості полів картки товару.
 *    - `UktzItem`: довідник кодів УКТЗЕД.
 *    - `PdvItem`: ставки ПДВ та акцизний збір.
 *    - `GroupItem`: товарні групи та ознаки акцизу.
 *    - `ReceiptDbEntity`: збережені чеки (історія продажів).
 *    - `TestEntity`: тестова таблиця.
 * 3. Версіонування: актуальна версія схеми — 10 (підтримка AutoMigration 9 -> 10).
 * 4. Конвертери типів (Converters): забезпечують JSON-серіалізацію списку позицій чека
 *    (`List<ProductForReceipt>`) у єдине текстове поле SQLite за допомогою Gson.
 */
@Database(
    entities = [
        TestEntity::class,
        ProductEntity::class,
        UserEntity::class,
        ProductEnabledEntity::class,
        UktzItem::class,
        PdvItem::class,
        GroupItem::class,
        ReceiptDbEntity::class
    ],
    version = 10,
    autoMigrations = [
        AutoMigration(
            from = 9,
            to = 10
        )
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        /** Singleton екземпляр бази даних з безпечним доступом через double-checked locking */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Отримання екземпляра бази даних `kasa_app.db`.
         * Використовує applicationContext для запобігання витоку пам'яті Activity.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasa_app.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /** DAO для тестових операцій */
    abstract fun testDao(): TestDao

    /** Головний DAO товарів, ставок податків, касирів та груп */
    abstract fun dao(): Dao

    /** Спеціалізований DAO для збереження та вибірки чеків */
    abstract fun receiptDao(): ReceiptDao
}

/**
 * Конвертер складних структур даних для зберігання в SQLite.
 * Перетворює список позицій чека `List<ProductForReceipt>` у JSON-рядок через Gson і навпаки.
 */
class Converters {
    /** Серіалізація списку позицій чека у JSON-рядок для збереження в колонці SQLite */
    @TypeConverter
    fun fromProductWithExciseList(list: List<ProductForReceipt>): String {
        return Gson().toJson(list)
    }

    /** Десеріалізація JSON-рядка з SQLite у типізований список об'єктів ProductForReceipt */
    @TypeConverter
    fun toProductWithExciseList(value: String): List<ProductForReceipt> {
        val listType = object : TypeToken<List<ProductForReceipt>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

