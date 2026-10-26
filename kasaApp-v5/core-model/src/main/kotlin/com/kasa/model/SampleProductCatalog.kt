package com.kasa.model

/**
 * Стандартний демонстраційний каталог товарів для первинного старту POS-терміналу.
 * Містить товари повсякденного попиту, вагові позиції та підакцизні одиниці (алкоголь, тютюн).
 */
object SampleProductCatalog {

    fun createSampleCatalog(): List<Product> = listOf(
        Product(
            id = 1,
            name = "Хліб пшеничний 500г",
            price = 24.50,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000001"),
            isWeighted = false,
            remainder = 50.0
        ),
        Product(
            id = 2,
            name = "Молоко 'Селянське' 2.5%",
            price = 38.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000002"),
            isWeighted = false,
            remainder = 30.0
        ),
        Product(
            id = 3,
            name = "Яблука 'Голден' (Вагові)",
            price = 28.90,
            taxGroup = TaxGroup.GROUP_A,
            section = 2,
            barcodes = listOf("482000000003"),
            isWeighted = true,
            unitOfMeasurement = "кг",
            remainder = 75.5
        ),
        Product(
            id = 4,
            name = "Банани свіжі (Вагові)",
            price = 56.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 2,
            barcodes = listOf("482000000004"),
            isWeighted = true,
            unitOfMeasurement = "кг",
            remainder = 42.0
        ),
        Product(
            id = 5,
            name = "Пиво 'Чернігівське Світле'",
            price = 34.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 3,
            barcodes = listOf("482000000005"),
            isWeighted = false,
            uktzed = "2203000100",
            remainder = 120.0
        ),
        Product(
            id = 6,
            name = "Цигарки 'Marlboro Gold'",
            price = 98.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 4,
            barcodes = listOf("482000000006"),
            isWeighted = false,
            uktzed = "2402209020",
            remainder = 60.0
        ),
        Product(
            id = 7,
            name = "Кава в зернах 250г",
            price = 145.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000007"),
            isWeighted = false,
            remainder = 25.0
        ),
        Product(
            id = 8,
            name = "Вода 'Моршинська' н/г 1.5л",
            price = 19.50,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            barcodes = listOf("482000000008"),
            isWeighted = false,
            remainder = 80.0
        )
    )
}
