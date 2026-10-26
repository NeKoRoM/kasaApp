package com.kasa.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopBarcodeScannerWedgeTest {

    @Test
    fun testRapidKeystrokesTriggersBarcodeScan() {
        var scannedBarcode: String? = null
        var simulatedTime = 1000L
        var beepCount = 0

        val wedge = DesktopBarcodeScannerWedge(
            maxIntervalMs = 60L,
            minBarcodeLength = 3,
            timeProvider = { simulatedTime },
            beepProvider = { beepCount++ },
            onBarcodeScanned = { scannedBarcode = it }
        )

        val barcode = "482000000001"
        for (char in barcode) {
            simulatedTime += 10L // 10мс між символами — типова швидкість апаратного лазерного сканера
            val consumed = wedge.onKey(Key.Unknown, KeyEventType.KeyDown, char.code)
            assertFalse(consumed, "Printable character should not be consumed")
        }

        // Завершальний Enter від сканера
        simulatedTime += 10L
        val enterConsumed = wedge.onKey(Key.Enter, KeyEventType.KeyDown, 0)

        assertTrue(enterConsumed, "Enter after valid barcode should be consumed")
        assertEquals("482000000001", scannedBarcode)
        assertEquals(1, beepCount, "Beep should be triggered on successful scan")
    }

    @Test
    fun testSlowTypingResetsBufferAndDoesNotTriggerScan() {
        var scannedBarcode: String? = null
        var simulatedTime = 1000L

        val wedge = DesktopBarcodeScannerWedge(
            maxIntervalMs = 60L,
            minBarcodeLength = 3,
            timeProvider = { simulatedTime },
            onBarcodeScanned = { scannedBarcode = it }
        )

        // Символ 1
        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '1'.code)

        // Пауза 200мс (людина повільно шукає клавішу)
        simulatedTime += 200L

        // Символ 2
        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '2'.code)

        // Символ Enter
        simulatedTime += 10L
        val enterConsumed = wedge.onKey(Key.Enter, KeyEventType.KeyDown, 0)

        assertFalse(enterConsumed)
        assertNull(scannedBarcode)
    }

    @Test
    fun testShortBarcodeIgnoredAndTriggersErrorBeep() {
        var scannedBarcode: String? = null
        var simulatedTime = 1000L
        var errorBeepCount = 0

        val wedge = DesktopBarcodeScannerWedge(
            maxIntervalMs = 60L,
            minBarcodeLength = 4,
            timeProvider = { simulatedTime },
            errorBeepProvider = { errorBeepCount++ },
            onBarcodeScanned = { scannedBarcode = it }
        )

        // Тільки 2 цифри
        simulatedTime += 10L
        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '1'.code)
        simulatedTime += 10L
        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '2'.code)
        simulatedTime += 10L
        val enterConsumed = wedge.onKey(Key.Enter, KeyEventType.KeyDown, 0)

        assertFalse(enterConsumed)
        assertNull(scannedBarcode)
        assertEquals(1, errorBeepCount)
    }

    @Test
    fun testUkrainianLayoutAutoCorrectsToEnglishBarcode() {
        var scannedBarcode: String? = null
        var simulatedTime = 1000L

        val wedge = DesktopBarcodeScannerWedge(
            maxIntervalMs = 60L,
            minBarcodeLength = 3,
            timeProvider = { simulatedTime },
            onBarcodeScanned = { scannedBarcode = it }
        )

        // Символи, що вводяться при увімкненій українській розкладці замість "ABC123"
        val ukChars = "ФИС123"
        for (char in ukChars) {
            simulatedTime += 10L
            val consumed = wedge.onKey(Key.Unknown, KeyEventType.KeyDown, char.code)
            assertFalse(consumed)
        }

        simulatedTime += 10L
        val enterConsumed = wedge.onKey(Key.Enter, KeyEventType.KeyDown, 0)
        assertTrue(enterConsumed)
        assertEquals("ABC123", scannedBarcode)
    }

    @Test
    fun testHotKeyTriggeredWhenBufferEmpty() {
        var hotKeyTriggered = false
        val wedge = DesktopBarcodeScannerWedge(
            onHotKey = { key ->
                if (key == Key.F2) { hotKeyTriggered = true; true } else false
            },
            onBarcodeScanned = {}
        )
        val consumed = wedge.onKey(Key.F2, KeyEventType.KeyDown, 0)
        assertTrue(consumed)
        assertTrue(hotKeyTriggered)
    }

    @Test
    fun testBufferLengthAndManualClear() {
        var simulatedTime = 1000L
        val wedge = DesktopBarcodeScannerWedge(
            timeProvider = { simulatedTime },
            onBarcodeScanned = {}
        )

        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '4'.code)
        simulatedTime += 10L
        wedge.onKey(Key.Unknown, KeyEventType.KeyDown, '8'.code)
        assertEquals(2, wedge.getBufferLength())

        wedge.clear()
        assertEquals(0, wedge.getBufferLength())
    }
}
