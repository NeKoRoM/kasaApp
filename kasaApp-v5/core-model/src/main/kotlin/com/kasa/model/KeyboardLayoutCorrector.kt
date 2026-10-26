package com.kasa.model

/**
 * Конвертер розкладки клавіатури (QWERTY <-> ЙЦУКЕН).
 * Забезпечує миттєвий пошук товарів касиром навіть при введенні в хибній розкладці клавіатури (наприклад "[ks," -> "хліб").
 */
object KeyboardLayoutCorrector {

    private val enToUkMap = mapOf(
        'q' to 'й', 'w' to 'ц', 'e' to 'у', 'r' to 'к', 't' to 'е', 'y' to 'н', 'u' to 'г', 'i' to 'ш', 'o' to 'щ', 'p' to 'з',
        '[' to 'х', ']' to 'ї', 'a' to 'ф', 's' to 'і', 'd' to 'в', 'f' to 'а', 'g' to 'п', 'h' to 'р', 'j' to 'о', 'k' to 'л',
        'l' to 'д', ';' to 'ж', '\'' to 'є', 'z' to 'я', 'x' to 'ч', 'c' to 'с', 'v' to 'м', 'b' to 'и', 'n' to 'т', 'm' to 'ь',
        ',' to 'б', '.' to 'ю', '`' to 'ʼ',
        'Q' to 'Й', 'W' to 'Ц', 'E' to 'У', 'R' to 'К', 'T' to 'Е', 'Y' to 'Н', 'U' to 'Г', 'I' to 'Ш', 'O' to 'Щ', 'P' to 'З',
        '{' to 'Х', '}' to 'Ї', 'A' to 'Ф', 'S' to 'І', 'D' to 'В', 'F' to 'А', 'G' to 'П', 'H' to 'Р', 'J' to 'О', 'K' to 'Л',
        'L' to 'Д', ':' to 'Ж', '"' to 'Є', 'Z' to 'Я', 'X' to 'Ч', 'C' to 'С', 'V' to 'М', 'B' to 'И', 'N' to 'Т', 'M' to 'Ь',
        '<' to 'Б', '>' to 'Ю', '~' to 'Ґ'
    )

    private val ukToEnMap = enToUkMap.entries.associate { (k, v) -> v to k }

    /**
     * Конвертує текст з англійської розкладки в українську (наприклад, "[ks," -> "хліб").
     */
    fun toUkrainian(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(enToUkMap[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Конвертує текст з української розкладки в англійську.
     */
    fun toEnglish(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(ukToEnMap[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Перевіряє, чи містить назва товару шуканий запит, враховуючи можливу помилку розкладки клавіатури.
     */
    fun matchesQuery(text: String, query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return true
        if (text.contains(cleanQuery, ignoreCase = true)) return true

        val ukVariant = toUkrainian(cleanQuery)
        if (ukVariant != cleanQuery && text.contains(ukVariant, ignoreCase = true)) return true

        val enVariant = toEnglish(cleanQuery)
        if (enVariant != cleanQuery && text.contains(enVariant, ignoreCase = true)) return true

        return false
    }
}
