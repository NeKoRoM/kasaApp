package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ====================================================================================================
 * СУТНІСТЬ СТАВКИ ПДВ / АКЦИЗНОГО ЗБОРУ (ROOM ENTITY: PDV - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Зберігає фіскальну податкову групу, зчитану з касового апарата Exellio (команда запиту ставок ПДВ 42/83).
 *
 * @property id Унікальний автоінкрементний ідентифікатор рядка в SQLite.
 * @property pdvName Літерний код податкової групи в РРО ('А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж').
 * @property pdvNds Відсоткова ставка ПДВ (наприклад, "20.00", "7.00", "0.00").
 * @property pdvExcise Додаткова ставка акцизного збору (наприклад, "5.00" для роздрібного акцизу).
 */
@Entity(tableName = "pdv")
data class PdvItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "pdvName")
    var pdvName: String = "",
    @ColumnInfo(name = "pdvNds")
    var pdvNds: String = "",
    @ColumnInfo(name = "pdvExcise")
    var pdvExcise: String = "",
) {
    /**
     * Форматує ставку для відображення у Spinner або списках UI:
     * Якщо є акцизний збір — додається помітка "+ЗБІР".
     */
    override fun toString(): String {
        return if (pdvExcise != "0.00") {
            "$pdvName \t:\t $pdvNds% +ЗБІР "
        } else {
            "$pdvName \t:\t $pdvNds%"
        }
    }
}

