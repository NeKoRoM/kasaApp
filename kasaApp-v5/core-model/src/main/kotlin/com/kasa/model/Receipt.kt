package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Канонічна модель розрахункового документа (фіскального чека).
 * Всі грошові суми зберігаються в цілих копійках (Long).
 */
@Serializable
data class Receipt(
    val id: String,
    val createdAt: Long,
    val cashierId: Long,
    val items: List<ReceiptItem> = emptyList(),
    val paymentType: PaymentType = PaymentType.CASH,
    val splitCashKopecks: Long? = null,
    val splitCardKopecks: Long? = null,
    val receiptType: ReceiptType = ReceiptType.SALE,
    val isFiscalized: Boolean = false,
    val fiscalCheckNumber: String? = null,
    val fiscalRegisterNumber: String? = null,
    val isSyncedWithServer: Boolean = false,
    val discountPercent: Double = 0.0,
    val customer: Customer? = null
) {
    val customerName: String?
        get() = customer?.name
    /**
     * Точна сума всіх товарних позицій чека без загальної знижки.
     */
    val rawTotalKopecks: Long
        get() = items.fold(0L) { acc, item -> acc + item.totalKopecks }

    /**
     * Сума загальної знижки на весь чек у копійках.
     */
    val receiptDiscountKopecks: Long
        get() = if (discountPercent > 0.0) {
            Math.round(rawTotalKopecks * (discountPercent / 100.0))
        } else {
            0L
        }

    /**
     * Сума чека після застосування загальної знижки.
     */
    val subtotalAfterDiscountKopecks: Long
        get() = (rawTotalKopecks - receiptDiscountKopecks).coerceAtLeast(0L)

    /**
     * Фінальна сума до сплати з урахуванням правил округлення готівки НБУ.
     * Для безготівкової оплати сума збігається з [subtotalAfterDiscountKopecks].
     */
    val totalPayableKopecks: Long
        get() = when (paymentType) {
            PaymentType.CASH -> FiscalMath.roundCashPayment(subtotalAfterDiscountKopecks)
            PaymentType.CARD -> subtotalAfterDiscountKopecks
            PaymentType.COMBINED -> {
                val cash = splitCashKopecks ?: 0L
                val card = splitCardKopecks ?: (subtotalAfterDiscountKopecks - cash).coerceAtLeast(0L)
                cash + card
            }
        }

    /**
     * Різниця між заокругленою сумою до сплати та реальною сумою товарів (заокруглення за чеком).
     */
    val cashRoundingDifferenceKopecks: Long
        get() = totalPayableKopecks - subtotalAfterDiscountKopecks

    /**
     * Загальна сума акцизного збору 5% за всіма підакцизними позиціями чека.
     */
    val totalExciseKopecks: Long
        get() = items
            .filter { it.taxGroup.hasExcise }
            .fold(0L) { acc, item -> acc + FiscalMath.calculateExciseKopecks(item.totalKopecks) }

    /**
     * Загальна сума ПДВ за чеком.
     */
    val totalVatKopecks: Long
        get() = items.fold(0L) { acc, item ->
            acc + FiscalMath.calculateTaxKopecks(item.totalKopecks, item.taxGroup)
        }

    /**
     * Чи містить чек підакцизні товари без введеної акцизної марки.
     */
    val hasUnstampedExciseItems: Boolean
        get() = items.any { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }

    /**
     * Чи готовий чек до відправки на фізичну касу для друку та фіскалізації.
     */
    val canBeFiscalized: Boolean
        get() = items.isNotEmpty() && !hasUnstampedExciseItems && rawTotalKopecks > 0L && !isFiscalized
}
