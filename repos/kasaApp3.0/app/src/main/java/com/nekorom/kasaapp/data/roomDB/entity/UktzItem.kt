package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity

/**
 * Сутність коду класифікатора УКТЗЕД (Український класифікатор товарів зовнішньоекономічної діяльності).
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/UktzItem.kt
 *
 * Архітектурна роль:
 * 1. Зберігає довідник 10-значних кодів УКТЗЕД для швидкого призначення товарам.
 * 2. Фіскальна вимога закону про РРО № 265/95-ВР: друк 10-значного коду УКТЗЕД
 *    у кожному рядку фіскального чека є обов'язковим для всіх підакцизних товарів (пальне, алкоголь, тютюн).
 * 3. Передається в поле 16 команди програмування товару РРО (`006;P\t`).
 */
@Entity(tableName = "uktz")
data class UktzItem(
    /** Первинний автоінкрементний ключ SQLite */
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    /** Назва або опис категорії товару за класифікатором (наприклад: "Сигарети з фільтром", "Бензин А-95") */
    @ColumnInfo(name = "pdvName")
    var pdvName: String = "",

    /** Офіційний 10-значний числовий код УКТЗЕД (наприклад: "2402209010", "2203000100") */
    @ColumnInfo(name = "pdvValue")
    var pdvValue: String = "",

    /** Ідентифікатор касира/організації */
    val userId: Int = 0
) {
    /**
     * Форматоване текстове представлення для елементів списків UI.
     */
    override fun toString(): String {
        return "$pdvName \t:\t $pdvValue"
    }
}

/**
 * Відношення один-до-багатьох: Касир та закріплені за ним коди УКТЗЕД.
 */
data class UserWithUktz(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val uktzList: List<UktzItem>
)

