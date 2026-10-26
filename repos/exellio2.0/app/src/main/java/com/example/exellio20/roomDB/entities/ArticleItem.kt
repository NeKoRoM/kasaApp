package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * ====================================================================================================
 * СУТНІСТЬ ТОВАРУ/АРТИКУЛУ В ЛОКАЛЬНІЙ БАЗІ ДАНИХ ROOM (ARTICLE ITEM - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Представляє фіскальний товар (артикул/PLU) у таблиці `article` бази даних `article_list.db`.
 * Відповідає структурі полів товару касових апаратів Exellio/Datecs.
 *
 * @property id Номер запису / PLU товару в РРО (PrimaryKey).
 * @property TAX Податкова група ('А'..'Ж').
 * @property section Номер секції/відділу в РРО (1..99).
 * @property group Код товарної групи.
 * @property type Ознака типу товару (0 - звичайний).
 * @property price Ціна товару з урахуванням податків.
 * @property salesAmount Сума продажів товару в касовому апараті.
 * @property salesCount Кількість проданих одиниць.
 * @property remainder Поточний обліковий залишок товару.
 * @property barCode1 Основний штрихкод (EAN-13, UPC тощо).
 * @property barCode2 Додатковий штрихкод 2.
 * @property barCode3 Додатковий штрихкод 3.
 * @property barCode4 Додатковий штрихкод 4.
 * @property name Фіскальне найменування товару.
 * @property weightOrUnit Ознака вагового товару або одиниця виміру (поле 15).
 * @property UKTZED Код товарної підкатегорії УКТЗЕД для підакцизних товарів.
 * @property unitOfMeasurement Скорочена назва одиниці виміру ("шт", "кг", "л").
 */
@Entity(tableName = "article")
data class ArticleItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "TAX")
    var TAX: String = "",
    @ColumnInfo(name = "section")
    var section: String = "",
    @ColumnInfo(name = "group")
    var group: String = "",
    @ColumnInfo(name = "type")
    var type: String = "",
    @ColumnInfo(name = "price")
    var price: String = "",
    @ColumnInfo(name = "salesAmount")
    var salesAmount: String = "",
    @ColumnInfo(name = "salesCount")
    var salesCount: String = "",
    @ColumnInfo(name = "remainder")
    var remainder: String = "",
    @ColumnInfo(name = "barCode1")
    var barCode1: String = "",
    @ColumnInfo(name = "barCode2")
    var barCode2: String = "",
    @ColumnInfo(name = "barCode3")
    var barCode3: String = "",
    @ColumnInfo(name = "barCode4")
    var barCode4: String = "",
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "weightOrUnit")
    var weightOrUnit: String = "",
    @ColumnInfo(name = "UKTZED")
    var UKTZED: String = "",
    @ColumnInfo(name = "unitOfMeasurement")
    var unitOfMeasurement: String = "",
) : Serializable

