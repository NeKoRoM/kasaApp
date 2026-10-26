package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Тип фіскального розрахункового документа.
 */
@Serializable
enum class ReceiptType(val code: Int, val title: String) {
    /** Чек продажу товару/послуги */
    SALE(1, "Продаж"),

    /** Чек повернення товару / видатковий чек */
    RETURN(2, "Повернення")
}
