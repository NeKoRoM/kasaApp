package com.kasa.model

/**
 * Генератор стандартних фіскальних штрих-кодів роздрібної торгівлі (EAN-13).
 * Підтримує генерацію внутрішніх кодів фасування (префікс 20..29) для штучних та вагових товарів.
 */
object BarcodeGenerator {

    /**
     * Генерація внутрішнього штучного штрих-коду EAN-13 для власного фасування (префікс 25).
     * Формат: 25 + [5 цифр PLU] + [5 нулів] + [контрольний розряд].
     * Наприклад: PLU 101 -> 2500101000007 (розпізнається як штучний товар).
     */
    fun generateInternalPieceEan13(plu: Long): String {
        val safePlu = (plu % 100000L).coerceAtLeast(1L)
        val pluFormatted = safePlu.toString().padStart(5, '0')
        val payload12 = "25${pluFormatted}00000"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(payload12)
        return "$payload12$checkDigit"
    }

    /**
     * Генерація вагового штрих-коду торгових ваг (префікс 20..24).
     * Формат: [префікс 2 цифри] + [5 цифр PLU] + [5 цифр ваги в грамах] + [контрольний розряд].
     * Наприклад: префікс 21, PLU 102, вага 1.250 кг (1250 г) -> 210010201250 + check.
     */
    fun generateWeightedEan13(prefix: Int = 21, plu: Long, weightGrams: Int): String {
        require(prefix in 20..24) { "Префікс вагового штрих-коду має бути в межах 20..24" }
        val safePlu = (plu % 100000L).coerceAtLeast(1L).toString().padStart(5, '0')
        val safeGrams = (weightGrams % 100000).coerceAtLeast(0).toString().padStart(5, '0')
        val payload12 = "$prefix$safePlu$safeGrams"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(payload12)
        return "$payload12$checkDigit"
    }

    /**
     * Генерація стандартного товарного штрих-коду EAN-13 з національним префіксом України (482).
     * Формат: 482 + [9 цифр коду товару] + [контрольний розряд] = 13 символів.
     */
    fun generateRetailEan13(plu: Long): String {
        val safePlu = (plu % 1000000000L).coerceAtLeast(1L).toString().padStart(9, '0')
        val payload12 = "482$safePlu"
        val checkDigit = BarcodeParser.calculateEan13CheckDigit(payload12)
        return "$payload12$checkDigit"
    }

    /**
     * Валідація валідності повного 13-значного штрих-коду EAN-13 за контрольною сумою.
     */
    fun isValidEan13(barcode: String): Boolean {
        return BarcodeParser.isValidEan13Checksum(barcode)
    }
}
