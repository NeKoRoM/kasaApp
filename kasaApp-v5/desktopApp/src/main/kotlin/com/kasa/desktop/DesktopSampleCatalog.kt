package com.kasa.desktop

import com.kasa.model.Product
import com.kasa.model.TaxGroup

/**
 * Демонстраційний каталог товарів для першого запуску або тестування терміналу.
 */
object DesktopSampleCatalog {

    fun createSampleCatalog(): List<Product> = listOf(
        Product(
            id = 1,
            name = "Хліб пшеничний 500г",
            price = 24.50,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000001"),
            isWeighted = false
        ),
        Product(
            id = 2,
            name = "Молоко 'Селянське' 2.5%",
            price = 38.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000002"),
            isWeighted = false
        ),
        Product(
            id = 3,
            name = "Яблука 'Голден' (Вагові)",
            price = 28.90,
            taxGroup = TaxGroup.GROUP_A,
            section = 2,
            barcodes = listOf("482000000003"),
            isWeighted = true,
            unitOfMeasurement = "кг"
        ),
        Product(
            id = 4,
            name = "Банани свіжі (Вагові)",
            price = 56.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 2,
            barcodes = listOf("482000000004"),
            isWeighted = true,
            unitOfMeasurement = "кг"
        ),
        Product(
            id = 5,
            name = "Пиво 'Чернігівське Світле'",
            price = 34.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 3,
            barcodes = listOf("482000000005"),
            isWeighted = false,
            uktzed = "2203000100"
        ),
        Product(
            id = 6,
            name = "Цигарки 'Marlboro Gold'",
            price = 98.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 4,
            barcodes = listOf("482000000006"),
            isWeighted = false,
            uktzed = "2402209020"
        ),
        Product(
            id = 7,
            name = "Кава в зернах 250г",
            price = 145.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000007"),
            isWeighted = false
        ),
        Product(
            id = 8,
            name = "Вода 'Моршинська' н/г 1.5л",
            price = 19.50,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000008"),
            isWeighted = false
        )
    )
}
