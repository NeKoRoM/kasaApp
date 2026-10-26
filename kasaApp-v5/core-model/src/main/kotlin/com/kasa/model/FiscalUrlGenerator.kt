package com.kasa.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Генератор офіційних посилань та корисного навантаження QR-кодів ДПС для онлайн-перевірки
 * фіскальних чеків на порталі Електронного кабінету платника податків України (cabinet.tax.gov.ua).
 */
object FiscalUrlGenerator {

    const val TAX_CABINET_BASE_URL: String = "https://cabinet.tax.gov.ua/cashregs/check"
    const val DEFAULT_FISCAL_NUMBER: String = "3000123456"

    /**
     * Формує офіційну URL-адресу перевірки чека на веб-порталі ДПС.
     *
     * @param receipt Фіскалізований розрахунковий документ.
     * @param defaultFn Фіскальний номер РРО за замовчуванням, якщо не вказано в чеку.
     * @return Повний URL перевірки або null, якщо чек не має присвоєного фіскального номера.
     */
    fun generateTaxVerificationUrl(
        receipt: Receipt,
        defaultFn: String = DEFAULT_FISCAL_NUMBER
    ): String? {
        val checkId = receipt.fiscalCheckNumber?.trim()?.ifBlank { null } ?: return null
        val fn = receipt.fiscalRegisterNumber?.trim()?.ifBlank { defaultFn } ?: defaultFn

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Kyiv")
        }
        val timeFormat = SimpleDateFormat("HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Kyiv")
        }

        val dateStr = dateFormat.format(Date(receipt.createdAt))
        val timeStr = timeFormat.format(Date(receipt.createdAt))
        val sumStr = String.format(Locale.US, "%.2f", receipt.totalPayableKopecks / 100.0)

        return "$TAX_CABINET_BASE_URL?fn=$fn&id=$checkId&date=$dateStr&time=$timeStr&sm=$sumStr"
    }

    /**
     * Формує корисне навантаження для QR-коду фіскального чека.
     */
    fun generateQrPayload(
        receipt: Receipt,
        defaultFn: String = DEFAULT_FISCAL_NUMBER
    ): String? {
        return generateTaxVerificationUrl(receipt, defaultFn)
    }
}
