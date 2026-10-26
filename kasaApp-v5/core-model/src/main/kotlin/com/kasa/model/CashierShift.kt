package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Канонічна модель касової зміни оператора (касира) в системі РРО.
 * Фіксує час відкриття, накопичувальні обороти за зміну, рух готівки в скриньці та номер підсумкового Z-звіту.
 * Відповідно до Закону України «Про застосування РРО» зміна не може перевищувати 24 години.
 */
@Serializable
data class CashierShift(
    val id: String,
    val shiftNumber: Long,
    val cashierId: Long,
    val cashierName: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    val initialDrawerBalanceKopecks: Long = 0L,
    val totalSalesKopecks: Long = 0L,
    val totalSalesCashKopecks: Long = 0L,
    val totalSalesCardKopecks: Long = 0L,
    val totalReturnsKopecks: Long = 0L,
    val totalReturnsCashKopecks: Long = 0L,
    val totalReturnsCardKopecks: Long = 0L,
    val totalCashInKopecks: Long = 0L,
    val totalCashOutKopecks: Long = 0L,
    val receiptsCount: Int = 0,
    val zReportNumber: String? = null,
    val isClosed: Boolean = false
) {
    val isOpen: Boolean
        get() = !isClosed

    val durationMillis: Long
        get() = (closedAt ?: System.currentTimeMillis()) - openedAt

    val isExpired24h: Boolean
        get() = isOpen && (System.currentTimeMillis() - openedAt) >= TWENTY_FOUR_HOURS_MS

    val formattedDuration: String
        get() {
            val totalMinutes = (durationMillis / (1000L * 60L)).coerceAtLeast(0L)
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            return "${hours}г ${minutes}хв"
        }

    val expectedDrawerCashKopecks: Long
        get() = (initialDrawerBalanceKopecks + totalSalesCashKopecks - totalReturnsCashKopecks + totalCashInKopecks - totalCashOutKopecks)
            .coerceAtLeast(0L)

    companion object {
        const val TWENTY_FOUR_HOURS_MS: Long = 24L * 60L * 60L * 1000L
    }
}
