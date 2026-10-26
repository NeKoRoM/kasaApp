package com.kasa.model

/**
 * Генератор форматованих цінників та етикеток для друку на термопринтері каси (57мм / 80мм).
 * Формує читабельний стандартизований цінник для полички торгового залу.
 */
object PriceTagRenderer {

    private const val LINE_WIDTH = 32

    /**
     * Рендерить текстовий цінник для товару.
     */
    fun renderPriceTag(
        product: Product,
        storeName: String = "МАГАЗИН",
        printDate: String = "2026-09-25",
        lineWidth: Int = LINE_WIDTH
    ): String {
        val sb = StringBuilder()
        val sepDouble = "=".repeat(lineWidth)
        val sepSingle = "-".repeat(lineWidth)

        sb.append(sepDouble).append("\n")
        sb.append(centerText(storeName, lineWidth)).append("\n")
        sb.append(centerText("ЦІННИК ТОВАРУ", lineWidth)).append("\n")
        sb.append(sepDouble).append("\n")

        // Назва товару
        sb.append(wrapText(product.name, lineWidth)).append("\n")

        // PLU та категорія
        val catInfo = if (product.categoryName.isNotBlank()) "Кат: ${product.categoryName}" else "Відділ: ${product.section}"
        val pluInfo = "PLU: #${product.id}"
        sb.append(twoColumns(pluInfo, catInfo, lineWidth)).append("\n")
        sb.append(sepSingle).append("\n")

        // Велика ціна
        val uah = product.price.toLong()
        val kop = Math.round((product.price - uah) * 100.0).toInt()
        val priceStr = String.format("%d.%02d ГРН", uah, kop)
        sb.append("ЦІНА ЗА 1 ${product.unitOfMeasurement}:\n")
        sb.append(centerText(">>> $priceStr <<<", lineWidth)).append("\n")
        sb.append(sepSingle).append("\n")

        // Штрих-код
        val mainBarcode = product.barcodes.firstOrNull() ?: BarcodeGenerator.generateInternalPieceEan13(product.id)
        sb.append("Штрих-код: $mainBarcode\n")
        sb.append(centerText("||| |||| ||||| |||| |||", lineWidth)).append("\n")

        // Податкова інформація та дата
        val taxInfo = "ПДВ: ${product.taxGroup.codeLetter} (${product.taxGroup.vatRatePercent}%)"
        sb.append(twoColumns(taxInfo, "Дата: $printDate", lineWidth)).append("\n")
        sb.append(sepDouble).append("\n")

        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val left = (width - text.length) / 2
        val right = width - text.length - left
        return " ".repeat(left) + text + " ".repeat(right)
    }

    private fun twoColumns(left: String, right: String, width: Int): String {
        val totalLen = left.length + right.length
        if (totalLen >= width) {
            val maxLeft = (width - right.length - 1).coerceAtLeast(1)
            return left.take(maxLeft) + " " + right
        }
        val spaces = " ".repeat(width - totalLen)
        return left + spaces + right
    }

    private fun wrapText(text: String, width: Int): String {
        val words = text.split(" ")
        val sb = StringBuilder()
        var lineLen = 0
        for (word in words) {
            if (lineLen + word.length + 1 > width) {
                sb.append("\n")
                lineLen = 0
            } else if (lineLen > 0) {
                sb.append(" ")
                lineLen++
            }
            sb.append(word)
            lineLen += word.length
        }
        return sb.toString()
    }
}
