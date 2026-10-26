package com.nekorom.kasaapp.data.roomDB.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.Serializable

/**
 * Сутність товару в локальній базі даних Room.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/ProductEntity.kt
 *
 * Архітектурна роль:
 * 1. Локальний кеш номенклатури товарів для автономної роботи касира без постійного доступу до інтернету.
 * 2. 100% відповідність структурі пам'яті артикулів РРО Datecs / Exellio (команда `006;P\t`):
 *    Кожне поле (1..17) відповідає окремому позиційному параметру у фіскальному протоколі реєстратора.
 * 3. Ізоляція даних за касиром/організацією (Multi-tenancy):
 *    Композитний унікальний індекс [productId, userId] гарантує унікальність PLU в межах одного користувача.
 */
@Entity(
    indices = [
        Index(
            value = ["productId", "userId"],
            unique = true
        )
    ]
)
data class ProductEntity(
    /** Внутрішній первинний ключ SQLite */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(defaultValue = "0")
    var id: Int = 0,

    /** [Поле 1 Datecs] Код товару (PLU / Артикул у пам'яті каси від 1 до 100000) */
    @ColumnInfo(name = "productId")
    var productId: Int = 0,

    /** [Поле 2 Datecs] Податкова група ПДВ (літера: 'А', 'Б', 'В', 'Г', 'Д') */
    @ColumnInfo(name = "TAX")
    var TAX: String = "",

    /** [Поле 3 Datecs] Номер відділу (секції) торговельного залу (1..99) */
    @ColumnInfo(name = "section")
    var section: String = "",

    /** [Поле 4 Datecs] Товарна група (категорія) для звітів */
    @ColumnInfo(name = "group")
    var group: String = "",

    /** [Поле 5 Datecs] Тип ціни (0 = фіксована ціна, 1 = вільна ціна, що вводиться касиром) */
    @ColumnInfo(name = "type")
    var type: String = "",

    /** [Поле 6 Datecs] Роздрібна ціна за одиницю товару (формат: "0.00") */
    @ColumnInfo(name = "price")
    var price: String = "",

    /** [Поле 7 Datecs] Накопичена сума продажів товару в касі */
    @ColumnInfo(name = "salesAmount")
    var salesAmount: String = "",

    /** [Поле 8 Datecs] Накопичена кількість проданих одиниць у касі */
    @ColumnInfo(name = "salesCount")
    var salesCount: String = "",

    /** [Поле 9 Datecs] Поточний залишок товару на складі/точці */
    @ColumnInfo(name = "remainder")
    var remainder: String = "",

    /** [Поле 10 Datecs] Основний штрихкод (EAN-13 / EAN-8) */
    @ColumnInfo(name = "barCode1")
    var barCode1: String = "",

    /** [Поле 11 Datecs] Додатковий штрихкод 2 */
    @ColumnInfo(name = "barCode2")
    var barCode2: String = "",

    /** [Поле 12 Datecs] Додатковий штрихкод 3 */
    @ColumnInfo(name = "barCode3")
    var barCode3: String = "",

    /** [Поле 13 Datecs] Додатковий штрихкод 4 */
    @ColumnInfo(name = "barCode4")
    var barCode4: String = "",

    /** [Поле 14 Datecs] Найменування товару (друкується в чеку, до 40 символів у Windows-1251) */
    @ColumnInfo(name = "name")
    var name: String = "",

    /** [Поле 15 Datecs] Ознака вагового товару: "0" = штучний, "1" = ваговий (вимагає запиту ваги з ваг) */
    @ColumnInfo(name = "weightOrUnit")
    var weightOrUnit: String = "",

    /** [Поле 16 Datecs] 10-значний код УКТЗЕД (обов'язковий для підакцизних товарів в Україні) */
    @ColumnInfo(name = "UKTZED")
    var UKTZED: String = "",

    /** [Поле 17 Datecs] Код одиниці виміру (0=ШТ, 1=КГ, 2=М, 3=Л, 4=Гр) */
    @ColumnInfo(name = "unitOfMeasurement")
    var unitOfMeasurement: String = "",

    /** Прапорець автономності товару (незалежний від хмарного каталогу) */
    @ColumnInfo(name = "isIndependent", defaultValue = "false")
    var isIndependent: Boolean = false,

    /** Ідентифікатор касира/власника для мультитенантного розділення БД */
    val userId: Int = 0
) : Serializable {

    /**
     * Конвертує цифровий код одиниці виміру Datecs у скорочену українську назву для UI.
     */
    fun toUnitOfMeasurementShortName(): String {
        return when (unitOfMeasurement) {
            "0" -> "ШТ"
            "1" -> "КГ"
            "2" -> "М"
            "3" -> "Л"
            "4" -> "Гр"
            else -> "помилка"
        }
    }
}

/**
 * Відношення один-до-багатьох: Користувач (касир) та його персональний каталог товарів.
 */
data class UserWithProducts(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "productId"
    )
    val productList: List<ProductEntity>
)

/**
 * Міграція схеми Room 3 -> 4: додавання автоінкрементного первинного ключа `id`
 * та оновлення структури таблиці ProductEnabledEntity.
 */
val MIGRATION_6_7 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ProductEnabledEntity ADD COLUMN id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE ProductEnabledEntity RENAME COLUMN productId TO old_productId")
        database.execSQL("UPDATE ProductEnabledEntity SET id = old_productId")
        database.execSQL("ALTER TABLE ProductEnabledEntity DROP COLUMN old_productId")
    }
}