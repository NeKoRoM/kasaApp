package com.example.exellio20.domain.usecases

/**
 * ====================================================================================================
 * ЮЗКЕЙС ГЕНЕРАЦІЇ ВАГОВОГО ШТРИХКОДУ EAN-13 (CODE OF ARTICLE TO CODE FOR KASA USE CASE)
 * ====================================================================================================
 *
 * Формує стандартний 13-значний штрихкод внутрішнього маркування вагового або штучного товару (префікс "23"):
 *
 * АЛГОРИТМ СТРУКТУРИ EAN-13 ВАГОВОГО ТОВАРУ:
 * `[23] [5 цифр: PLU] [2 цифри: ціла частина ваги/кількості] [3 цифри: дробова частина ваги] [1 цифра: контрольна сума]`
 *
 * РОЗРАХУНОК КОНТРОЛЬНОГО РОЗРЯДУ (EAN-13 Checksum):
 * 1. Цифри на парних позиціях множаться на 3.
 * 2. Цифри на непарних позиціях множаться на 1.
 * 3. Сума обчислюється, і контрольна цифра знаходиться як `(10 - (sum % 10)) % 10`.
 */
class CodeOfArticleToCodeForKasaUseCase {

    /**
     * Генерує 13-значний штрихкод для передачі в касовий апарат або друку на вагах.
     *
     * @param code Артикул або PLU товару (до 5 цифр, доповнюється провідними нулями).
     * @param count Ціла частина кількості чи ваги в грамах/кілограмах (до 2 цифр).
     * @param fractionalPart Дробова частина ваги (до 3 цифр, наприклад грами).
     * @return 13-значний валідний рядок EAN-13.
     */
    fun execute(code: String, count: String, fractionalPart: String): String {
        var codeToSend = "23"
        var i = 5 - code.length
        var j = 2 - count.length
        var f = 3 - fractionalPart.length

        // Доповнення коду товару нулями до 5 знаків
        while (i > 0) {
            codeToSend += "0"
            i--
        }
        codeToSend += code

        // Доповнення цілої частини ваги нулями до 2 знаків
        while (j > 0) {
            codeToSend += "0"
            j--
        }
        codeToSend += count
        codeToSend += fractionalPart

        // Доповнення дробової частини ваги нулями до 3 знаків
        while (f > 0) {
            codeToSend += "0"
            f--
        }

        // Обчислення контрольної суми за алгоритмом EAN-13
        var controlSum = 0
        var parity = 0
        for (c in codeToSend) {
            controlSum += if ((parity % 2) == 0) {
                c.toString().toInt() * 3
            } else {
                c.toString().toInt()
            }
            parity++
        }

        controlSum = (10 - (controlSum % 10)) % 10
        codeToSend += controlSum.toString()

        return codeToSend
    }
}