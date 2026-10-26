package com.kasa.feature.pos

import com.kasa.model.PaymentType
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.model.ReceiptItem
import com.kasa.model.TaxGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileJsonReceiptJournalStorageTest {

    @Test
    fun testRecordAndLoadReceipts() {
        val tempFile = File.createTempFile("test_receipts_journal", ".json")
        tempFile.deleteOnExit()

        val storage = FileJsonReceiptJournalStorage(tempFile)
        assertTrue(storage.loadJournal().isEmpty())

        val product = Product(
            id = 1L,
            name = "Чай зелений",
            price = 55.00,
            taxGroup = TaxGroup.GROUP_A
        )
        val receipt = Receipt(
            id = "rec-12345",
            createdAt = 1700000000L,
            cashierId = 1L,
            items = listOf(ReceiptItem.fromProduct(product, 2.0)),
            paymentType = PaymentType.CASH,
            isFiscalized = true,
            fiscalCheckNumber = "000055"
        )

        storage.recordReceipt(receipt)

        val reloadedStorage = FileJsonReceiptJournalStorage(tempFile)
        val journal = reloadedStorage.loadJournal()

        assertEquals(1, journal.size)
        assertEquals("rec-12345", journal[0].id)
        assertEquals("000055", journal[0].fiscalCheckNumber)
        assertEquals(11000L, journal[0].totalPayableKopecks)
        assertEquals(1, journal[0].items.size)
        assertEquals("Чай зелений", journal[0].items[0].productName)

        tempFile.delete()
    }
}
