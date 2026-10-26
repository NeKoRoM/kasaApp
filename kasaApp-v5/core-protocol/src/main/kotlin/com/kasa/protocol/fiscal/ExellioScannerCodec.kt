package com.kasa.protocol.fiscal

import com.kasa.model.Receipt
import com.kasa.model.ReceiptItem
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Кодек внутрішніх вагових/кількісних штрих-кодів для касових апаратів Exellio (DP-05, DP-15, DP-25)
 * у режимі реєстрації продажів (Режим 1 — «Реєстрація»).
 *
 * У режимі «Реєстрація» Bluetooth SPP порт касового апарата Exellio працює як вхід
 * бездротового сканера штрих-кодів і очікує 13-значний внутрішній штрих-код формату:
 *
 *   23 <PLU: 5 цифр> <ЦІЛА_КІЛЬКІСТЬ: 2 цифри> <ДРОБОВА_КІЛЬКІСТЬ: 3 цифри> <КОНТРОЛЬНА_СУМА: 1 цифра> \r
 *
 * Приклад (PLU = 1, кількість = 1.000):
 *   "23" + "00001" + "01" + "000" + "5" + "\r" -> "2300001010005\r"
 *
 * Якщо товар підакцизний і має акцизну марку, після відправки коду товару (через паузу 200 мс)
 * окремим рядком передається серія та номер акцизної марки, завершені символом '\r' (0x0D).
 */
object ExellioScannerCodec {

    private val windows1251: Charset = Charset.forName("windows-1251")
    private const val INTERNAL_PREFIX = "23"
    private const val CARRIAGE_RETURN = "\r"

    /**
     * Формує 13-значний внутрішній штрих-код Exellio (без символу переносу рядка '\r')
     * для реєстрації товару з кодом [pluCode] та кількістю [quantity].
     */
    fun encodePluSaleCode(pluCode: Long, quantity: Double): String {
        val sanitizedPlu = pluCode.coerceIn(1L, 99999L)
        val sanitizedQty = quantity.coerceIn(0.001, 99.999)

        val intPart = sanitizedQty.toInt().coerceIn(0, 99)
        val fracPart = ((sanitizedQty - intPart) * 1000.0).roundToInt().coerceIn(0, 999)

        val pluFormatted = String.format(Locale.US, "%05d", sanitizedPlu)
        val intFormatted = String.format(Locale.US, "%02d", intPart)
        val fracFormatted = String.format(Locale.US, "%03d", fracPart)

        val payload12 = "$INTERNAL_PREFIX$pluFormatted$intFormatted$fracFormatted"
        val checkDigit = calculateControlDigit(payload12)
        return "$payload12$checkDigit"
    }

    /**
     * Формує готовий масив байтів у кодуванні Windows-1251 із завершальним '\r' (0x0D)
     * для прямої передачі товарної позиції у Bluetooth SPP сокет каси.
     */
    fun encodePluSaleBytes(pluCode: Long, quantity: Double): ByteArray {
        val codeWithCr = encodePluSaleCode(pluCode, quantity) + CARRIAGE_RETURN
        return codeWithCr.toByteArray(windows1251)
    }

    /**
     * Формує готовий масив байтів для передачі акцизної марки або зовнішнього штрих-коду
     * у кодуванні Windows-1251 із завершальним '\r' (0x0D).
     */
    fun encodeRawLineBytes(rawValue: String): ByteArray {
        val clean = rawValue.trim() + CARRIAGE_RETURN
        return clean.toByteArray(windows1251)
    }

    /**
     * Впорядковує позиції чека перед відправкою в Exellio у режимі реєстрації
     * (спочатку звичайні товари, потім підакцизні — аналогічно kasaApp 3.0).
     */
    fun orderItemsForRegistration(receipt: Receipt): List<ReceiptItem> {
        return receipt.items.sortedBy { it.exciseStamp ?: "" }
    }

    /**
     * Обчислює контрольну цифру за алгоритмом Exellio / kasaApp 3.0:
     * парні індекси (0, 2, 4, 6, 8, 10) множаться на 3, непарні — на 1.
     */
    fun calculateControlDigit(twelveDigits: String): Int {
        require(twelveDigits.length == 12 && twelveDigits.all { it.isDigit() }) {
            "Expected exactly 12 digits, got: '$twelveDigits'"
        }
        var sum = 0
        for ((index, ch) in twelveDigits.withIndex()) {
            val digit = ch - '0'
            sum += if (index % 2 == 0) digit * 3 else digit
        }
        return (10 - (sum % 10)) % 10
    }
}
