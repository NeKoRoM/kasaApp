// =================================================================================================
// USE CASE: ПАРСИНГ ВІДПОВІДІ РРО DATECS / EXELLIO В СУТНІСТЬ ТОВАРУ (PRODUCT PARSER)
//
// Архітектурний шар: Domain Layer (Use Case).
// Призначення:
// Десеріалізує сирий текстовий кадр відповіді касового апарата (команда 68 / 0x44)
// у типізований об'єкт ProductEntity для збереження в Room Database.
//
// Протокольний формат кадру відповіді Datecs:
// Дані розділені символом табуляції ('\t'). Всього в кадрі 17 полів:
// [0] Код (PLU / productId)
// [1] Податкова група (TAX: 1-8 / А-К)
// [2] Номер відділу (section: 1-99)
// [3] Товарна група (group: 1-99)
// [4] Тип товару (type)
// [5] Ціна (price, копійки або гривні з копійками)
// [6] Сума продажів накопичувальна (salesAmount)
// [7] Кількість продажів накопичувальна (salesCount)
// [8] Залишок товару (remainder)
// [9..12] Штрих-коди 1..4 (barCode1..4)
// [13] Найменування товару (name)
// [14] Ознака ваги (weightOrUnit)
// [15] Код УКТЗЕД (UKTZED)
// [16] Одиниця виміру (unitOfMeasurement: шт, кг, л тощо)
// =================================================================================================

package com.nekorom.kasaapp.domain.usecase

import android.util.Log
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.utils.USER
import java.lang.Exception

/**
 * Прецедент парсингу текстового пакету товару від РРО.
 */
class MessageToArticleUseCase {

    /**
     * Перетворює сирий текстовий рядок [message] відповіді РРО у сутність [ProductEntity].
     *
     * @param message Текстовий рядок, що надійшов від касового апарата через Bluetooth.
     * @return Заповнений екземпляр [ProductEntity] з прив'язкою до поточного касира [USER].
     */
    fun execute(message: String): ProductEntity {
        val article = ProductEntity(userId = USER?.userId ?: 100)
        Log.d("toArt", message)

        // Крок 1: Індексуємо всі розділювачі табуляції '\t' у повідомленні
        val dane = mutableListOf<Int>()
        for ((index, value) in message.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }
        var code = ""

        // Крок 2: Посимвольно розподіляємо значення між табуляціями у відповідні поля моделі
        for ((index, value) in message.withIndex()) {
            try {
                if (index > dane[0] && index < dane[1])
                    code += value
                else if (index > dane[1] && index < dane[2])
                    article.TAX += value
                else if (index > dane[2] && index < dane[3])
                    article.section += value
                else if (index > dane[3] && index < dane[4])
                    article.group += value
                else if (index > dane[4] && index < dane[5])
                    article.type += value
                else if (index > dane[5] && index < dane[6])
                    article.price += value
                else if (index > dane[6] && index < dane[7])
                    article.salesAmount += value
                else if (index > dane[7] && index < dane[8])
                    article.salesCount += value
                else if (index > dane[8] && index < dane[9])
                    article.remainder += value
                else if (index > dane[9] && index < dane[10])
                    article.barCode1 += value
                else if (index > dane[10] && index < dane[11])
                    article.barCode2 += value
                else if (index > dane[11] && index < dane[12])
                    article.barCode3 += value
                else if (index > dane[12] && index < dane[13])
                    article.barCode4 += value
                else if (index > dane[13] && index < dane[14])
                    article.name += value
                else if (index > dane[14] && index < dane[15])
                    article.weightOrUnit += value
                else if (index > dane[15] && index < dane[16])
                    article.UKTZED += value
                else if (index > dane[16] && index < dane[17])
                    article.unitOfMeasurement += value

            } catch (e: Exception) {
                // У разі передчасного завершення кадру або пошкодження даних
                if (code != "") {
                    article.productId = code.toInt()
                }
                return article
            }
        }

        // Крок 3: Фіксуємо артикул товару як цілочисельний ідентифікатор РРО
        if (code.isNotEmpty()) {
            article.productId = code.toInt()
        }
        return article
    }
}