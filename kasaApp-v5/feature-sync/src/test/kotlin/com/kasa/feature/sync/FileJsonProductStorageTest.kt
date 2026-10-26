package com.kasa.feature.sync

import com.kasa.model.Product
import com.kasa.model.TaxGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileJsonProductStorageTest {

    @Test
    fun testSaveAndReloadCatalog() {
        val tempFile = File.createTempFile("test_catalog", ".json")
        tempFile.deleteOnExit()

        val storage = FileJsonProductStorage(tempFile)
        assertTrue(storage.loadCatalog().isEmpty())

        val product = Product(
            id = 101L,
            name = "Кава Арабіка 250г",
            price = 145.00,
            taxGroup = TaxGroup.GROUP_A,
            barcodes = listOf("482000000101"),
            isWeighted = false
        )

        storage.saveCatalog(listOf(product))

        val reloadedStorage = FileJsonProductStorage(tempFile)
        val catalog = reloadedStorage.loadCatalog()

        assertEquals(1, catalog.size)
        assertEquals(101L, catalog[0].id)
        assertEquals("Кава Арабіка 250г", catalog[0].name)
        assertEquals(145.00, catalog[0].price)
        assertEquals(listOf("482000000101"), catalog[0].barcodes)

        tempFile.delete()
    }
}
