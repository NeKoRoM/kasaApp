package com.kasa.feature.sync

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileJsonSyncStorageTest {

    @Test
    fun testSaveAndReloadFromDisk() {
        val tempFile = File.createTempFile("test_sync_queue", ".json")
        tempFile.deleteOnExit()

        val storage = FileJsonSyncStorage(tempFile)
        assertTrue(storage.loadQueue().isEmpty())

        val order = OrderCreateDto(
            cashRegisterId = 1,
            receiptId = "rec-persistent-1",
            fiscalCheckNumber = "0042",
            totalPrice = 125.50,
            paymentType = 0,
            createdAt = 1700000000L,
            items = listOf(
                OrderItemDto(
                    productId = 1,
                    productName = "Хліб",
                    productPrice = 25.50,
                    quantity = 1.0,
                    totalPrice = 25.50
                )
            )
        )

        storage.saveQueue(listOf(order))

        // Створюємо новий екземпляр сховища, читаючи той самий файл
        val reloadedStorage = FileJsonSyncStorage(tempFile)
        val reloadedQueue = reloadedStorage.loadQueue()

        assertEquals(1, reloadedQueue.size)
        assertEquals("rec-persistent-1", reloadedQueue[0].receiptId)
        assertEquals("0042", reloadedQueue[0].fiscalCheckNumber)
        assertEquals(125.50, reloadedQueue[0].totalPrice)
        assertEquals(1, reloadedQueue[0].items.size)
        assertEquals("Хліб", reloadedQueue[0].items[0].productName)

        tempFile.delete()
    }
}
