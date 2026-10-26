package com.nekorom.kasaapp.domain.models.receipt.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nekorom.kasaapp.domain.models.receipt.entites.ProductForReceipt
import com.nekorom.kasaapp.domain.models.receipt.entites.Receipt

/**
 * Сутність чека в таблиці `receipt` локальної бази даних Room.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/domain/models/receipt/room/ReceiptDbEntity.kt
 *
 * Архітектурна роль:
 * 1. Зберігає історію сформованих чеків продажу для автономного обліку та аналітики.
 * 2. Поле `products` зберігається як єдиний серіалізований JSON-текст через `Converters`.
 * 3. Контроль статусу фіскалізації: `in_kasa` дозволяє відстежувати чеки, які були надруковані
 *    на касовому апараті, та відрізняти їх від чорнових або перерваних через збій Bluetooth.
 */
@Entity(tableName = "receipt")
data class ReceiptDbEntity(
    /** Унікальний автоінкрементний первинний ключ чека в базі SQLite */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Час створення чека в мілісекундах */
    @ColumnInfo("created_at")
    val createdAt: Long,

    /** Чи був чек успішно видрукуваний та зафіксований у пам'яті фізичного РРО */
    @ColumnInfo("in_kasa")
    val inKasa: Boolean = false,

    /** Ідентифікатор касира, який пробив чек */
    @ColumnInfo("user_id")
    val userId: Int,

    /** Заводський або реєстраційний номер касового апарата */
    @ColumnInfo("kasa_id")
    val kasaId: String,

    /** Список товарних рядків чека (серіалізується конвертером у JSON) */
    val products: List<ProductForReceipt>
) {
    /**
     * Мапінг сутності бази даних у чисту доменну модель `Receipt` для використання в UI та UseCase.
     */
    fun toReceipt(): Receipt = Receipt(
        id = id,
        createdAt = createdAt,
        inKasa = inKasa,
        userId = userId,
        kasaId = kasaId,
        products = products
    )
}

