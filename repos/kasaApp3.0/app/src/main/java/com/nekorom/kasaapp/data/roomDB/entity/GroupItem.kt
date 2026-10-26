package com.example.exellio20.roomDB.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import java.io.Serializable

/**
 * Сутність товарної групи (категорії) в базі даних Room.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/GroupItem.kt
 *
 * Архітектурна роль:
 * 1. Класифікація товарів у чеку та для звітності РРО (Z-звіт за товарними групами).
 * 2. Ознака акцизного товару (`groupExcise`):
 *    Якщо `groupExcise == true` (алкоголь, тютюн тощо), застосунок вимагає сканування
 *    або введення коду акцизної марки перед додаванням позиції до чека продажу.
 * 3. Прив'язка до користувача/касира (`userId`) для мультитенантності.
 */
@Entity(tableName = "group")
data class GroupItem(
    /** Первинний автоінкрементний ключ групи */
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    /** Назва товарної групи (наприклад: "Напої", "Тютюнові вироби", "Хлібобулочні") */
    @ColumnInfo(name = "groupName", defaultValue = "")
    var groupName: String = "",

    /** Код або цифрове значення товарної групи в РРО */
    @ColumnInfo(name = "groupValue")
    var groupValue: String = "",

    /** Ознака підакцизності (true = вимагати сканування коду акцизної марки) */
    @ColumnInfo(name = "groupExcise", defaultValue = "false")
    var groupExcise: Boolean = false,

    /** Ідентифікатор касира/власника */
    val userId: Int = 0
) : Serializable {

    /**
     * Форматоване представлення групи для відображення у Spinner / випадаючих списках.
     */
    override fun toString(): String {
        return if (!groupExcise) {
            "$groupName #: $groupValue  "
        } else {
            "$groupName #: $groupValue +акциз"
        }
    }
}

/**
 * Відношення один-до-багатьох: Касир та його перелік налаштованих товарних груп.
 */
data class UserWithGroup(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val groupList: List<GroupItem>
)

