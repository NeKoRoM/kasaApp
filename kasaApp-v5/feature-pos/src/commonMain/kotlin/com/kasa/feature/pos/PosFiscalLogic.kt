package com.kasa.feature.pos

import com.kasa.model.CashierShift
import com.kasa.model.PaymentType
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.model.ReceiptType
import java.util.UUID

/**
 * Чиста бізнес-логіка фіскальних розрахунків, залишків каси та змін (Fiscal Domain Logic).
 */
object PosFiscalLogic {

    fun ensureShiftOpen(
        existing: CashierShift?,
        cashierId: Long = 1L,
        cashierName: String = "Головний касир",
        currentDrawerBalanceKopecks: Long = 0L
    ): CashierShift {
        if (existing != null && existing.isOpen) {
            return existing
        }
        val nextShiftNumber = (existing?.shiftNumber ?: 0L) + 1L
        return CashierShift(
            id = UUID.randomUUID().toString(),
            shiftNumber = nextShiftNumber,
            cashierId = cashierId,
            cashierName = cashierName,
            openedAt = System.currentTimeMillis(),
            initialDrawerBalanceKopecks = currentDrawerBalanceKopecks
        )
    }

    data class PostReceiptState(
        val newState: PosState,
        val completedReceipt: Receipt,
        val newDrawerBalance: Long,
        val updatedShift: CashierShift?,
        val updatedCatalog: List<Product>
    )

    fun computePostReceiptState(
        current: PosState,
        receipt: Receipt,
        fiscalCheckNumber: String?,
        hardwareInfo: String?
    ): PostReceiptState {
        val fn = hardwareInfo?.takeIf { it.isNotBlank() } ?: "3000123456"
        val completedReceipt = receipt.copy(
            fiscalCheckNumber = fiscalCheckNumber,
            fiscalRegisterNumber = fn
        )

        var newDrawerBalance = current.cashDrawerBalanceKopecks
        val cashDelta = when (completedReceipt.paymentType) {
            PaymentType.CASH -> completedReceipt.totalPayableKopecks
            PaymentType.CARD -> 0L
            PaymentType.COMBINED -> completedReceipt.splitCashKopecks ?: 0L
        }
        if (cashDelta != 0L) {
            val delta = if (completedReceipt.receiptType == ReceiptType.RETURN) {
                -cashDelta
            } else {
                cashDelta
            }
            newDrawerBalance = (newDrawerBalance + delta).coerceAtLeast(0L)
        }

        val isReturn = completedReceipt.receiptType == ReceiptType.RETURN
        val total = completedReceipt.totalPayableKopecks
        val cash = when (completedReceipt.paymentType) {
            PaymentType.CASH -> total
            PaymentType.CARD -> 0L
            PaymentType.COMBINED -> completedReceipt.splitCashKopecks ?: 0L
        }
        val card = when (completedReceipt.paymentType) {
            PaymentType.CASH -> 0L
            PaymentType.CARD -> total
            PaymentType.COMBINED -> completedReceipt.splitCardKopecks ?: 0L
        }

        val updatedShift = current.currentShift?.let { cur ->
            if (isReturn) {
                cur.copy(
                    totalReturnsKopecks = cur.totalReturnsKopecks + total,
                    totalReturnsCashKopecks = cur.totalReturnsCashKopecks + cash,
                    totalReturnsCardKopecks = cur.totalReturnsCardKopecks + card,
                    receiptsCount = cur.receiptsCount + 1
                )
            } else {
                cur.copy(
                    totalSalesKopecks = cur.totalSalesKopecks + total,
                    totalSalesCashKopecks = cur.totalSalesCashKopecks + cash,
                    totalSalesCardKopecks = cur.totalSalesCardKopecks + card,
                    receiptsCount = cur.receiptsCount + 1
                )
            }
        }

        val updatedCatalog = current.catalogProducts.map { prod ->
            val totalItemQty = completedReceipt.items.filter { it.productId == prod.id }.sumOf { it.quantity }
            if (totalItemQty > 0.0) {
                val newRemainder = if (isReturn) {
                    prod.remainder + totalItemQty
                } else {
                    (prod.remainder - totalItemQty).coerceAtLeast(0.0)
                }
                prod.copy(remainder = newRemainder)
            } else {
                prod
            }
        }

        val updatedCarts = current.carts.toMutableList()
        if (current.activeCartIndex in updatedCarts.indices) {
            val idx = current.activeCartIndex
            updatedCarts[idx] = PosCart(id = idx, name = "Чек ${idx + 1}")
        }

        val finalState = current.copy(
            catalogProducts = updatedCatalog,
            items = emptyList(),
            rawTotalKopecks = 0L,
            totalPayableKopecks = 0L,
            roundingDifferenceKopecks = 0L,
            receiptDiscountPercent = 0.0,
            receiptType = ReceiptType.SALE,
            splitCashKopecks = null,
            splitCardKopecks = null,
            selectedCustomer = null,
            receivedCashKopecks = null,
            isFiscalProcessing = false,
            lastFiscalCheckNumber = fiscalCheckNumber,
            currentShift = updatedShift,
            statusMessage = "Чек №${fiscalCheckNumber ?: ""} успішно роздруковано",
            journalReceipts = listOf(completedReceipt) + current.journalReceipts,
            cashDrawerBalanceKopecks = newDrawerBalance,
            carts = updatedCarts
        )

        return PostReceiptState(
            newState = finalState,
            completedReceipt = completedReceipt,
            newDrawerBalance = newDrawerBalance,
            updatedShift = updatedShift,
            updatedCatalog = updatedCatalog
        )
    }

    data class CashInOutState(
        val newState: PosState,
        val newBalance: Long,
        val updatedShift: CashierShift?
    )

    fun computeCashInOutState(
        current: PosState,
        amountKopecks: Long,
        message: String
    ): CashInOutState {
        val newBalance = (current.cashDrawerBalanceKopecks + amountKopecks).coerceAtLeast(0L)
        val updatedShift = current.currentShift?.let { cur ->
            if (amountKopecks > 0L) {
                cur.copy(totalCashInKopecks = cur.totalCashInKopecks + amountKopecks)
            } else {
                cur.copy(totalCashOutKopecks = cur.totalCashOutKopecks + Math.abs(amountKopecks))
            }
        }
        val newState = current.copy(
            cashDrawerBalanceKopecks = newBalance,
            currentShift = updatedShift,
            statusMessage = message
        )
        return CashInOutState(newState, newBalance, updatedShift)
    }

    data class CloseShiftState(
        val newState: PosState,
        val closedShift: CashierShift?
    )

    fun computeCloseShiftState(
        current: PosState,
        zReportNumber: String,
        statusMessage: String
    ): CloseShiftState {
        val closedShift = current.currentShift?.copy(
            closedAt = System.currentTimeMillis(),
            zReportNumber = zReportNumber,
            isClosed = true
        )
        val newState = current.copy(
            currentShift = null,
            lastClosedShift = closedShift,
            isShiftReportDialogOpen = true,
            statusMessage = statusMessage
        )
        return CloseShiftState(newState, closedShift)
    }
}
