package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Тип оплати розрахункової операції для реєстратора Exellio / Datecs.
 */
@Serializable
enum class PaymentType(
    val code: Int,
    val title: String
) {
    CASH(0, "Готівка"),
    CARD(1, "Безготівка (банківська картка)"),
    COMBINED(2, "Комбінована (картка + готівка)");

    companion object {
        fun fromCode(code: Int): PaymentType {
            return when (code) {
                0 -> CASH
                1 -> CARD
                2 -> COMBINED
                else -> CASH
            }
        }
    }
}
