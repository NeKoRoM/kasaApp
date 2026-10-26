package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тести валідації демонстраційного каталогу товарів для касового терміналу.
 * Перевіряють консистентність ідентифікаторів, штрих-кодів, цін, ставок ПДВ,
 * секцій фіскального реєстратора та залишків на складі.
 */
class SampleProductCatalogTest {

    @Test
    fun testSampleCatalogNotEmpty() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        assertTrue(catalog.isNotEmpty(), "Демонстраційний каталог не повинен бути порожнім")
        assertEquals(8, catalog.size, "Очікується 8 демонстраційних товарів")
    }

    @Test
    fun testProductIdsAreUniqueAndPositive() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        val ids = catalog.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals(ids.size, uniqueIds.size, "Усі ID товарів у каталозі мають бути унікальними")
        assertTrue(ids.all { it > 0 }, "Усі ID товарів мають бути додатними числами")
    }

    @Test
    fun testPricesAreValidAndPositive() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        catalog.forEach { product ->
            assertTrue(product.price > 0.0, "Ціна товару '${product.name}' має бути додатною")
            assertTrue(product.remainder >= 0.0, "Залишок товару '${product.name}' не може бути від'ємним")
        }
    }

    @Test
    fun testBarcodesAreUniqueAndValid() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        val allBarcodes = catalog.flatMap { it.barcodes }
        val uniqueBarcodes = allBarcodes.toSet()

        assertEquals(allBarcodes.size, uniqueBarcodes.size, "Штрих-коди в каталозі не повинні дублюватися")
        catalog.forEach { product ->
            assertTrue(product.barcodes.isNotEmpty(), "Товар '${product.name}' повинен мати хоча б один штрих-код")
            product.barcodes.forEach { barcode ->
                assertTrue(barcode.all { it.isDigit() }, "Штрих-код '$barcode' повинен містити лише цифри")
            }
        }
    }

    @Test
    fun testExciseProductsHaveValidUktzed() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        val exciseProducts = catalog.filter { it.requiresExciseStamp }

        assertFalse(exciseProducts.isEmpty(), "Каталог повинен містити підакцизні товари для тестування акцизних марок")
        exciseProducts.forEach { product ->
            assertNotNull(product.uktzed, "Підакцизний товар '${product.name}' повинен мати код УКТЗЕД")
            assertTrue(product.uktzed!!.length == 10, "Код УКТЗЕД для '${product.name}' має бути 10-значним")
        }
    }

    @Test
    fun testWeightedProductsHaveUnitOfMeasurement() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        val weightedProducts = catalog.filter { it.isWeighted }

        assertFalse(weightedProducts.isEmpty(), "Каталог повинен містити вагові товари для тестування ваг")
        weightedProducts.forEach { product ->
            assertEquals("кг", product.unitOfMeasurement, "Одиниця виміру вагового товару '${product.name}' має бути 'кг'")
            assertTrue(product.remainder > 0.0, "Ваговий товар '${product.name}' повинен мати додатний залишок")
        }
    }

    @Test
    fun testTaxGroupsAndSectionsAreConfigured() {
        val catalog = SampleProductCatalog.createSampleCatalog()
        catalog.forEach { product ->
            assertNotNull(product.taxGroup, "Товар '${product.name}' повинен мати призначену податкову групу")
            assertTrue(product.section in 1..10, "Секція товару '${product.name}' має бути в діапазоні 1..10")
        }
    }

    @Test
    fun testProductSearchLookupByNameAndBarcode() {
        val catalog = SampleProductCatalog.createSampleCatalog()

        val bread = catalog.find { it.name.contains("Хліб", ignoreCase = true) }
        assertNotNull(bread, "Товар 'Хліб' повинен бути присутнім у демо-каталозі")
        assertEquals("482000000001", bread.barcodes.first())

        val coffee = catalog.find { it.barcodes.contains("482000000007") }
        assertNotNull(coffee, "Товар зі штрих-кодом 482000000007 повинен знаходитися")
        assertEquals("Кава в зернах 250г", coffee.name)
    }
}
