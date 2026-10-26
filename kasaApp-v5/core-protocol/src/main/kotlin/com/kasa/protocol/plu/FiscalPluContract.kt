package com.kasa.protocol.plu

import com.kasa.model.Product
import com.kasa.model.TaxGroup
import com.kasa.protocol.fiscal.FiscalError

/**
 * Стани та результати операцій з енергонезалежною таблицею артикулів (PLU) РРО.
 */
sealed interface FiscalPluResult<out T> {
    data class Success<T>(
        val data: T,
        val message: String = ""
    ) : FiscalPluResult<T>

    data class Failure(
        val error: FiscalError,
        val message: String,
        val rawResponse: String = ""
    ) : FiscalPluResult<Nothing>
}

/**
 * Прогрес пакетної передачі або зчитування бази артикулів РРО.
 */
data class PluSyncProgress(
    val current: Int,
    val total: Int,
    val currentItemName: String = "",
    val percent: Float = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
)

/**
 * Парсер текстових рядків протоколу Datecs / Exellio для артикулів PLU.
 */
object PluRecordParser {

    /**
     * Розбирає табуляторну відповідь каси (команди 006;F, 006;N, 006;R, 006;L) у [Product].
     *
     * Формат кадру відповіді:
     * 006;R0 \t <id> \t <tax> \t <sec> \t <grp> \t <type> \t <price> \t <salesSum> \t <salesQty> \t
     * <rem> \t <b1> \t <b2> \t <b3> \t <b4> \t <name> \t <isWeight> \t <uktzed> \t <unit> \t <status>
     */
    fun parse(rawResponse: String): Product? {
        if (!rawResponse.contains("006;")) return null
        if (rawResponse.contains("-1030") || rawResponse.contains("-111004")) return null

        val parts = rawResponse.split('\t')
        val offset = if (parts.getOrNull(7)?.trim().isNullOrEmpty()) -1 else 0
        val unitIndex = 17 + offset
        if (parts.size <= unitIndex) return null
        // Якщо кадр обірвано посередині поля одиниці виміру (немає ні завершального табу, ні маркера статусу \u0004)
        if (parts.size == unitIndex + 1 && !rawResponse.contains('\u0004')) return null

        // Перший елемент містить префікс команди, наприклад "006;F0" або "006;R0"
        val codeStr = parts.getOrNull(1)?.trim() ?: return null
        val pluId = codeStr.toLongOrNull() ?: return null

        val taxLetter = parts.getOrNull(2)?.trim() ?: "А"
        val section = parts.getOrNull(3)?.trim()?.toIntOrNull() ?: 1
        val group = parts.getOrNull(4)?.trim()?.toIntOrNull() ?: 1
        val priceStr = parts.getOrNull(6)?.trim()?.replace(',', '.') ?: "0.00"
        val price = priceStr.toDoubleOrNull() ?: 0.0

        val remainderStr = parts.getOrNull(9 + offset)?.trim()?.replace(',', '.') ?: "0.00"
        val remainder = remainderStr.toDoubleOrNull() ?: 0.0

        val b1 = parts.getOrNull(10 + offset)?.trim() ?: ""
        val b2 = parts.getOrNull(11 + offset)?.trim() ?: ""
        val b3 = parts.getOrNull(12 + offset)?.trim() ?: ""
        val b4 = parts.getOrNull(13 + offset)?.trim() ?: ""
        val barcodes = listOf(b1, b2, b3, b4).filter { it.isNotBlank() && it != "0" }

        val rawName = parts.getOrNull(14 + offset)?.trim() ?: ""
        val name = rawName.ifBlank { "Товар $pluId" }
        val isWeightedStr = parts.getOrNull(15 + offset)?.trim() ?: "0"

        val uktzed = parts.getOrNull(16 + offset)?.trim() ?: ""
        val rawUnit = parts.getOrNull(unitIndex)?.substringBefore('\u0004')?.trim() ?: ""
        val unit = when (rawUnit) {
            "0" -> "шт"
            "1" -> "кг"
            "2" -> "м"
            "3" -> "л"
            "4" -> "г"
            "" -> if (isWeightedStr == "1") "кг" else "шт"
            else -> rawUnit
        }
        val isWeighted = unit == "кг" || unit == "г" || (rawUnit.isEmpty() && isWeightedStr == "1")

        val isExcise = isExciseTaxOrUktzed(taxLetter, uktzed)
        val taxGroup = TaxGroup.fromCode(taxLetter, isExcise)

        return Product(
            id = pluId,
            name = name,
            price = price,
            taxGroup = taxGroup,
            section = section,
            group = group,
            barcodes = barcodes,
            isWeighted = isWeighted,
            uktzed = uktzed,
            unitOfMeasurement = unit,
            remainder = remainder
        )
    }

    /**
     * Вилучає код PLU з відповіді на команду 006;L або з часткового/обірваного кадру 006;F / 006;N.
     * Вимагає наявності табуляції після коду (parts.size >= 3), щоб не зчитати обірване посередині число.
     */
    fun parseLastCode(rawResponse: String): Long {
        if (!rawResponse.contains("006;")) return 0L
        val parts = rawResponse.split('\t')
        if (parts.size < 3) return 0L
        return parts.getOrNull(1)?.substringBefore('\u0004')?.trim()?.toLongOrNull() ?: 0L
    }

    private val EXCISE_UKTZED_PREFIXES = listOf(
        "2203", "2204", "2205", "2206", "2207", "2208",
        "2401", "2402", "2403", "2404", "2710", "2711", "3824"
    )

    private fun isExciseTaxOrUktzed(taxLetter: String, uktzed: String): Boolean {
        if (taxLetter == "5" || taxLetter == "6") return true
        if (taxLetter.contains("Д") || taxLetter.contains("D") || taxLetter.contains("Е") || taxLetter.contains("E")) return true
        val cleanUktzed = uktzed.trim()
        return cleanUktzed.length >= 4 && EXCISE_UKTZED_PREFIXES.any { cleanUktzed.startsWith(it) }
    }
}
