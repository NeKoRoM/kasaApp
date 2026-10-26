package com.kasa.model

/**
 * Рушій генерації компактного аркуша / стрічки штрих-кодів товарів для термодруку (57мм / 80мм).
 * Дозволяє касиру видрукувати швидкі штрих-коди товарів без упаковки (випічка, кава, фрукти)
 * та сканувати їх безпосередньо з надрукованого аркуша на касовому місці.
 */
object BarcodeSheetRenderer {

    private const val DEFAULT_LINE_WIDTH = 32

    /**
     * Формує текстовий документ стрічки штрих-кодів для передачі на друк у РРО або чековий принтер.
     *
     * @param title Заголовок документа (за замовчуванням "АРКУШ ШТРИХ-КОДІВ").
     * @param products Список товарів, які необхідно включити до аркуша друку.
     * @param categoryName Необов'язкова назва фільтрованої товарної групи.
     * @param storeName Назва торгової точки або магазину.
     * @param lineWidth Ширина рядка термострічки у символах (32 для 57мм, 42..48 для 80мм).
     */
    fun renderBarcodeSheet(
        title: String = "АРКУШ ШТРИХ-КОДІВ",
        products: List<Product>,
        categoryName: String? = null,
        storeName: String = "KASA-POS СУПЕРМАРКЕТ",
        lineWidth: Int = DEFAULT_LINE_WIDTH
    ): String {
        val sb = StringBuilder()
        val sepDouble = "=".repeat(lineWidth)
        val sepSingle = "-".repeat(lineWidth)

        sb.append(sepDouble).append("\n")
        sb.append(centerText(storeName, lineWidth)).append("\n")
        sb.append(centerText(title, lineWidth)).append("\n")
        if (!categoryName.isNullOrBlank()) {
            sb.append(centerText("Категорія: $categoryName", lineWidth)).append("\n")
        }
        sb.append(centerText("Для сканування на касі", lineWidth)).append("\n")
        sb.append(sepDouble).append("\n")

        if (products.isEmpty()) {
            sb.append(centerText("(Немає товарів для друку)", lineWidth)).append("\n")
            sb.append(sepDouble).append("\n")
            return sb.toString()
        }

        for (product in products) {
            val barcode = product.barcodes.firstOrNull() ?: BarcodeGenerator.generateInternalPieceEan13(product.id)
            val uah = product.price.toLong()
            val kop = Math.round((product.price - uah) * 100.0).toInt()
            val priceStr = String.format("%d.%02d грн", uah, kop)

            sb.append("[#${product.id}] ${product.printableName}\n")
            sb.append(twoColumns("Ціна: $priceStr", "Од: ${product.unitOfMeasurement}", lineWidth)).append("\n")
            sb.append("Штрихкод: $barcode\n")
            sb.append(centerText("||| |||| ||||| |||| |||", lineWidth)).append("\n")
            sb.append(sepSingle).append("\n")
        }

        sb.append(twoColumns("Позицій: ${products.size}", "KASA-POS V5", lineWidth)).append("\n")
        sb.append(sepDouble).append("\n")

        return sb.toString()
    }

    /**
     * Центрує вказаний текст у межах заданої ширини рядка [width].
     */
    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val left = (width - text.length) / 2
        val right = width - text.length - left
        return " ".repeat(left) + text + " ".repeat(right)
    }

    /**
     * Форматує два текстових поля по лівому та правому краю рядка фіксованої ширини [width].
     */
    private fun twoColumns(left: String, right: String, width: Int): String {
        val totalLen = left.length + right.length
        if (totalLen >= width) {
            val maxLeft = (width - right.length - 1).coerceAtLeast(1)
            return left.take(maxLeft) + " " + right
        }
        val spaces = " ".repeat(width - totalLen)
        return left + spaces + right
    }
}
