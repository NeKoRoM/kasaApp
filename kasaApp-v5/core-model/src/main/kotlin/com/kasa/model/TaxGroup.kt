package com.kasa.model

import kotlinx.serialization.Serializable

/**
 * Податкові групи відповідно до чинного фіскального законодавства України
 * та внутрішньої кодифікації реєстраторів Exellio / Datecs.
 */
@Serializable
enum class TaxGroup(
    val codeLetter: String,
    val vatRatePercent: Double,
    val hasExcise: Boolean = false,
    val description: String
) {
    GROUP_A("А", 20.0, false, "Основна ставка ПДВ 20%"),
    GROUP_B("Б", 0.0, false, "Нульова ставка ПДВ 0%"),
    GROUP_V("В", 7.0, false, "Пільгова ставка ПДВ 7% (медикаменти)"),
    GROUP_G("Г", 0.0, false, "Звільнені від ПДВ (ст. 197 ПКУ)"),
    GROUP_D("Д", 0.0, false, "Не є об'єктом оподаткування"),
    GROUP_A_EXCISE("А", 20.0, true, "ПДВ 20% + Акцизний збір 5% (алкоголь, тютюн)"),
    GROUP_D_EXCISE("Д", 0.0, true, "Без ПДВ + Акцизний збір 5%");

    companion object {
        fun fromCode(letter: String, isExcise: Boolean = false): TaxGroup {
            val clean = letter.trim()
            return when {
                isExcise && (clean == "А" || clean == "A" || clean == "1" || clean == "5" || clean == "6") -> GROUP_A_EXCISE
                isExcise && (clean == "Д" || clean == "D" || clean == "7" || clean == "8") -> GROUP_D_EXCISE
                clean == "А" || clean == "A" || clean == "1" -> GROUP_A
                clean == "Б" || clean == "B" || clean == "2" -> GROUP_B
                clean == "В" || clean == "V" || clean == "3" -> GROUP_V
                clean == "Г" || clean == "G" || clean == "4" -> GROUP_G
                clean == "Д" || clean == "D" || clean == "5" -> GROUP_D
                else -> if (isExcise) GROUP_A_EXCISE else GROUP_A
            }
        }
    }
}
