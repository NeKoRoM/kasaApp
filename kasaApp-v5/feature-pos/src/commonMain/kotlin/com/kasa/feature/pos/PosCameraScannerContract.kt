package com.kasa.feature.pos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Контракт та стан для сканування штрих-кодів через камеру смартфона / планшета.
 */
enum class PosCameraPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

/**
 * Модель результату розпізнавання штрих-коду з камери.
 */
data class PosCameraScanResult(
    val rawValue: String,
    val format: String,
    val timestampMs: Long
)

/**
 * Конфігурація параметрів камери-сканера для роздрібної каси.
 */
data class PosCameraScannerConfig(
    val cooldownSameCodeMs: Long = PosCameraDebounceLogic.DEFAULT_SAME_CODE_COOLDOWN_MS,
    val cooldownAnyCodeMs: Long = PosCameraDebounceLogic.DEFAULT_ANY_CODE_COOLDOWN_MS,
    val isTorchEnabled: Boolean = false,
    val isBatchMode: Boolean = false,
    val isAimLaserVisible: Boolean = true
)

/**
 * Логіка дебаунсу та фільтрації повторних зчитувань камери.
 * Запобігає спаму одного й того ж штрих-коду при наведенні камери (60 FPS).
 */
object PosCameraDebounceLogic {
    const val DEFAULT_SAME_CODE_COOLDOWN_MS = 1800L
    const val DEFAULT_ANY_CODE_COOLDOWN_MS = 500L

    /**
     * Очищає сирий рядок від пробільних символів та переносів рядків,
     * які можуть передаватися камерою або емулятором сканера.
     */
    fun normalizeScannedBarcode(raw: String): String {
        return raw.trim()
            .replace("\r", "")
            .replace("\n", "")
    }

    /**
     * Визначає, чи слід приймати щойно зчитаний камерою штрих-код:
     * - Відхиляє порожні рядки;
     * - Блокує повтори того самого коду протягом [cooldownSameCodeMs];
     * - Тротлить надто швидкі серії зчитувань будь-яких кодів ([cooldownAnyMs]).
     */
    fun shouldAcceptScannedCode(
        code: String,
        lastScannedCode: String?,
        lastScannedTimeMs: Long,
        currentTimeMs: Long,
        cooldownSameCodeMs: Long = DEFAULT_SAME_CODE_COOLDOWN_MS,
        cooldownAnyMs: Long = DEFAULT_ANY_CODE_COOLDOWN_MS
    ): Boolean {
        val clean = normalizeScannedBarcode(code)
        if (clean.isBlank()) {
            return false
        }
        val elapsed = currentTimeMs - lastScannedTimeMs
        if (elapsed < 0L) {
            return true
        }
        // Захист від занадто швидкого повторного виклику будь-якого коду (cooldown)
        if (elapsed < cooldownAnyMs && lastScannedTimeMs > 0L) {
            return false
        }
        // Якщо це той самий код — чекаємо повний період cooldownSameCodeMs
        if (clean == lastScannedCode && elapsed < cooldownSameCodeMs && lastScannedTimeMs > 0L) {
            return false
        }
        return true
    }
}

/**
 * Кросплатформенний віджет огляду камери для розпізнавання штрих-кодів.
 * - Android: CameraX + Google ML Kit Barcode Scanning + PreviewView
 * - Desktop: Інформаційний в'юпорт із підтримкою USB/BT сканера
 */
@Composable
expect fun PosCameraScannerView(
    modifier: Modifier = Modifier,
    isFlashlightOn: Boolean = false,
    isScanningEnabled: Boolean = true,
    onBarcodeDetected: (String) -> Unit
)
