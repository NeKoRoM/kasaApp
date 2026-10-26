package com.example.exellio20.domain.usecases

/**
 * ====================================================================================================
 * ЮЗКЕЙС ВИЛУЧЕННЯ ФІСКАЛЬНОГО НОМЕРА РРО (ID KASA TO STRING USE CASE)
 * ====================================================================================================
 *
 * Витягує заводський або фіскальний реєстраційний номер касового апарата (ID каси)
 * з табуляторного текстового рядка відповіді РРО на команду діагностики/інформації.
 */
class IdKasaToStringUseCase {

    /**
     * Парсить рядок [message], знаходячи вміст між першим та другим символами табуляції `\t`.
     *
     * @param message Необроблений рядок відповіді від касового апарата.
     * @return Фіскальний номер РРО у вигляді рядка.
     */
    fun execute(message: String): String {
        var idKasa = ""
        val dane = mutableListOf<Int>()
        for ((index, value) in message.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }

        if (dane.size >= 2) {
            for ((index, value) in message.withIndex()) {
                if (index > dane[0] && index < dane[1])
                    idKasa += value
            }
        }
        return idKasa
    }
}