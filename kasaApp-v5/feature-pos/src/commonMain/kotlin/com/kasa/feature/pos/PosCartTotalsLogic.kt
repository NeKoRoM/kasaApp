package com.kasa.feature.pos

import com.kasa.model.FiscalMath
import com.kasa.model.PaymentType
import com.kasa.model.ReceiptItem

/**
 * Чиста логіка розрахунку підсумків чека, знижок та заокруглення готівки НБУ №25 (Cart Totals Logic).
 */
object PosCartTotalsLogic {

    fun updateItemsAndTotals(
        current: PosState,
        items: List<ReceiptItem>,
        activePromptIndex: Int? = current.activeExcisePromptIndex,
        receiptDiscountPercent: Double = current.receiptDiscountPercent,
        activeQuantityPromptIndex: Int? = current.activeQuantityPromptIndex
    ): PosState {
        val rawTotal = items.fold(0L) { acc, item -> acc + item.totalKopecks }
        val discountKopecks = if (receiptDiscountPercent > 0.0) {
            Math.round(rawTotal * (receiptDiscountPercent / 100.0))
        } else {
            0L
        }
        val subtotalAfterDiscount = (rawTotal - discountKopecks).coerceAtLeast(0L)

        val payable = when (current.paymentType) {
            PaymentType.CASH -> FiscalMath.roundCashPayment(subtotalAfterDiscount)
            PaymentType.CARD -> subtotalAfterDiscount
            PaymentType.COMBINED -> {
                val cash = current.splitCashKopecks ?: (subtotalAfterDiscount / 2)
                val card = current.splitCardKopecks ?: (subtotalAfterDiscount - cash)
                cash + card
            }
        }

        val updatedCarts = current.carts.toMutableList()
        if (current.activeCartIndex in updatedCarts.indices) {
            updatedCarts[current.activeCartIndex] = updatedCarts[current.activeCartIndex].copy(
                items = items,
                receiptDiscountPercent = receiptDiscountPercent,
                receiptType = current.receiptType,
                paymentType = current.paymentType,
                selectedCustomer = current.selectedCustomer,
                splitCashKopecks = current.splitCashKopecks,
                splitCardKopecks = current.splitCardKopecks
            )
        }

        return current.copy(
            items = items,
            rawTotalKopecks = rawTotal,
            totalPayableKopecks = payable,
            roundingDifferenceKopecks = payable - subtotalAfterDiscount,
            activeExcisePromptIndex = activePromptIndex,
            activeQuantityPromptIndex = activeQuantityPromptIndex,
            receiptDiscountPercent = receiptDiscountPercent,
            carts = updatedCarts
        )
    }

    fun calculateSubtotalAndPayable(
        rawTotalKopecks: Long,
        receiptDiscountPercent: Double,
        paymentType: PaymentType,
        splitCashKopecks: Long?,
        splitCardKopecks: Long?
    ): Pair<Long, Long> {
        val discountKopecks = if (receiptDiscountPercent > 0.0) {
            Math.round(rawTotalKopecks * (receiptDiscountPercent / 100.0))
        } else {
            0L
        }
        val subtotal = (rawTotalKopecks - discountKopecks).coerceAtLeast(0L)
        val payable = when (paymentType) {
            PaymentType.CASH -> FiscalMath.roundCashPayment(subtotal)
            PaymentType.CARD -> subtotal
            PaymentType.COMBINED -> {
                val cash = splitCashKopecks ?: (subtotal / 2)
                val card = splitCardKopecks ?: (subtotal - cash)
                cash + card
            }
        }
        return Pair(subtotal, payable)
    }
}
