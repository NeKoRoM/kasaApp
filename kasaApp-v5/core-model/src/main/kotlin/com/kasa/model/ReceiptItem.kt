package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Товарна позиція у фіскальному розрахунковому документі (чеку).
 * Вартість зберігається виключно в цілих копійках (Long).
 */
@Serializable
data class ReceiptItem(
    val productId: Long,
    val productName: String,
    val priceKopecks: Long,
    val quantity: Double = 1.0,
    val taxGroup: TaxGroup = TaxGroup.GROUP_A,
    val exciseStamp: String? = null,
    val uktzed: String = "",
    val discountPercent: Double = 0.0,
    val isWeighted: Boolean = false,
    val unitOfMeasurement: String = "шт"
) {
    /**
     * Повна вартість позиції без знижки в копійках.
     */
    val rawTotalKopecks: Long
        get() = Math.round(priceKopecks * quantity)

    /**
     * Сума знижки за позицією в копійках.
     */
    val discountKopecks: Long
        get() = if (discountPercent > 0.0) {
            Math.round(rawTotalKopecks * (discountPercent / 100.0))
        } else {
            0L
        }

    /**
     * Фінальна вартість позиції з урахуванням знижки.
     */
    val totalKopecks: Long
        get() = (rawTotalKopecks - discountKopecks).coerceAtLeast(0L)

    /**
     * Ознака, чи потребує ця позиція обов'язкового сканування акцизної марки.
     */
    val requiresExciseStamp: Boolean
        get() = taxGroup.hasExcise || uktzed.isNotBlank() && (
            uktzed.startsWith("2204") ||
            uktzed.startsWith("2205") ||
            uktzed.startsWith("2206") ||
            uktzed.startsWith("2208") ||
            uktzed.startsWith("2402")
        )

    /**
     * Перевірка валідності позиції перед додаванням у фіскальний чек.
     */
    val isValidForSale: Boolean
        get() = priceKopecks > 0L && quantity > 0.0 && (!requiresExciseStamp || !exciseStamp.isNullOrBlank())

    companion object {
        fun fromProduct(
            product: Product,
            quantity: Double = 1.0,
            exciseStamp: String? = null
        ): ReceiptItem {
            val priceInKopecks = Math.round(product.price * 100.0)
            return ReceiptItem(
                productId = product.id,
                productName = product.printableName,
                priceKopecks = priceInKopecks,
                quantity = quantity,
                taxGroup = product.taxGroup,
                exciseStamp = exciseStamp,
                uktzed = product.uktzed,
                isWeighted = product.isWeighted,
                unitOfMeasurement = product.unitOfMeasurement
            )
        }
    }
}
