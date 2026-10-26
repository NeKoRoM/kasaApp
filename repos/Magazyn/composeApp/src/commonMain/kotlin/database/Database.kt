package database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Мультиплатформна база даних Room (KMP RoomDatabase) для додатку Magazyn.
 *
 * Об'єднує сутності:
 * - [Person]: Персонал та касири.
 * - [Product]: Складські залишки товарів.
 * - [Department]: Товарні відділи та склади.
 *
 * Працює на Android (SQLite) та Desktop JVM (SQLite через Xerial driver).
 */
@Database(
    entities = [Person::class, Product::class, Department::class],
    version = 1
)
abstract class Database : RoomDatabase() {
    /** Повертає екземпляр інтерфейсу доступу до даних [Dao] */
    abstract fun dao(): Dao
}
