package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ====================================================================================================
 * СУТНІСТЬ ДОВІДНИКА КОДІВ УКТЗЕД (ROOM ENTITY: UKTZ - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Зберігає зіставлення назв підакцизних категорій та відповідних числових кодів УКТЗЕД (10 цифр)
 * для автоматичної підстановки в чеки та команду програмування РРО.
 *
 * @property id Унікальний ідентифікатор рядка.
 * @property pdvName Опис товарної групи або категорії підакцизу.
 * @property pdvValue Числовий код товарної підкатегорії УКТЗЕД (наприклад, "2203000100" для пива).
 */
@Entity(tableName = "uktz")
data class UktzItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "pdvName")
    var pdvName: String = "",
    @ColumnInfo(name = "pdvValue")
    var pdvValue: String = "",
) {
    /**
     * Форматує запис коду УКТЗЕД для відображення в списках вибору.
     */
    override fun toString(): String {
        return "$pdvName \t:\t $pdvValue"
    }
}

