package com.nekorom.kasaapp.domain.models.receipt.entites

import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity

/**
 * Товарна позиція (рядок) у фіскальному чеку продажу.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/domain/models/receipt/entites/ProductForReceipt.kt
 *
 * Архітектурна роль:
 * @property product Знімок сутності товару (найменування, ціна, ПДВ, УКТЗЕД, відділ).
 * @property excise Код обов'язкової акцизної марки для підакцизного товару (алкоголь/тютюн).
 *                  Передається окремим фіскальним рядком або в параметрах фіскальної команди продажу.
 * @property amount Кількість або вага товару (Double: підтримує як цілі штуки, так і дробову масу з ваг, наприклад 1.250 кг).
 * @property id Унікальний ідентифікатор рядка чека (за замовчуванням timestamp створення) для ключів Compose LazyColumn.
 */
data class ProductForReceipt(
    val product: ProductEntity,
    val excise: String,
    val amount: Double,
    val id: Long = System.currentTimeMillis()
)

