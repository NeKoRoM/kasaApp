package com.kasa.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.kasa.model.KeyboardLayoutCorrector

/**
 * Перехоплювач глобальних клавіатурних подій від апаратних USB/Bluetooth сканерів штрих-коду.
 * Сканери штрих-коду емулюють роботу клавіатури (HID Keyboard Wedge), надсилаючи символи
 * із надзвичайно малим інтервалом (< 50–65 мс) та завершуючи зчитування клавішею Enter (CR/LF).
 *
 * Особливості десктопної нативності:
 * - Автоматичне звукове підтвердження успішного сканування (native audio beep);
 * - Двостороння авто-корекція розкладки клавіатури (Кирилиця -> English);
 * - Захист від поглинання звичайного користувацького набору тексту в полі вводу;
 * - Підтримка функціональних гарячих клавіш касира (F1..F12, Escape).
 */
class DesktopBarcodeScannerWedge(
    private val maxIntervalMs: Long = 65L,
    private val minBarcodeLength: Int = 3,
    private val timeProvider: () -> Long = System::currentTimeMillis,
    private val beepProvider: () -> Unit = {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep()
        } catch (_: Throwable) {
            // Безпечний фолбек при роботі в середовищі без AWT Desktop Toolkit
        }
    },
    private val errorBeepProvider: (() -> Unit)? = null,
    private val onHotKey: ((Key) -> Boolean)? = null,
    private val onBarcodeScanned: (String) -> Unit
) {
    private val buffer = StringBuilder()
    private var lastKeyTimestampMs: Long = 0L

    fun onKeyEvent(event: KeyEvent): Boolean {
        return onKey(
            key = event.key,
            type = event.type,
            utf16CodePoint = event.utf16CodePoint
        )
    }

    fun onKey(key: Key, type: KeyEventType, utf16CodePoint: Int): Boolean {
        if (type != KeyEventType.KeyDown) return false

        // Обробка швидких клавіш касира (F1..F12, Escape) за умови порожнього буфера сканера
        if (buffer.isEmpty() && onHotKey != null && onHotKey.invoke(key)) {
            return true
        }

        val now = timeProvider()
        val elapsed = now - lastKeyTimestampMs

        // Якщо затримка між клавішами завелика — це ручний набір оператора, скидаємо буфер сканера
        if (buffer.isNotEmpty() && elapsed > maxIntervalMs) {
            buffer.clear()
        }

        if (key == Key.Enter || key == Key.NumPadEnter) {
            if (buffer.length >= minBarcodeLength) {
                val rawBarcode = buffer.toString()
                buffer.clear()
                val converted = KeyboardLayoutCorrector.toEnglish(rawBarcode)

                // Нативний звуковий сигнал успішного зчитування
                beepProvider.invoke()
                onBarcodeScanned(converted)
                return true // Поглинаємо Enter, щоб запобігти випадковим клікам кнопок у фокусі
            }

            // Якщо буфер був надто коротким для штрих-коду
            if (buffer.isNotEmpty()) {
                errorBeepProvider?.invoke()
                buffer.clear()
            }
            return false
        }

        if (utf16CodePoint >= 32 && !Character.isISOControl(utf16CodePoint)) {
            buffer.append(utf16CodePoint.toChar())
            lastKeyTimestampMs = now
            return false // Дозволяємо проходження символів до активного поля введення
        }

        return false
    }

    /**
     * Повертає поточну кількість накопичених у буфері сканера символів.
     */
    fun getBufferLength(): Int = buffer.length

    /**
     * Примусове очищення буфера (наприклад, при зміні фокусу або закритті вікна).
     */
    fun clear() {
        buffer.clear()
    }
}
