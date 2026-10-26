package com.kasa.feature.pos

import com.kasa.model.HeldReceipt
import com.kasa.model.Receipt
import com.kasa.model.ReceiptType
import java.util.UUID

/**
 * Чиста бізнес-логіка керування паралельними мульти-чеками та відкладеними чеками (MultiCart Domain Logic).
 */
object PosMultiCartLogic {

    fun switchCart(current: PosState, cartIndex: Int): PosState {
        if (cartIndex == current.activeCartIndex || cartIndex !in current.carts.indices) return current

        val updatedCarts = current.carts.toMutableList()
        updatedCarts[current.activeCartIndex] = updatedCarts[current.activeCartIndex].copy(
            items = current.items,
            receiptType = current.receiptType,
            paymentType = current.paymentType,
            receiptDiscountPercent = current.receiptDiscountPercent,
            selectedCustomer = current.selectedCustomer,
            splitCashKopecks = current.splitCashKopecks,
            splitCardKopecks = current.splitCardKopecks
        )

        val targetCart = updatedCarts[cartIndex]

        val stateWithTargetCart = current.copy(
            activeCartIndex = cartIndex,
            carts = updatedCarts,
            receiptType = targetCart.receiptType,
            paymentType = targetCart.paymentType,
            selectedCustomer = targetCart.selectedCustomer,
            splitCashKopecks = targetCart.splitCashKopecks,
            splitCardKopecks = targetCart.splitCardKopecks,
            statusMessage = "Активний ${targetCart.name} (${targetCart.items.size} поз.)"
        )

        return PosCartLogic.updateItemsAndTotals(
            stateWithTargetCart,
            items = targetCart.items,
            activePromptIndex = null,
            receiptDiscountPercent = targetCart.receiptDiscountPercent,
            activeQuantityPromptIndex = null
        )
    }

    fun loadReceiptForReturn(current: PosState, receipt: Receipt): PosState {
        val stateForReturn = current.copy(
            receiptType = ReceiptType.RETURN,
            paymentType = receipt.paymentType,
            isJournalDialogOpen = false,
            statusMessage = "Завантажено чек №${receipt.fiscalCheckNumber ?: "Б/Н"} для оформлення повернення"
        )
        return PosCartLogic.updateItemsAndTotals(
            stateForReturn,
            receipt.items,
            activePromptIndex = null,
            receiptDiscountPercent = receipt.discountPercent
        )
    }

    data class HoldReceiptResult(
        val newState: PosState,
        val updatedHeldList: List<HeldReceipt>
    )

    fun holdCurrentReceipt(current: PosState, note: String, cashierId: Long = 1L): HoldReceiptResult? {
        if (current.items.isEmpty()) return null

        val currentReceipt = Receipt(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            cashierId = cashierId,
            items = current.items,
            paymentType = current.paymentType,
            receiptType = current.receiptType,
            discountPercent = current.receiptDiscountPercent
        )

        val held = HeldReceipt(
            id = UUID.randomUUID().toString(),
            heldAt = System.currentTimeMillis(),
            note = note,
            receipt = currentReceipt
        )

        val updatedHeldList = listOf(held) + current.heldReceipts
        val stateWithHeld = current.copy(
            heldReceipts = updatedHeldList,
            isHoldPromptOpen = false,
            statusMessage = "Чек відкладено (${current.items.size} поз.)"
        )
        val clearedState = PosCartLogic.clearReceipt(stateWithHeld)

        return HoldReceiptResult(
            newState = clearedState,
            updatedHeldList = updatedHeldList
        )
    }

    data class RestoreReceiptResult(
        val newState: PosState,
        val updatedHeldList: List<HeldReceipt>
    )

    fun restoreHeldReceipt(current: PosState, heldReceiptId: String): RestoreReceiptResult? {
        val held = current.heldReceipts.find { it.id == heldReceiptId } ?: return null
        val updatedHeldList = current.heldReceipts.filterNot { it.id == heldReceiptId }

        val stateWithRestored = current.copy(
            heldReceipts = updatedHeldList,
            isHeldReceiptsDialogOpen = false,
            receiptType = held.receipt.receiptType,
            paymentType = held.receipt.paymentType,
            statusMessage = "Відновлено відкладений чек (${held.receipt.items.size} поз.)"
        )

        val finalState = PosCartLogic.updateItemsAndTotals(
            stateWithRestored,
            items = held.receipt.items,
            activePromptIndex = null,
            receiptDiscountPercent = held.receipt.discountPercent
        )

        return RestoreReceiptResult(
            newState = finalState,
            updatedHeldList = updatedHeldList
        )
    }

    data class DeleteReceiptResult(
        val newState: PosState,
        val updatedHeldList: List<HeldReceipt>
    )

    fun deleteHeldReceipt(current: PosState, heldReceiptId: String): DeleteReceiptResult {
        val updatedHeldList = current.heldReceipts.filterNot { it.id == heldReceiptId }
        val stateAfterDelete = current.copy(
            heldReceipts = updatedHeldList,
            statusMessage = "Відкладений чек видалено"
        )
        return DeleteReceiptResult(
            newState = stateAfterDelete,
            updatedHeldList = updatedHeldList
        )
    }
}
