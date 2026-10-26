package com.kasa.feature.pos

import java.util.Locale

/**
 * Доменний форматувальник грошових сум та кількостей для касового терміналу (POS).
 * Забезпечує детерміноване відображення валюти в гривнях (UAH) без похибок чисел із плаваючою комою:
 * - Форматування цілочисельних копійок (Long) у стандартний формат «грн.коп» (Locale.US із крапкою);
 * - Підтримка символу національної валюти «₴»;
 * - Безпечний парсинг рядка від касира або сканера у суму в копійках;
 * - Форматування одиниць виміру та дробових мас (кг / шт).
 */
object PosCurrencyFormatter {

    /**
     * Форматує суму в копійках у десятковий рядок гривень (наприклад, 1250L -> "12.50").
     */
    fun formatKopecks(kopecks: Long): String {
        return String.format(Locale.US, "%.2f", kopecks / 100.0)
    }

    /**
     * Форматує суму в копійках із символом гривні (наприклад, 1250L -> "12.50 ₴").
     */
    fun formatWithSymbol(kopecks: Long): String {
        return "${formatKopecks(kopecks)} ₴"
    }

    /**
     * Форматує суму в копійках із суфіксом «грн» (наприклад, 1250L -> "12.50 грн").
     */
    fun formatWithSuffix(kopecks: Long): String {
        return "${formatKopecks(kopecks)} грн"
    }

    /**
     * Форматує різницю зі знаком плюс або мінус (наприклад, +50L -> "+0.50 грн", -100L -> "-1.00 грн").
     */
    fun formatSignedKopecks(kopecks: Long): String {
        val sign = if (kopecks > 0L) "+" else if (kopecks < 0L) "-" else ""
        return "$sign${formatKopecks(Math.abs(kopecks))} грн"
    }

    /**
     * Форматує число з плаваючою комою (Double грн) у стандартний рядок (наприклад, 12.5 -> "12.50").
     */
    fun formatHryvnias(hryvnias: Double): String {
        return String.format(Locale.US, "%.2f", hryvnias)
    }

    /**
     * Безпечно парсить введений користувачем рядок (із комою або крапкою) у копійки (Long).
     * Повертає null у разі некоректного вводу.
     */
    fun parseToKopecks(input: String): Long? {
        val sanitized = input.trim().replace(',', '.')
        val parsedDouble = sanitized.toDoubleOrNull() ?: return null
        if (parsedDouble.isNaN() || parsedDouble.isInfinite() || parsedDouble < 0.0) return null
        return Math.round(parsedDouble * 100.0)
    }

    /**
     * Форматує кількість товару: для цілих чисел відкидає нулі, для дробових (вагових) залишає до 3 знаків.
     */
    fun formatQuantity(quantity: Double, unit: String): String {
        val qtyStr = if (quantity % 1.0 == 0.0) {
            "${quantity.toLong()}"
        } else {
            String.format(Locale.US, "%.3f", quantity).trimEnd('0').trimEnd('.')
        }
        return if (unit.isNotBlank()) "$qtyStr $unit" else qtyStr
    }

    /**
     * Виконує фіскальне заокруглення готівкової суми до 10 копійок згідно з постановою НБУ № 25:
     * - від 1 до 4 копійок — заокруглюється в бік зменшення до 0;
     * - від 5 до 9 копійок — заокруглюється в бік збільшення до 10.
     */
    fun roundToNbu10Kopecks(totalKopecks: Long): Long {
        val lastDigit = (totalKopecks % 10L).toInt()
        val base = totalKopecks - lastDigit
        return when {
            lastDigit in 1..4 -> base
            lastDigit in 5..9 -> base + 10L
            else -> totalKopecks
        }
    }

    /**
     * Повертає різницю заокруглення НБУ (наприклад, +2 коп або -3 коп).
     */
    fun calculateNbuRoundingDifference(totalKopecks: Long): Long {
        return roundToNbu10Kopecks(totalKopecks) - totalKopecks
    }
}
