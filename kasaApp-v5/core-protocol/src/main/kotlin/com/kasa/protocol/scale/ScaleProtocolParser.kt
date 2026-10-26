package com.kasa.protocol.scale

/**
 * Типізоване значення маси від електронних торгових ваг.
 */
data class ScaleWeight(
    val weightGrams: Int,
    val isStable: Boolean,
    val rawMessage: String
) {
    /**
     * Вага в кілограмах для розрахунку суми (вага * ціна).
     */
    val weightKilograms: Double
        get() = weightGrams / 1000.0
}

/**
 * Драйвер-парсер потоку даних від електронних ваг через Bluetooth SPP / Serial COM.
 * Підтримує протоколи CAS, DIGI, Промприлад та сумісних торгових ваг.
 */
object ScaleProtocolParser {

    /** Команда обнулення / тарування ваг */
    val TARE_COMMAND_BYTES: ByteArray = "T\r\n".toByteArray(Charsets.US_ASCII)

    private val WEIGHT_REGEX = Regex("([+-]?\\s*\\d+\\.?\\d*)")

    /**
     * Парсить текстовий рядок з потоку ваг та повертає [ScaleWeight] або null, якщо рядок не містить ваги.
     *
     * Приклади вхідних рядків:
     * - "ST,GS,+  1.250kg" -> 1250г, стабільний
     * - "US,GS,+  0.500kg" -> 500г, нестабільний
     * - "  1.250 \r\n" -> 1250г
     * - "W: -  0.020kg" -> -20г
     */
    fun parse(rawMessage: String): ScaleWeight? {
        val trimmed = rawMessage.trim()
        if (trimmed.isEmpty()) return null

        val match = WEIGHT_REGEX.find(trimmed) ?: return null
        val weightStr = match.groupValues[1].replace(" ", "")
        val kgValue = weightStr.toDoubleOrNull() ?: return null

        val grams = Math.round(kgValue * 1000.0).toInt()

        // Визначення стабільності ваги за стандартними префіксами
        val isStable = when {
            trimmed.contains("US", ignoreCase = true) -> false
            trimmed.contains("DYN", ignoreCase = true) -> false
            trimmed.contains("ST", ignoreCase = true) -> true
            else -> true // За замовчуванням вважаємо стабільним, якщо немає маркера US
        }

        return ScaleWeight(
            weightGrams = grams,
            isStable = isStable,
            rawMessage = trimmed
        )
    }
}
