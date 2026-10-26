package com.kasa.model

import java.util.Locale

/**
 * Детермінований текстовий рендерер чекової стрічки для фіскальних реєстраторів,
 * чекових принтерів (ESC/POS) та екранного попереднього перегляду касира.
 */
object ReceiptTextRenderer {

    /**
     * Форматує об'єкт [Receipt] у моноширинний текстовий документ заданої ширини.
     *
     * @param receipt Фіскальний або розрахунковий чек.
     * @param headerLines Рядки шапки/кліше (назва торгової точки, адреса, код ЄДРПОУ, ПН, ФН).
     * @param footerLines Рядки підвалу (рекламний текст, умови повернення).
     * @param tapeWidth Ширина чекової стрічки в символах (32 для 57мм вузької, 40 для стандартної Datecs, 48 для 80мм).
     */
    fun render(
        receipt: Receipt,
        headerLines: List<String> = emptyList(),
        footerLines: List<String> = listOf("ДЯКУЄМО ЗА ПОКУПКУ!", "ГАРНОГО ДНЯ!"),
        tapeWidth: Int = 40
    ): String {
        val width = tapeWidth.coerceAtLeast(30)
        val sb = StringBuilder()

        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)

        // 1. Шапка кліше торгової точки
        sb.appendLine(doubleDivider)
        for (line in headerLines.filter { it.isNotBlank() }) {
            sb.appendLine(centerText(line, width))
        }

        // 2. Інформація про чек
        sb.appendLine(divider)
        val docType = if (receipt.receiptType == ReceiptType.SALE) "ФІСКАЛЬНИЙ ЧЕК" else "ВИДАТКОВИЙ ЧЕК"
        sb.appendLine(centerText(docType, width))

        val checkNum = receipt.fiscalCheckNumber?.let { "ЧЕК № $it" } ?: "ПОПЕРЕДНІЙ ПЕРЕГЛЯД"
        val operationType = if (receipt.receiptType == ReceiptType.SALE) "ПРОДАЖ" else "ПОВЕРНЕННЯ"
        sb.appendLine(justifyText(checkNum, operationType, width))

        sb.appendLine(divider)

        // 3. Товарні позиції
        for (item in receipt.items) {
            sb.appendLine(item.productName)

            val qtyStr = String.format(Locale.US, "%.3f", item.quantity)
            val priceStr = formatKopecks(item.priceKopecks)
            val totalStr = "${formatKopecks(item.totalKopecks)} ${item.taxGroup.codeLetter}"

            val detailsLeft = "  $qtyStr x $priceStr"
            sb.appendLine(justifyText(detailsLeft, totalStr, width))

            if (item.discountPercent > 0.0) {
                val discLabel = "  ЗНИЖКА ${String.format(Locale.US, "%.1f", item.discountPercent)}%"
                val discVal = "-${formatKopecks(item.discountKopecks)}"
                sb.appendLine(justifyText(discLabel, discVal, width))
            }

            if (!item.exciseStamp.isNullOrBlank()) {
                sb.appendLine("  АКЦИЗ: ${item.exciseStamp}")
            }
        }

        sb.appendLine(divider)

        // Блок програми лояльності
        receipt.customer?.let { customer ->
            sb.appendLine("КЛІЄНТ: ${customer.name.uppercase()}")
            if (customer.cardBarcode.isNotBlank()) {
                sb.appendLine(justifyText("КАРТКА:", customer.maskedCardNumber, width))
            }
            sb.appendLine(divider)
        }

        // 4. Підсумки та розрахунок
        val rawTotalStr = formatKopecks(receipt.rawTotalKopecks)
        sb.appendLine(justifyText("СУМА", rawTotalStr, width))

        if (receipt.discountPercent > 0.0) {
            val discLabel = "ЗАГАЛЬНА ЗНИЖКА ${String.format(Locale.US, "%.1f", receipt.discountPercent)}%"
            val discVal = "-${formatKopecks(receipt.receiptDiscountKopecks)}"
            sb.appendLine(justifyText(discLabel, discVal, width))
            val subtotalStr = formatKopecks(receipt.subtotalAfterDiscountKopecks)
            sb.appendLine(justifyText("ПІДСУМОК ЗІ ЗНИЖКОЮ", subtotalStr, width))
        }

        if (receipt.cashRoundingDifferenceKopecks != 0L) {
            val roundStr = formatKopecks(receipt.cashRoundingDifferenceKopecks)
            sb.appendLine(justifyText("ЗАОКРУГЛЕННЯ", roundStr, width))
        }

        val payableStr = "${formatKopecks(receipt.totalPayableKopecks)} ГРН"
        sb.appendLine(justifyText("ДО ОПЛАТИ", payableStr, width))

        when (receipt.paymentType) {
            PaymentType.CASH -> sb.appendLine(justifyText("ГОТІВКА", formatKopecks(receipt.totalPayableKopecks), width))
            PaymentType.CARD -> sb.appendLine(justifyText("БЕЗГОТІВКОВА (КАРТКА)", formatKopecks(receipt.totalPayableKopecks), width))
            PaymentType.COMBINED -> {
                val card = receipt.splitCardKopecks ?: 0L
                val cash = receipt.splitCashKopecks ?: 0L
                if (card > 0L) sb.appendLine(justifyText("БЕЗГОТІВКА (КАРТКА)", formatKopecks(card), width))
                if (cash > 0L) sb.appendLine(justifyText("ГОТІВКА", formatKopecks(cash), width))
            }
        }

        // 5. Податковий блок
        sb.appendLine(divider)
        val itemsByTax = receipt.items.groupBy { it.taxGroup }
        for ((taxGroup, groupItems) in itemsByTax) {
            val groupSum = groupItems.fold(0L) { acc, i -> acc + i.totalKopecks }
            val vat = FiscalMath.calculateTaxKopecks(groupSum, taxGroup)
            if (vat > 0L) {
                val label = "ПДВ ${taxGroup.codeLetter} (${String.format(Locale.US, "%.1f", taxGroup.vatRatePercent)}%)"
                sb.appendLine(justifyText(label, formatKopecks(vat), width))
            }
        }

        if (receipt.totalExciseKopecks > 0L) {
            sb.appendLine(justifyText("АКЦИЗНИЙ ЗБІР 5.00%", formatKopecks(receipt.totalExciseKopecks), width))
        }

        // 6. Підвал
        sb.appendLine(divider)
        for (footer in footerLines.filter { it.isNotBlank() }) {
            sb.appendLine(centerText(footer, width))
        }

        val taxUrl = FiscalUrlGenerator.generateTaxVerificationUrl(receipt)
        if (taxUrl != null) {
            sb.appendLine(divider)
            sb.appendLine(centerText("ПЕРЕВІРКА ЧЕКА В ДПС:", width))
            sb.appendLine(centerText("cabinet.tax.gov.ua", width))
        }
        sb.appendLine(doubleDivider)

        return sb.toString()
    }

    private fun formatKopecks(kopecks: Long): String {
        return String.format(Locale.US, "%.2f", kopecks / 100.0)
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun justifyText(left: String, right: String, width: Int): String {
        val totalLength = left.length + right.length
        if (totalLength >= width) {
            val spaceNeeded = width - right.length - 1
            val truncatedLeft = if (spaceNeeded > 0) left.take(spaceNeeded) else ""
            return "$truncatedLeft $right"
        }
        val spaces = " ".repeat(width - totalLength)
        return left + spaces + right
    }
}
