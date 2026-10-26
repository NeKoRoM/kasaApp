package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity

/**
 * Сутність ставки ПДВ (податкової групи) в локальній базі даних Room.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/PdvItem.kt
 *
 * Архітектурна роль:
 * 1. Зберігає конфігурацію податкових груп фіскального реєстратора Exellio (команда `0053`).
 * 2. Розрахунок обов'язкових податків згідно з ПКУ (Податковим кодексом України):
 *    - Основна ставка ПДВ (`pdvNds`): 20%, 7% (медпрепарати), 0% або звільнені від оподаткування.
 *    - Акцизний збір (`pdvExcise`): 5% для роздрібного продажу підакцизних товарів (алкоголь, тютюн).
 * 3. Відображення у випадаючих списках інтерфейсу налаштувань товарів.
 */
@Entity(tableName = "pdv")
data class PdvItem(
    /** Внутрішній первинний ключ SQLite */
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    /** Системний номер податкової групи в РРО Datecs (1..8) */
    @ColumnInfo(name = "pdvId")
    var pdvId: Int = 0,

    /** Назва податкової літери/групи (наприклад: "А", "Б", "В", "Г", "Д") */
    @ColumnInfo(name = "pdvName")
    var pdvName: String = "",

    /** Відсоткова ставка ПДВ (наприклад: "20.00", "7.00", "0.00") */
    @ColumnInfo(name = "pdvNds")
    var pdvNds: String = "",

    /** Відсоткова ставка додаткового акцизного збору (наприклад: "5.00" або "0.00") */
    @ColumnInfo(name = "pdvExcise")
    var pdvExcise: String = "",

    /** Ідентифікатор касира/власника */
    val userId: Int = 0
) {
    /**
     * Форматоване представлення податкової ставки для вибору в інтерфейсі (UI).
     * Якщо акцизний збір відмінний від нуля — додає маркер "+ЗБІР".
     */
    override fun toString(): String {
        return if (pdvExcise != "0.00") {
            "$pdvName \t:\t $pdvNds% +ЗБІР "
        } else {
            "$pdvName \t:\t $pdvNds%"
        }
    }
}

/**
 * Відношення один-до-багатьох: Касир та доступний йому набір податкових ставок.
 */
data class UserWithPdv(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val pdvList: List<PdvItem>
)

/**
 * Міграція схеми Room 4 -> 5: перестворення таблиці `pdv` з додаванням обов'язкового поля `pdvId`.
 */
val MIGRATION_1_2 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `pdv_new` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`pdvId` INTEGER NOT NULL, " +
                    "`pdvName` TEXT NOT NULL, " +
                    "`pdvNds` TEXT NOT NULL, " +
                    "`pdvExcise` TEXT NOT NULL, " +
                    "`userId` INTEGER NOT NULL)"
        )
        database.execSQL(
            "INSERT INTO `pdv_new` (`id`, `pdvId`, `pdvName`, `pdvNds`, `pdvExcise`, `userId`) " +
                    "SELECT `id`, 0, `pdvName`, `pdvNds`, `pdvExcise`, `userId` FROM `pdv`"
        )
        database.execSQL("DROP TABLE `pdv`")
        database.execSQL("ALTER TABLE `pdv_new` RENAME TO `pdv`")
    }
}