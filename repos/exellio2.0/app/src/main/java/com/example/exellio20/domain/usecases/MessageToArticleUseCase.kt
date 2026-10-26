package com.example.exellio20.domain.usecases

import com.example.exellio20.roomDB.entities.ArticleItem

/**
 * ====================================================================================================
 * ЮЗКЕЙС ТРАНСФОРМАЦІЇ ВІДПОВІДІ РРО В ТОВАР (MESSAGE TO ARTICLE USE CASE)
 * ====================================================================================================
 *
 * Здійснює розбір табуляторного текстового рядка відповіді від касового апарату Exellio (команди 68 / 107)
 * та створює сутність [ArticleItem] для локального збереження в Room.
 *
 * РОЗБИРАЄ 17 ПОЛІВ:
 * 1. `code`: PLU номер товару
 * 2. `TAX`: податкова група
 * 3. `section`: відділ/секція
 * 4. `group`: товарна група
 * 5. `type`: тип товару
 * 6. `price`: ціна
 * 7. `salesAmount`: обіг
 * 8. `salesCount`: кількість чеків
 * 9. `remainder`: залишок
 * 10..13: `barCode1..4`: штрихкоди
 * 14: `name`: фіскальна назва
 * 15: `weightOrUnit`: ваговий або штучний
 * 16: `UKTZED`: код УКТЗЕД
 * 17: `unitOfMeasurement`: одиниця виміру
 */
class MessageToArticleUseCase {

    /**
     * Парсить сирий рядок від РРО [message] та заповнює екземпляр [ArticleItem].
     *
     * @param message Текстовий рядок відповіді від каси з символами табуляції `\t`.
     * @return Повністю заповнений об'єкт [ArticleItem].
     */
    fun execute(message: String): ArticleItem {
        val article = ArticleItem()

        val dane = mutableListOf<Int>()
        for ((index, value) in message.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }
        var code = ""

        if (dane.size >= 17) {
            for ((index, value) in message.withIndex()) {
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
            }
        }
        article.id = code.toIntOrNull() ?: 0
        return article
    }
}