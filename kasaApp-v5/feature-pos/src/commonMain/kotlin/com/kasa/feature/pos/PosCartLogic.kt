package com.kasa.feature.pos

import com.kasa.model.Customer
import com.kasa.model.PaymentType
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * Чиста бізнес-логіка операцій над кошиком чека (Cart Domain Logic).
 * Відповідає за додавання, зміну кількості, знижки та марки акцизу.
 */
object PosCartLogic {

    data class AddProductResult(
        val newState: PosState,
        val displayLine1: String,
        val displayLine2: String
    )

    fun updateItemsAndTotals(
        current: PosState,
        items: List<ReceiptItem>,
        activePromptIndex: Int? = current.activeExcisePromptIndex,
        receiptDiscountPercent: Double = current.receiptDiscountPercent,
        activeQuantityPromptIndex: Int? = current.activeQuantityPromptIndex
    ): PosState = PosCartTotalsLogic.updateItemsAndTotals(
        current, items, activePromptIndex, receiptDiscountPercent, activeQuantityPromptIndex
    )

    fun addProduct(current: PosState, product: Product, customQuantity: Double?): AddProductResult {
        val qty = when {
            customQuantity != null && customQuantity > 0.0 -> customQuantity
            product.isWeighted && current.scaleWeight?.isStable == true -> current.scaleWeight.weightKilograms
            else -> 1.0
        }

        if (product.requiresExciseStamp && qty > 1.0 && qty % 1.0 == 0.0) {
            val count = qty.toInt()
            var currentItems = current.items
            for (i in 1..count) {
                currentItems = currentItems + ReceiptItem.fromProduct(product = product, quantity = 1.0)
            }
            val promptIndex = currentItems.size - count
            val stateAfterAdd = updateItemsAndTotals(current, items = currentItems, activePromptIndex = promptIndex)
            val priceFormatted = PosCurrencyFormatter.formatKopecks(Math.round(product.price * 100.0) * count)
            return AddProductResult(
                newState = stateAfterAdd,
                displayLine1 = product.name.take(20),
                displayLine2 = "${count}x${product.price} = $priceFormatted"
            )
        }

        val newItem = ReceiptItem.fromProduct(product = product, quantity = qty)
        val updatedItems = current.items + newItem
        val newIndex = updatedItems.size - 1

        val promptIndex = if (product.requiresExciseStamp) newIndex else current.activeExcisePromptIndex
        val pricePromptIndex = if (product.price <= 0.0) newIndex else current.activePricePromptIndex
        val weightPromptIndex = if (product.isWeighted && current.scaleWeight?.isStable != true && customQuantity == null) {
            newIndex
        } else {
            current.activeQuantityPromptIndex
        }

        var stateAfterAdd = updateItemsAndTotals(
            current,
            items = updatedItems,
            activePromptIndex = promptIndex,
            activeQuantityPromptIndex = weightPromptIndex
        )
        if (pricePromptIndex != null) {
            stateAfterAdd = stateAfterAdd.copy(activePricePromptIndex = pricePromptIndex)
        }
        val priceFormatted = PosCurrencyFormatter.formatKopecks(newItem.totalKopecks)
        return AddProductResult(
            newState = stateAfterAdd,
            displayLine1 = product.name.take(20),
            displayLine2 = "${newItem.quantity}x${product.price} = $priceFormatted"
        )
    }

    fun updateQuantity(current: PosState, itemIndex: Int, quantity: Double): PosState {
        if (quantity <= 0.0) {
            return removeItem(current, itemIndex)
        }
        val currentItems = current.items.toMutableList()
        if (itemIndex !in currentItems.indices) return current

        currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = quantity)
        return updateItemsAndTotals(current, currentItems)
    }

    fun removeItem(current: PosState, itemIndex: Int): PosState {
        val currentItems = current.items.toMutableList()
        if (itemIndex !in currentItems.indices) return current

        currentItems.removeAt(itemIndex)
        return updateItemsAndTotals(current, currentItems)
    }

    fun setPaymentType(current: PosState, paymentType: PaymentType): PosState {
        val (subtotal, payable) = PosCartTotalsLogic.calculateSubtotalAndPayable(
            rawTotalKopecks = current.rawTotalKopecks,
            receiptDiscountPercent = current.receiptDiscountPercent,
            paymentType = paymentType,
            splitCashKopecks = current.splitCashKopecks,
            splitCardKopecks = current.splitCardKopecks
        )
        return current.copy(
            paymentType = paymentType,
            splitCashKopecks = if (paymentType == PaymentType.COMBINED) current.splitCashKopecks else null,
            splitCardKopecks = if (paymentType == PaymentType.COMBINED) current.splitCardKopecks else null,
            totalPayableKopecks = payable,
            roundingDifferenceKopecks = payable - subtotal
        )
    }

    fun setSplitPayment(current: PosState, cashKopecks: Long, cardKopecks: Long): PosState {
        val (subtotal, _) = PosCartTotalsLogic.calculateSubtotalAndPayable(
            rawTotalKopecks = current.rawTotalKopecks,
            receiptDiscountPercent = current.receiptDiscountPercent,
            paymentType = PaymentType.COMBINED,
            splitCashKopecks = cashKopecks,
            splitCardKopecks = cardKopecks
        )
        val total = cashKopecks + cardKopecks
        return current.copy(
            paymentType = PaymentType.COMBINED,
            splitCashKopecks = cashKopecks,
            splitCardKopecks = cardKopecks,
            isSplitPaymentDialogOpen = false,
            totalPayableKopecks = total,
            roundingDifferenceKopecks = total - subtotal,
            statusMessage = "Змішана оплата: Готівка ${cashKopecks / 100.0} грн, Картка ${cardKopecks / 100.0} грн"
        )
    }

    fun enterExciseStamp(current: PosState, itemIndex: Int, stamp: String): PosState {
        val currentItems = current.items.toMutableList()
        if (itemIndex !in currentItems.indices) return current

        val cleanStamp = stamp.trim()
        val duplicateItem = currentItems.find {
            it.exciseStamp.equals(cleanStamp, ignoreCase = true) && currentItems.indexOf(it) != itemIndex
        }
        if (cleanStamp.isNotBlank() && duplicateItem != null) {
            return current.copy(
                statusMessage = "УВАГА: Марка '$cleanStamp' вже додана до позиції '${duplicateItem.productName}'!"
            )
        }

        currentItems[itemIndex] = currentItems[itemIndex].copy(exciseStamp = cleanStamp)
        val nextUnstamped = currentItems.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
        val nextPrompt = if (nextUnstamped != -1) nextUnstamped else null
        return updateItemsAndTotals(current, currentItems, activePromptIndex = nextPrompt)
    }

    fun updateItemPrice(current: PosState, itemIndex: Int, priceKopecks: Long): PosState {
        val currentItems = current.items.toMutableList()
        if (itemIndex !in currentItems.indices || priceKopecks <= 0L) return current

        val item = currentItems[itemIndex]
        currentItems[itemIndex] = item.copy(priceKopecks = priceKopecks)
        return updateItemsAndTotals(current, currentItems)
    }

    fun setItemDiscount(current: PosState, itemIndex: Int, discountPercent: Double): PosState {
        val currentItems = current.items.toMutableList()
        val item = currentItems.getOrNull(itemIndex) ?: return current
        currentItems[itemIndex] = item.copy(discountPercent = discountPercent)
        val stateWithoutPrompt = current.copy(activeDiscountPromptItemIndex = null)
        return updateItemsAndTotals(stateWithoutPrompt, currentItems)
    }

    fun setReceiptDiscount(current: PosState, discountPercent: Double): PosState {
        val stateWithDiscount = current.copy(receiptDiscountPercent = discountPercent, isReceiptDiscountPromptOpen = false)
        return updateItemsAndTotals(stateWithDiscount, current.items, receiptDiscountPercent = discountPercent)
    }

    fun attachCustomer(current: PosState, customer: Customer): PosState {
        val stateWithCustomer = current.copy(
            selectedCustomer = customer,
            isLoyaltyDialogOpen = false,
            statusMessage = "Клієнт: ${customer.name} (Знижка ${customer.discountPercent}%)"
        )
        return if (customer.discountPercent > 0.0) {
            setReceiptDiscount(stateWithCustomer, customer.discountPercent)
        } else {
            stateWithCustomer
        }
    }

    fun detachCustomer(current: PosState): PosState {
        val stateWithoutCustomer = current.copy(
            selectedCustomer = null,
            isLoyaltyDialogOpen = false,
            statusMessage = "Клієнта відкріплено"
        )
        return setReceiptDiscount(stateWithoutCustomer, 0.0)
    }

    fun clearReceipt(current: PosState): PosState {
        val updatedCarts = current.carts.toMutableList()
        if (current.activeCartIndex in updatedCarts.indices) {
            val idx = current.activeCartIndex
            updatedCarts[idx] = PosCart(id = idx, name = "Чек ${idx + 1}")
        }
        val cleared = current.copy(
            receiptDiscountPercent = 0.0,
            receiptType = com.kasa.model.ReceiptType.SALE,
            splitCashKopecks = null,
            splitCardKopecks = null,
            selectedCustomer = null,
            receivedCashKopecks = null,
            carts = updatedCarts
        )
        return updateItemsAndTotals(
            cleared,
            emptyList(),
            activePromptIndex = null,
            receiptDiscountPercent = 0.0,
            activeQuantityPromptIndex = null
        )
    }
}
