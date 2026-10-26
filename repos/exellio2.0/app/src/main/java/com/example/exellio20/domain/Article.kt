package com.example.exellio20.domain

import java.io.Serializable

/**
 * ====================================================================================================
 * ЕТАЛОННИЙ ПАРСЕР ТА ОНТОЛОГІЯ ТОВАРУ DATECS / EXELLIO (ТАБУЛЯТОРНИЙ ФОРМАТ РРО)
 * ====================================================================================================
 *
 * Даний файл містить еталонний алгоритм розбору сирої відповіді від РРО/касового апарата Exellio
 * (команди запиту товарів 68 / 107). Відповідь від прошивки апарата повертається у вигляді текстового
 * рядка, де окремі фіскальні та складські параметри товару розділені символом табуляції `\t` (0x09).
 *
 * СТРУКТУРА ПОЛІВ ТАБУЛЯТОРНОЇ ВІДПОВІДІ КАСОВОГО АПАРАТА (17 ПОЛІВ):
 * - 1:  [code] — Номер комірки/артикулу в пам'яті РРО (PLU, 1..50000)
 * - 2:  [TAX] — Податкова група ПДВ (літера 'А'..'Ж')
 * - 3:  [section] — Номер відділу (секції) у касовому апараті
 * - 4:  [group] — Товарна група (код групи)
 * - 5:  [type] — Ознака типу товару (0 - звичайний, штучний тощо)
 * - 6:  [price] — Фіскальна ціна товару за одиницю (з копійками)
 * - 7:  [salesAmount] — Сумарний грошовий обіг з даного товару в оперативній пам'яті РРО
 * - 8:  [salesCount] — Кількість проведених операцій продажу
 * - 9:  [remainder] — Поточний залишок товару в пам'яті РРО
 * - 10: [barCode1] — Основний штрихкод товару (EAN-13 / UPC / EAN-8)
 * - 11: [barCode2] — Додатковий штрихкод 2
 * - 12: [barCode3] — Додатковий штрихкод 3
 * - 13: [barCode4] — Додатковий штрихкод 4
 * - 14: [name] — Фіскальна назва товару (до 32 або 48 символів залежно від моделі РРО)
 * - 15: [unit] — Одиниця виміру товару (наприклад, "шт", "кг", "л")
 * - 16: [UKTZED] — Код УКТЗЕД для підакцизних товарів (алкоголь, тютюн, паливо)
 * - 17: [productInSet] — Ознака вагового, комплекту або складового товару
 */
class Article(kp: String) : Comparable<Article>, Serializable {

    val article = ArticleData()

    init {
        val dane = mutableListOf<Int>()
        for ((index, value) in kp.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }

        if (dane.size >= 17) {
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
    }

    override fun compareTo(other: Article): Int = article.code.compareTo(other.article.code)

    /**
     * Повна структура даних товару в енергонезалежній пам'яті фіскального реєстратора Exellio.
     */
    data class ArticleData(
        var code: String = "",         // 1: PLU артикул у касі
        var TAX: String = "",          // 2: Податкова група ('А'..'Ж')
        var section: String = "",      // 3: Секція / відділ
        var group: String = "",        // 4: Товарна група
        var type: String = "",         // 5: Тип позиції
        var price: String = "",        // 6: Ціна за одиницю
        var salesAmount: String = "",  // 7: Обіг
        var salesCount: String = "",   // 8: Кількість чеків продажу
        var remainder: String = "",    // 9: Залишок
        var barCode1: String = "",     // 10: Штрихкод 1
        var barCode2: String = "",     // 11: Штрихкод 2
        var barCode3: String = "",     // 12: Штрихкод 3
        var barCode4: String = "",     // 13: Штрихкод 4
        var name: String = "",         // 14: Фіскальне найменування
        var unit: String = "",         // 15: Одиниця виміру
        var UKTZED: String = "",       // 16: Код УКТЗЕД
        var productInSet: String = "", // 17: Ознака вагового/набору
    ) : Serializable
}