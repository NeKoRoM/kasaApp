package com.nekorom.kasaapp.data.roomDB.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність облікового запису касира / робочого місця в локальній базі даних Room.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/entity/UserEntity.kt
 *
 * Архітектурна роль:
 * 1. Зберігає налаштування робочої касової зміни конкретного оператора:
 *    - `name`: ПІБ касира (друкується в службових рядках чека).
 *    - `kasaId`: Системний номер фіскального реєстратора.
 *    - `mac`: Bluetooth MAC-адреса закріпленого РРО для швидкого фонового автопідключення.
 *    - `locationAddress`: Адреса торгової точки для чеків та податкових звітів.
 * 2. Синхронізація: поле `firebaseId` зв'язує локального касира з віддаленим бекендом.
 */
@Entity
data class UserEntity(
    /** Локальний числовий ідентифікатор касира в Room */
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,

    /** Заводський або реєстраційний номер каси (РРО) */
    @ColumnInfo
    var kasaId: String = "",

    /** Фізична Bluetooth MAC-адреса касового апарата (наприклад: "00:11:22:33:AA:BB") */
    @ColumnInfo
    var mac: String = "",

    /** Адреса господарської одиниці / магазину */
    @ColumnInfo
    var locationAddress: String = "",

    /** Унікальний ідентифікатор користувача у хмарі (Firebase / FastAPI) */
    val firebaseId: String = "",

    /** ПІБ або псевдонім касира */
    @ColumnInfo(defaultValue = "")
    var name: String = ""
)

