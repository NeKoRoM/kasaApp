package com.kasa.model

/**
 * Тип розпізнаного штрих-коду.
 */
sealed interface ParsedBarcode {
    /**
     * Стандартний штучний товар (EAN-13, EAN-8, Code-128 тощо).
     */
    data class Standard(val barcode: String) : ParsedBarcode

    /**
     * Ваговий товар із друкованої етикетки торгових ваг (префікси 20..24).
     * Формат: 2X PPPPP WWWWW C (13 символів).
     */
    data class Weighted(
        val rawBarcode: String,
        val plu: Long,
        val weightKilograms: Double
    ) : ParsedBarcode

    /**
     * Товар із закодованою вартістю з етикетки (префікси 28..29).
     * Формат: 2X PPPPP SSSSS C (13 символів).
     */
    data class PriceEmbedded(
        val rawBarcode: String,
        val plu: Long,
        val totalPriceKopecks: Long
    ) : ParsedBarcode
}

/**
 * Парсер та валідатор штрих-кодів роздрібної торгівлі України.
 * Підтримує стандартну перевірку контрольних сум EAN-13 та розбір
 * етикеток фасувальних ваг (CAS, Bizerba, DIGI, Mettler Toledo).
 */
object BarcodeParser {

    /**
     * Валідація контрольної цифри EAN-13 за алгоритмом Modulo-10.
     */
    fun isValidEan13Checksum(barcode: String): Boolean {
        if (barcode.length != 13 || !barcode.all { it.isDigit() }) return false
        val sum = barcode.take(12).mapIndexed { index, char ->
            val digit = char.digitToInt()
            if (index % 2 == 0) digit else digit * 3
        }.sum()
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == barcode[12].digitToInt()
    }

    /**
     * Розрахунок контрольної цифри EAN-13 для перших 12 цифр.
     */
    fun calculateEan13CheckDigit(twelveDigits: String): Int {
        require(twelveDigits.length == 12 && twelveDigits.all { it.isDigit() }) {
            "Потрібно рівно 12 десяткових цифр"
        }
        val sum = twelveDigits.mapIndexed { index, char ->
            val digit = char.digitToInt()
            if (index % 2 == 0) digit else digit * 3
        }.sum()
        return (10 - (sum % 10)) % 10
    }

    /**
     * Розбір штрих-коду з автоматичною детекцією вагових етикеток.
     */
    fun parse(barcode: String): ParsedBarcode {
        val trimmed = barcode.trim()
        if (trimmed.length == 13 && trimmed.all { it.isDigit() }) {
            val prefix = trimmed.substring(0, 2)
            if (isValidEan13Checksum(trimmed)) {
                // Вагові штрих-коди торговельних ваг (префікси 20..24)
                if (prefix in listOf("20", "21", "22", "23", "24")) {
                    val plu = trimmed.substring(2, 7).toLongOrNull() ?: 0L
                    val weightGrams = trimmed.substring(7, 12).toDoubleOrNull() ?: 0.0
                    val weightKg = weightGrams / 1000.0
                    return ParsedBarcode.Weighted(
                        rawBarcode = trimmed,
                        plu = plu,
                        weightKilograms = weightKg
                    )
                }

                // Штрих-коди із зашитою вартістю (префікси 28..29)
                if (prefix in listOf("28", "29")) {
                    val plu = trimmed.substring(2, 7).toLongOrNull() ?: 0L
                    val kopecks = trimmed.substring(7, 12).toLongOrNull() ?: 0L
                    return ParsedBarcode.PriceEmbedded(
                        rawBarcode = trimmed,
                        plu = plu,
                        totalPriceKopecks = kopecks
                    )
                }
            }
        }

        return ParsedBarcode.Standard(trimmed)
    }
}
