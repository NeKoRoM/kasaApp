package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Канонічна модель товару/послуги для роздрібної торгівлі та програмування в РРО.
 */
@Serializable
data class Product(
    val id: Long,
    val name: String,
    val price: Double,
    val taxGroup: TaxGroup = TaxGroup.GROUP_A,
    val section: Int = 1,
    val group: Int = 1,
    val barcodes: List<String> = emptyList(),
    val isWeighted: Boolean = false,
    val uktzed: String = "",
    val unitOfMeasurement: String = if (isWeighted) "кг" else "шт",
    val remainder: Double = 0.0,
    val description: String = "",
    val costPrice: Double = 0.0,
    val minRemainder: Double = 0.0,
    val isFavorite: Boolean = false,
    val categoryName: String = ""
) {
    /**
     * Повертає коротку назву товару (до 40 символів) для друку на чековій стрічці касового апарата.
     */
    val printableName: String
        get() = if (name.length > 40) name.substring(0, 40) else name

    /**
     * Ознака необхідності перевірки та введення акцизної марки.
     */
    val requiresExciseStamp: Boolean
        get() = taxGroup.hasExcise || uktzed.isNotBlank() && (
            uktzed.startsWith("2204") || // Вина
            uktzed.startsWith("2205") || // Вермути
            uktzed.startsWith("2206") || // Сидр
            uktzed.startsWith("2208") || // Міцний алкоголь
            uktzed.startsWith("2402")    // Тютюнові вироби
        )

    /**
     * Ознака низького залишку на складі (досягнуто критичний поріг).
     */
    val isLowStock: Boolean
        get() = remainder > 0.0 && minRemainder > 0.0 && remainder <= minRemainder

    /**
     * Ознака повної відсутності товару на складі.
     */
    val isOutOfStock: Boolean
        get() = remainder <= 0.0

    /**
     * Розрахункова націнка у відсотках від собівартості.
     */
    val marginPercent: Double
        get() = if (costPrice > 0.0) ((price - costPrice) / costPrice) * 100.0 else 0.0

    /**
     * Очікуваний валовий прибуток на одній одиниці товару.
     */
    val grossProfitPerUnit: Double
        get() = (price - costPrice).coerceAtLeast(0.0)
}

