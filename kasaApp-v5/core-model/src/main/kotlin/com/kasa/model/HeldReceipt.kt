package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Модель відкладеного касового чека (пауза обслуговування покупця).
 * Дозволяє касиру тимчасово зберегти кошик покупця, обслужити наступних клієнтів
 * та згодом відновити набір товарів без втрати знижок та марок акцизу.
 */
@Serializable
data class HeldReceipt(
    val id: String,
    val heldAt: Long,
    val note: String = "",
    val receipt: Receipt
)
