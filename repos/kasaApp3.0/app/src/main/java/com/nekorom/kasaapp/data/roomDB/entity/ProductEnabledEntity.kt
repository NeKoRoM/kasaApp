package com.nekorom.kasaapp.data.roomDB.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.Serializable

/**
 * Сутність конфігурації видимості полів товару в інтерфейсі касира (SettingsVisibleScreen).
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/ProductEnabledEntity.kt
 *
 * Архітектурна роль:
 * 1. UX-конфігурація POS: дозволяє адміністратору точки гнучко вмикати/вимикати відображення
 *    будь-якого з 17 полів товару на екрані редагування товару (`EditProductScreen`).
 * 2. Спрощення інтерфейсу для касирів:
 *    - Якщо магазин не використовує додаткові штрих-коди — `barCode2`, `barCode3`, `barCode4` вимикаються.
 *    - Якщо товари не мають акцизу — поле `UKTZED` можна приховати, щоб запобігти помилкам персоналу.
 */
@Entity
data class ProductEnabledEntity(
    /** Первинний автоінкрементний ключ налаштування */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(defaultValue = "0")
    var id: Int = 0,

    /** Ідентифікатор товару або набору налаштувань */
    @ColumnInfo(name = "productId")
    var productId: String = "",

    /** Видимість поля вибору податкової ставки ПДВ (TAX) */
    @ColumnInfo(name = "TAX")
    var TAX: Boolean = true,

    /** Видимість номера відділу (section) */
    @ColumnInfo(name = "section")
    var section: Boolean = true,

    /** Видимість вибору товарної групи (group) */
    @ColumnInfo(name = "group")
    var group: Boolean = true,

    /** Видимість типу ціни (type) */
    @ColumnInfo(name = "type")
    var type: Boolean = true,

    /** Видимість роздрібної ціни (price) */
    @ColumnInfo(name = "price")
    var price: Boolean = true,

    /** Видимість суми накопичених продажів (salesAmount) */
    @ColumnInfo(name = "salesAmount")
    var salesAmount: Boolean = true,

    /** Видимість кількості продажів (salesCount) */
    @ColumnInfo(name = "salesCount")
    var salesCount: Boolean = true,

    /** Видимість обліку залишку товару (remainder) */
    @ColumnInfo(name = "remainder")
    var remainder: Boolean = true,

    /** Видимість основного штрихкоду (barCode1) */
    @ColumnInfo(name = "barCode1")
    var barCode1: Boolean = true,

    /** Видимість додаткового штрихкоду 2 (barCode2) */
    @ColumnInfo(name = "barCode2")
    var barCode2: Boolean = true,

    /** Видимість додаткового штрихкоду 3 (barCode3) */
    @ColumnInfo(name = "barCode3")
    var barCode3: Boolean = true,

    /** Видимість додаткового штрихкоду 4 (barCode4) */
    @ColumnInfo(name = "barCode4")
    var barCode4: Boolean = true,

    /** Видимість найменування товару (name) */
    @ColumnInfo(name = "name")
    var name: Boolean = true,

    /** Видимість перемикача штучний/ваговий (weightOrUnit) */
    @ColumnInfo(name = "weightOrUnit")
    var weightOrUnit: Boolean = true,

    /** Видимість поля коду УКТЗЕД (UKTZED) */
    @ColumnInfo(name = "UKTZED")
    var UKTZED: Boolean = true,

    /** Видимість вибору одиниці виміру (unitOfMeasurement) */
    @ColumnInfo(name = "unitOfMeasurement")
    var unitOfMeasurement: Boolean = true
) : Serializable

/**
 * Міграція схеми Room 3 -> 4: реструктуризація таблиці ProductEnabledEntity
 * для додавання сурогатного автоінкрементного ключа `id`.
 */
val MIGRATION_4_5 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ProductEnabledEntity ADD COLUMN id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE ProductEnabledEntity RENAME COLUMN productId TO old_productId")
        database.execSQL("UPDATE ProductEnabledEntity SET id = old_productId")
        database.execSQL("ALTER TABLE ProductEnabledEntity DROP COLUMN old_productId")
    }
}