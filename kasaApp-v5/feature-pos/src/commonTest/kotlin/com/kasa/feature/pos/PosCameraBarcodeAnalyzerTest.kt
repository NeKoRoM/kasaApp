package com.kasa.feature.pos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тести для логіки аналізатора камери та дебаунсингу штрих-кодів (PosCameraDebounceLogic).
 */
class PosCameraBarcodeAnalyzerTest {

    @Test
    fun testNormalizeScannedBarcode_cleansWhitespaceAndNewlines() {
        val raw = "  4820000001011 \r\n "
        val cleaned = PosCameraDebounceLogic.normalizeScannedBarcode(raw)
        assertEquals("4820000001011", cleaned)
    }

    @Test
    fun testShouldAcceptScannedCode_rejectsBlankCodes() {
        val accepted = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = "   \n\r  ",
            lastScannedCode = null,
            lastScannedTimeMs = 0L,
            currentTimeMs = 1000L
        )
        assertFalse(accepted)
    }

    @Test
    fun testShouldAcceptScannedCode_acceptsFirstValidCode() {
        val accepted = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = "4820000001011",
            lastScannedCode = null,
            lastScannedTimeMs = 0L,
            currentTimeMs = 10_000L
        )
        assertTrue(accepted)
    }

    @Test
    fun testShouldAcceptScannedCode_debouncesSameCodeWithinCooldown() {
        val code = "4820000001011"
        val firstTime = 10_000L

        // Наступний кадр камери через 100 мс (той самий товар у прицілі)
        val accepted100ms = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = code,
            lastScannedCode = code,
            lastScannedTimeMs = firstTime,
            currentTimeMs = firstTime + 100L
        )
        assertFalse(accepted100ms)

        // Наступний кадр через 1000 мс (все ще в межах 1800 мс за замовчуванням)
        val accepted1000ms = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = code,
            lastScannedCode = code,
            lastScannedTimeMs = firstTime,
            currentTimeMs = firstTime + 1000L
        )
        assertFalse(accepted1000ms)

        // Після закінчення кулдауну (2000 мс > 1800 мс) повторне сканування дозволено
        val accepted2000ms = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = code,
            lastScannedCode = code,
            lastScannedTimeMs = firstTime,
            currentTimeMs = firstTime + 2000L
        )
        assertTrue(accepted2000ms)
    }

    @Test
    fun testShouldAcceptScannedCode_allowsDifferentCodeAfterMinimumInterval() {
        val codeA = "4820000001011"
        val codeB = "4820000002025"
        val firstTime = 10_000L

        // Код B прийшов занадто швидко (< 500 мс) — захист від брязкоту
        val tooFast = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = codeB,
            lastScannedCode = codeA,
            lastScannedTimeMs = firstTime,
            currentTimeMs = firstTime + 200L
        )
        assertFalse(tooFast)

        // Код B прийшов через 600 мс (> 500 мс) — це інший товар, дозволяємо
        val acceptedDifferent = PosCameraDebounceLogic.shouldAcceptScannedCode(
            code = codeB,
            lastScannedCode = codeA,
            lastScannedTimeMs = firstTime,
            currentTimeMs = firstTime + 600L
        )
        assertTrue(acceptedDifferent)
    }

    @Test
    fun testSimulatedBatchScanningStream() {
        val stream = listOf(
            Triple("4820000001011", 1000L, true),   // Товар 1
            Triple("4820000001011", 1050L, false),  // Дубль кадру
            Triple("4820000001011", 1300L, false),  // Дубль кадру
            Triple("4820000002025", 1700L, true),   // Товар 2 (інший код, пройшло 700мс)
            Triple("4820000002025", 1750L, false),  // Дубль товару 2
            Triple("https://t.gov.ua/AA123", 2400L, true) // Акцизна марка
        )

        var lastCode: String? = null
        var lastTime = 0L
        val acceptedCodes = mutableListOf<String>()

        for ((code, timestamp, expected) in stream) {
            val result = PosCameraDebounceLogic.shouldAcceptScannedCode(
                code = code,
                lastScannedCode = lastCode,
                lastScannedTimeMs = lastTime,
                currentTimeMs = timestamp
            )
            assertEquals(expected, result, "Mismatch at timestamp $timestamp for $code")
            if (result) {
                lastCode = PosCameraDebounceLogic.normalizeScannedBarcode(code)
                lastTime = timestamp
                acceptedCodes.add(lastCode)
            }
        }

        assertEquals(listOf("4820000001011", "4820000002025", "https://t.gov.ua/AA123"), acceptedCodes)
    }
}
