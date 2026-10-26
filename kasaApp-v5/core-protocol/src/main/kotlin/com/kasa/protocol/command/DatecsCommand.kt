package com.kasa.protocol.command

import java.util.Locale

/**
 * Строгий DSL фіскальних команд касового апарата Exellio / Datecs.
 * Усі команди мають явний код та формують валідне корисне навантаження.
 */
sealed interface DatecsCommand {
    val rawCommandCode: String
    fun buildPayload(): String

    /** Зчитування часу та дати з годинника реального часу (RTC) каси */
    data object ReadDateTime : DatecsCommand {
        override val rawCommandCode: String = "003>"
        override fun buildPayload(): String = ""
    }

    /** Встановлення дати і часу в касі (формат: dd-MM-yy HH:mm:ss) */
    data class SetDateTime(val formattedDateTime: String) : DatecsCommand {
        override val rawCommandCode: String = "003="
        override fun buildPayload(): String = "$formattedDateTime\t"
    }

    /** Запит заводського та фіскального номерів РРО */
    data object GetKasaId : DatecsCommand {
        override val rawCommandCode: String = "00??"
        override fun buildPayload(): String = "IDnumber\t\t\t"
    }

    /** Зчитування рядка кліше чека (шапка 0..6) */
    data class ReadHeaderLine(val lineIndex: Int) : DatecsCommand {
        override val rawCommandCode: String = "00??"
        override fun buildPayload(): String = "Header\t$lineIndex\t\t"
    }

    /** Програмування рядка кліше чека (шапка 0..6) */
    data class ProgramHeaderLine(val lineIndex: Int, val text: String) : DatecsCommand {
        override val rawCommandCode: String = "00??"
        override fun buildPayload(): String = "Header\t$lineIndex\t$text\t"
    }

    /** Запит встановлених ставок податкових груп (ПДВ/Акциз) */
    data object ReadTaxRates : DatecsCommand {
        override val rawCommandCode: String = "0053"
        override fun buildPayload(): String = ""
    }

    /** Друк денного звіту без обнулення оперативної пам'яті (X-звіт) */
    data object PrintXReport : DatecsCommand {
        override val rawCommandCode: String = "0045X"
        override fun buildPayload(): String = "\t"
    }

    /** Друк денного фіскального звіту з обнуленням оперативної пам'яті (Z-звіт) */
    data object PrintZReport : DatecsCommand {
        override val rawCommandCode: String = "0045Z"
        override fun buildPayload(): String = "\t"
    }

    /** Відкриття фіскального чека (1 = продаж, 2 = повернення) з паролем касира */
    data class OpenReceipt(
        val operatorPassword: Int = 1,
        val receiptType: Int = 1
    ) : DatecsCommand {
        override val rawCommandCode: String = "0048"
        override fun buildPayload(): String = "$receiptType\t$operatorPassword\t"
    }

    /** Реєстрація продажу товарної позиції у відкритому чеку з підтримкою знижки */
    data class RegisterSale(
        val name: String,
        val priceKopecks: Long,
        val quantity: Double,
        val taxGroupLetter: String,
        val discountPercent: Double = 0.0
    ) : DatecsCommand {
        override val rawCommandCode: String = "0049"
        override fun buildPayload(): String {
            val priceStr = String.format(Locale.US, "%.2f", priceKopecks / 100.0)
            val qtyStr = String.format(Locale.US, "%.3f", quantity)
            return if (discountPercent > 0.0) {
                val discStr = String.format(Locale.US, "%.2f", discountPercent)
                "$name\t$priceStr\t$qtyStr\t$taxGroupLetter\t1\t1\t$discStr\t"
            } else {
                "$name\t$priceStr\t$qtyStr\t$taxGroupLetter\t"
            }
        }
    }

    /** Підсумок чека (Subtotal) з підтримкою відсоткової знижки на весь чек */
    data class Subtotal(
        val discountPercent: Double = 0.0
    ) : DatecsCommand {
        override val rawCommandCode: String = "0051"
        override fun buildPayload(): String {
            return if (discountPercent > 0.0) {
                val discStr = String.format(Locale.US, "%.2f", discountPercent)
                "1\t0\t1\t$discStr\t"
            } else {
                "\t"
            }
        }
    }

    /** Оплата чека (paymentType: 0 = готівка, 1 = банківська картка) */
    data class Total(val paymentType: Int, val amountKopecks: Long) : DatecsCommand {
        override val rawCommandCode: String = "0054"
        override fun buildPayload(): String {
            val amountStr = String.format(Locale.US, "%.2f", amountKopecks / 100.0)
            return "$paymentType\t$amountStr\t"
        }
    }

    /** Закриття та завершення друку чека */
    data object CloseReceipt : DatecsCommand {
        override val rawCommandCode: String = "0056"
        override fun buildPayload(): String = ""
    }

    /** Анулювання відкритого чека (Void) */
    data object VoidReceipt : DatecsCommand {
        override val rawCommandCode: String = "0060"
        override fun buildPayload(): String = ""
    }

    /** Службове внесення / видача готівки (додатна сума = внесення '0', від'ємна = видача '1') */
    data class CashInOut(val amountKopecks: Long) : DatecsCommand {
        override val rawCommandCode: String = "0046"
        override fun buildPayload(): String {
            val typeCode = if (amountKopecks < 0L) "1" else "0"
            val amountStr = String.format(Locale.US, "%.2f", kotlin.math.abs(amountKopecks) / 100.0)
            return "$typeCode\t$amountStr\t"
        }
    }

    /** Відкриття грошової скриньки (Команда 106 / 0x6A -> 006:, імпульс 50 мс) */
    data object OpenDrawer : DatecsCommand {
        override val rawCommandCode: String = "006:"
        override fun buildPayload(): String = "50\t"
    }

    /** Повна діагностична інформація РРО (Команда 90 / 0x5A -> 005:, модель, версія ПЗ, ЗН, ФН) */
    data object ReadDiagnosticInfo : DatecsCommand {
        override val rawCommandCode: String = "005:"
        override fun buildPayload(): String = "1\t"
    }

    /** Звуковий сигнал зумера РРО (Команда 80 / 0x50 -> 0050) */
    data class SoundBeep(val hz: Int = 1000, val durationMs: Int = 100) : DatecsCommand {
        override val rawCommandCode: String = "0050"
        override fun buildPayload(): String = "$hz\t$durationMs\t"
    }

    /** Читання артикулу за кодом PLU */
    data class ReadPlu(val pluCode: Long) : DatecsCommand {
        override val rawCommandCode: String = "006;R\t"
        override fun buildPayload(): String = "$pluCode\t"
    }

    /** Читання першого збереженого артикулу */
    data class ReadFirstPlu(val startPlu: Long = 1L) : DatecsCommand {
        override val rawCommandCode: String = "006;F\t"
        override fun buildPayload(): String = "$startPlu\t"
    }

    /** Читання наступного артикулу */
    data object ReadNextPlu : DatecsCommand {
        override val rawCommandCode: String = "006;N\t"
        override fun buildPayload(): String = ""
    }

    /** Пошук останнього збереженого артикулу */
    data object ReadLastPlu : DatecsCommand {
        override val rawCommandCode: String = "006;L\t"
        override fun buildPayload(): String = "100000\t"
    }

    /** Видалення артикулу з пам'яті РРО за номером */
    data class DeletePlu(val pluCode: Long) : DatecsCommand {
        override val rawCommandCode: String = "006;D\t"
        override fun buildPayload(): String = "$pluCode\t$pluCode\t"
    }

    /** Програмування товару в пам'ять РРО (16 полів Exellio DP-25) */
    data class ProgramPlu(
        val pluCode: Long,
        val taxGroupLetter: String,
        val section: Int,
        val group: Int,
        val isWeighted: Boolean,
        val priceKopecks: Long,
        val remainder: Double,
        val barcodes: List<String>,
        val name: String,
        val uktzed: String,
        val unitName: String
    ) : DatecsCommand {
        override val rawCommandCode: String = "006;P\t"
        override fun buildPayload(): String {
            val priceStr = String.format(Locale.US, "%.2f", priceKopecks / 100.0)
            val remStr = String.format(Locale.US, "%.3f", remainder)
            val taxCode = when (taxGroupLetter.trim()) {
                "1", "А", "A" -> "1"
                "2", "Б", "B" -> "2"
                "3", "В", "V" -> "3"
                "4", "Г", "G" -> "4"
                "5", "Д", "D" -> "5"
                "6", "Е", "E" -> "6"
                else -> "1"
            }
            val weightVal = if (isWeighted) "1" else "0"
            val unitCode = when (unitName.trim().lowercase(Locale.ROOT)) {
                "0", "шт", "шт.", "посл", "послуга" -> "0"
                "1", "кг", "кг." -> "1"
                "2", "м", "м." -> "2"
                "3", "л", "л." -> "3"
                "4", "г", "г." -> "4"
                else -> if (isWeighted) "1" else "0"
            }
            val b1 = barcodes.getOrElse(0) { "" }
            val b2 = barcodes.getOrElse(1) { "" }
            val b3 = barcodes.getOrElse(2) { "" }
            val b4 = barcodes.getOrElse(3) { "" }

            return "$pluCode\t$taxCode\t$section\t$group\t0\t$priceStr\t\t" +
                    "$remStr\t$b1\t$b2\t$b3\t$b4\t$name\t$weightVal\t$uktzed\t$unitCode\t"
        }
    }

    /** Вивід текстового рядка на дисплей покупця (Команда 47 / 0x2F -> 002?, до 20 символів) */
    data class DisplayText(val line: Int, val text: String) : DatecsCommand {
        override val rawCommandCode: String = "002?"
        override fun buildPayload(): String = "${text.take(20).ifEmpty { " " }}\t"
    }

    /** Очищення дисплея покупця (20 пробілів через команду 47 / 0x2F -> 002?) */
    data class ClearDisplay(val line: Int = 0) : DatecsCommand {
        override val rawCommandCode: String = "002?"
        override fun buildPayload(): String = "                    \t"
    }
}
