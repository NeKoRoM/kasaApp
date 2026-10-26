package com.kasa.feature.pos

import com.kasa.model.CashierShift
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тести файлового JSON-сховища касових змін FileJsonShiftStorage.
 */
class FileJsonShiftStorageTest {

    private lateinit var tempFile: File
    private lateinit var storage: FileJsonShiftStorage

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("test_shifts_", ".json")
        tempFile.delete()
        storage = FileJsonShiftStorage(tempFile)
    }

    @AfterTest
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testEmptyStorageReturnsDefaults() {
        assertNull(storage.loadCurrentShift())
        assertTrue(storage.loadHistory().isEmpty())
    }

    @Test
    fun testSaveAndLoadCurrentShift() {
        val shift = CashierShift(
            id = "s-100",
            shiftNumber = 5L,
            cashierId = 1L,
            cashierName = "Марія Петренко",
            openedAt = 1700000000000L,
            initialDrawerBalanceKopecks = 50000L
        )

        storage.saveCurrentShift(shift)

        val loaded = storage.loadCurrentShift()
        assertNotNull(loaded)
        assertEquals("s-100", loaded.id)
        assertEquals(5L, loaded.shiftNumber)
        assertEquals("Марія Петренко", loaded.cashierName)
    }

    @Test
    fun testRecordClosedShiftAppendsToHistoryAndClearsCurrent() {
        val activeShift = CashierShift(
            id = "s-101",
            shiftNumber = 6L,
            cashierId = 2L,
            cashierName = "Олександр Коваленко",
            openedAt = 1700000000000L
        )
        storage.saveCurrentShift(activeShift)

        val closedShift = activeShift.copy(
            closedAt = 1700086400000L,
            isClosed = true,
            zReportNumber = "00055",
            totalSalesKopecks = 150000L
        )

        storage.recordClosedShift(closedShift)

        assertNull(storage.loadCurrentShift())
        val history = storage.loadHistory()
        assertEquals(1, history.size)
        assertEquals("00055", history.first().zReportNumber)
        assertTrue(history.first().isClosed)
    }
}
