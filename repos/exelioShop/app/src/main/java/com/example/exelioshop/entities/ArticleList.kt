package com.example.exelioshop.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * [ArticleList] — сутність Room для збереження товарної позиції (PLU/артикулу) у таблиці `list_name`.
 * 
 * Структура полів відповідає апаратному протоколу Datecs/Exellio (команди 0x44/0x6B програмування артикулів):
 * @property id внутрішній унікальний ідентифікатор запису / код PLU в касі
 * @property name назва товару (друкується в чеку, ліміт символів)
 * @property price ціна за одиницю товару
 * @property TAX податкова ставка ПДВ (символ групи А, Б, В, Г або індекс 1..8)
 * @property section відділ / секція в магазині (1..99)
 * @property group товарна група
 * @property type тип ціни (0 — програмована, 1 — вільна, 2 — максимальна)
 * @property salesAmount накопичена сума продажів за зміну
 * @property salesCount накопичена кількість проданих одиниць
 * @property remainder поточний залишок товару на складі
 * @property barCode1 основний штрихкод (EAN-13 тощо)
 * @property barCode2 додатковий штрихкод 2
 * @property barCode3 додатковий штрихкод 3
 * @property barCode4 додатковий штрихкод 4
 * @property productInSet ознака штучного/вагового товару
 * @property unit текстова одиниця вимірювання (шт, кг, л)
 * @property UKTZED код товарної підкатегорії згідно з УКТЗЕД (обов'язковий для підакцизних товарів)
 */
@Entity(tableName = "list_name")
data class ArticleList(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "price")
    val price: String,
    @ColumnInfo(name = "TAX", defaultValue = "")
    val TAX: String,
    @ColumnInfo(name = "section", defaultValue = "")
    val section: String,
    @ColumnInfo(name = "group", defaultValue = "")
    val group: String,
    @ColumnInfo(name = "type", defaultValue = "")
    val type: String,
    @ColumnInfo(name = "salesAmount", defaultValue = "")
    val salesAmount: String,
    @ColumnInfo(name = "salesCount", defaultValue = "")
    val salesCount: String,
    @ColumnInfo(name = "remainder", defaultValue = "")
    val remainder: String,
    @ColumnInfo(name = "barCode1", defaultValue = "")
    val barCode1: String,
    @ColumnInfo(name = "barCode2", defaultValue = "")
    val barCode2: String,
    @ColumnInfo(name = "barCode3", defaultValue = "")
    val barCode3: String,
    @ColumnInfo(name = "barCode4", defaultValue = "")
    val barCode4: String,
    @ColumnInfo(name = "productInSet", defaultValue = "")
    val productInSet: String,
    @ColumnInfo(name = "unit", defaultValue = "")
    var unit: String,
    @ColumnInfo(name = "UKTZED", defaultValue = "")
    var UKTZED: String,
) : Serializable
