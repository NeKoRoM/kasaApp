package com.nekorom.kasaapp.domain.models.receipt.entites

import com.nekorom.kasaapp.domain.models.receipt.room.ReceiptDbEntity
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Чиста доменна модель розрахункового документа (фіскального чека продажу).
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/domain/models/receipt/entites/Receipt.kt
 *
 * Архітектурна роль:
 * 1. Центральний агрегат бізнес-логіки продажу в інтерфейсі касира (POS Tablet UI).
 * 2. Акумулює список позицій `products: List<ProductForReceipt>` з їх кількістю, ціною та акцизними марками.
 * 3. Контроль фіскалізації (`inKasa`):
 *    - `false`: чек сформовано в UI / базі, але ще не фіскалізовано на апаратному РРО.
 *    - `true`: чек успішно видрукувано на РРО Datecs/Exellio та зафіксовано у фіскальній пам'яті.
 * 4. Забезпечує підрахунок загальної вартості чека (`getPrice()`) та конвертацію в сутність БД (`toReceiptDbEntity()`).
 */
data class Receipt(
    /** Унікальний числовий ідентифікатор чека */
    val id: Int,

    /** Час створення чека в мілісекундах (Unix Timestamp) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Прапорець фіскалізації (чи видрукувано на фізичному РРО Datecs) */
    val inKasa: Boolean = false,

    /** Ідентифікатор касира, що сформував чек */
    val userId: Int,

    /** Заводський або реєстраційний номер каси */
    val kasaId: String,

    /** Список товарних позицій чека */
    val products: List<ProductForReceipt>
) {
    /**
     * Розрахунок загальної вартості чека (Total Sum).
     * Сумує добуток ціни товару на його кількість/вагу для кожного рядка: `price * amount`.
     */
    fun getPrice(): Double {
        var price = 0.0
        products.forEach {
            price += it.product.price.toDouble() * it.amount
        }
        return price
    }

    /**
     * Форматування дати та часу створення чека згідно з локаллю пристрою.
     */
    fun getDate(): String {
        val formatter = SimpleDateFormat.getDateTimeInstance()
        return formatter.format(Date(createdAt))
    }

    /**
     * Отримання дати чека для групування у списках денної виручки.
     */
    fun getDayDate(): String {
        val formatter = SimpleDateFormat.getDateTimeInstance()
        return formatter.format(Date(createdAt))
    }

    /**
     * Перетворення доменної моделі в сутність Room `ReceiptDbEntity` для збереження в SQLite.
     */
    fun toReceiptDbEntity(): ReceiptDbEntity {
        return ReceiptDbEntity(
            id = id,
            createdAt = createdAt,
            inKasa = inKasa,
            userId = userId,
            kasaId = kasaId,
            products = products
        )
    }
}