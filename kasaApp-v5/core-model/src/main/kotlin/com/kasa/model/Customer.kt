package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Канонічна модель клієнта програми лояльності роздрібної мережі.
 * Містить персональні дані, відсоток постійної знижки та баланс бонусних копійок.
 */
@Serializable
data class Customer(
    val id: Long,
    val name: String,
    val phone: String = "",
    val cardBarcode: String = "",
    val discountPercent: Double = 0.0,
    val bonusBalanceKopecks: Long = 0L
) {
    val hasDiscount: Boolean
        get() = discountPercent > 0.0

    val maskedCardNumber: String
        get() = if (cardBarcode.length > 4) {
            "**** " + cardBarcode.takeLast(4)
        } else {
            cardBarcode
        }
}
