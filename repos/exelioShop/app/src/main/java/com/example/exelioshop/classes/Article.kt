package com.example.exelioshop.classes

import java.io.Serializable

/**
 * [Article] — парсер текстової табульованої відповіді фіскального реєстратора Datecs/Exellio
 * на команду зчитування артикулу (PLU).
 * 
 * РРО Datecs повертає рядок, де 17 полів товару розділені символом табуляції `\t`:
 * 1. Код артикулу / PLU
 * 2. Податкова ставка (ПДВ)
 * 3. Відділ / секція
 * 4. Товарна група
 * 5. Тип ціни
 * 6. Ціна
 * 7. Сума продажів
 * 8. Кількість продажів
 * 9. Залишок товару
 * 10..13. Штрихкоди 1-4
 * 14. Назва товару
 * 15. Одиниця вимірювання
 * 16. Код УКТЗЕД
 * 17. Ознака вагового/штучного товару
 * 
 * @param kp сирий табульований рядок, отриманий від касового апарата
 */
class Article(kp: String) : Comparable<Article>, Serializable {

    /** Розпарсений об'єкт даних товару */
    val article = ArticleData()

    init {
        // Знаходження індексів усіх розділювачів табуляції '\t'
        val dane = mutableListOf<Int>()
        for ((index, value) in kp.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }

        // Посимвольний розподіл значень між позиціями табуляції у відповідні поля моделі
        for ((index, value) in kp.withIndex()) {
            if (index > dane[0] && index < dane[1])
                article.code += value
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
                article.unit += value
            else if (index > dane[15] && index < dane[16])
                article.UKTZED += value
            else if (index > dane[16] && index < dane[17])
                article.productInSet += value
        }
    }

    override fun compareTo(other: Article): Int = article.code.compareTo(other.article.code)

    /**
     * [ArticleData] — модель даних товару для внутрішнього представлення у парсері.
     */
    data class ArticleData(
        var code: String = "",         // 1: Код / PLU
        var TAX: String = "",          // 2: ПДВ
        var section: String = "",      // 3: Секція / Відділ
        var group: String = "",        // 4: Товарна група
        var type: String = "",         // 5: Тип ціни
        var price: String = "",        // 6: Ціна
        var salesAmount: String = "",  // 7: Сума продажів
        var salesCount: String = "",   // 8: Кількість продажів
        var remainder: String = "",    // 9: Залишок
        var barCode1: String = "",     // 10: Штрихкод 1
        var barCode2: String = "",     // 11: Штрихкод 2
        var barCode3: String = "",     // 12: Штрихкод 3
        var barCode4: String = "",     // 13: Штрихкод 4
        var name: String = "",         // 14: Назва
        var unit: String = "",         // 15: Одиниця вимірювання
        var UKTZED: String = "",       // 16: Код УКТЗЕД
        var productInSet: String = ""  // 17: Ознака вагового/штучного товару
    ) : Serializable
}