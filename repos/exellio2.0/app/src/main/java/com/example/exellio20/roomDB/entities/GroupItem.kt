package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ====================================================================================================
 * СУТНІСТЬ ТОВАРНОЇ ГРУПИ (ROOM ENTITY: GROUP - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Зберігає налаштування товарної групи в касовому апараті Exellio.
 *
 * @property id Унікальний ідентифікатор рядка в SQLite.
 * @property groupName Назва товарної групи (наприклад, "Продукти", "Алкоголь", "Тютюн").
 * @property groupValue Числовий номер або код групи в пам'яті РРО (1..99).
 * @property groupExcise Ознака обов'язкового акцизного маркування для товарів цієї групи.
 */
@Entity(tableName = "group")
data class GroupItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "groupName")
    var groupName: String = "",
    @ColumnInfo(name = "groupValue")
    var groupValue: String = "",
    @ColumnInfo(name = "groupExcise")
    var groupExcise: Boolean = false,
) {
    /**
     * Форматує назву групи для Spinner або випадаючого списку.
     */
    override fun toString(): String {
        return if (!groupExcise) {
            "$groupName #: $groupValue  "
        } else {
            "$groupName #: $groupValue +акциз"
        }
    }
}

