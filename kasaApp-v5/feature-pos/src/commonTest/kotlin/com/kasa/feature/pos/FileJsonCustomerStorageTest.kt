package com.kasa.feature.pos

import com.kasa.model.Customer
import java.io.File
import kotlin.test.*

class FileJsonCustomerStorageTest {

    private lateinit var tempFile: File
    private lateinit var storage: FileJsonCustomerStorage

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("test_customers", ".json")
        tempFile.delete()
        storage = FileJsonCustomerStorage(tempFile)
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun testDefaultsCreatedWhenFileMissing() {
        val customers = storage.loadCustomers()
        assertTrue(customers.isNotEmpty())
        assertEquals(3, customers.size)
        assertEquals("Іван Петренко", customers[0].name)
    }

    @Test
    fun testSaveAndFindCustomerByBarcode() {
        val custom = listOf(
            Customer(id = 10L, name = "Тарас Шевченко", cardBarcode = "990000000099", discountPercent = 15.0)
        )
        storage.saveCustomers(custom)

        val found = storage.findCustomerByBarcode("990000000099")
        assertNotNull(found)
        assertEquals("Тарас Шевченко", found.name)
        assertEquals(15.0, found.discountPercent)

        assertNull(storage.findCustomerByBarcode("000000000000"))
    }

    @Test
    fun testFindCustomerByPhone() {
        val custom = listOf(
            Customer(id = 20L, name = "Леся Українка", phone = "+380509998877", cardBarcode = "CARD01")
        )
        storage.saveCustomers(custom)

        val found = storage.findCustomerByPhone("0509998877")
        assertNotNull(found)
        assertEquals("Леся Українка", found.name)
    }

    @Test
    fun testSearchCustomers() {
        val results = storage.searchCustomers("Ковальчук")
        assertEquals(1, results.size)
        assertEquals("Олена Ковальчук", results[0].name)

        val emptySearch = storage.searchCustomers("")
        assertEquals(3, emptySearch.size)
    }
}
