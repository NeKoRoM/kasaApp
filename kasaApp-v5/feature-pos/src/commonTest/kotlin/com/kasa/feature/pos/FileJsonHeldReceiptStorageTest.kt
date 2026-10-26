package com.kasa.feature.pos

import com.kasa.model.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileJsonHeldReceiptStorageTest {

    @Test
    fun testAddLoadAndRemoveHeldReceipts() {
        val tempFile = File.createTempFile("test_held_receipts", ".json")
        tempFile.deleteOnExit()

        val storage = FileJsonHeldReceiptStorage(tempFile)
        assertTrue(storage.loadHeldReceipts().isEmpty())

        val product = Product(
            id = 10L,
            name = "Молоко 2.5%",
            price = 38.50,
            taxGroup = TaxGroup.GROUP_A
        )
        val receipt1 = Receipt(
            id = "rec-held-1",
            createdAt = 1700000100L,
            cashierId = 1L,
            items = listOf(ReceiptItem.fromProduct(product, 2.0)),
            paymentType = PaymentType.CASH,
            discountPercent = 5.0
        )
        val held1 = HeldReceipt(
            id = "held-uuid-1",
            heldAt = 1700000150L,
            note = "Клієнт пішов за хлібом",
            receipt = receipt1
        )

        storage.addHeldReceipt(held1)

        val reloaded = FileJsonHeldReceiptStorage(tempFile).loadHeldReceipts()
        assertEquals(1, reloaded.size)
        assertEquals("held-uuid-1", reloaded[0].id)
        assertEquals("Клієнт пішов за хлібом", reloaded[0].note)
        assertEquals(7320L, reloaded[0].receipt.totalPayableKopecks) // 77.00 - 5% = 73.15 -> NBU rounding decree №25 -> 73.20 грн

        // Додаємо другий відкладений чек
        val receipt2 = Receipt(
            id = "rec-held-2",
            createdAt = 1700000200L,
            cashierId = 1L,
            items = listOf(ReceiptItem.fromProduct(product, 1.0))
        )
        val held2 = HeldReceipt(
            id = "held-uuid-2",
            heldAt = 1700000250L,
            note = "Забув картку",
            receipt = receipt2
        )
        storage.addHeldReceipt(held2)

        val twoHeld = storage.loadHeldReceipts()
        assertEquals(2, twoHeld.size)
        assertEquals("held-uuid-2", twoHeld[0].id) // найновіший перший

        // Видаляємо перший відкладений чек
        val removed = storage.removeHeldReceipt("held-uuid-1")
        assertNotNull(removed)
        assertEquals("held-uuid-1", removed.id)

        val remaining = storage.loadHeldReceipts()
        assertEquals(1, remaining.size)
        assertEquals("held-uuid-2", remaining[0].id)

        // Спроба видалити неіснуючий
        val removedNonExistent = storage.removeHeldReceipt("unknown-id")
        assertNull(removedNonExistent)

        tempFile.delete()
    }
}
