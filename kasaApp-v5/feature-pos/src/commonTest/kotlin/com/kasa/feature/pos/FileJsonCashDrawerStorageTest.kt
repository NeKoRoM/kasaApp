package com.kasa.feature.pos

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FileJsonCashDrawerStorageTest {

    @Test
    fun testSaveAndLoadDrawerState() {
        val tempFile = File.createTempFile("test_drawer", ".json")
        tempFile.deleteOnExit()

        val storage = FileJsonCashDrawerStorage(tempFile)
        val initial = storage.loadDrawerState()
        assertEquals(0L, initial.balanceKopecks)

        val newState = CashDrawerState(balanceKopecks = 150000L, lastUpdatedTimestamp = 1700000000L)
        storage.saveDrawerState(newState)

        val reloaded = FileJsonCashDrawerStorage(tempFile).loadDrawerState()
        assertEquals(150000L, reloaded.balanceKopecks)
        assertEquals(1700000000L, reloaded.lastUpdatedTimestamp)

        tempFile.delete()
    }
}
