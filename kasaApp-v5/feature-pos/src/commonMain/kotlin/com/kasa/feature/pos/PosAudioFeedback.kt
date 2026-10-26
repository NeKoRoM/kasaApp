package com.kasa.feature.pos

/**
 * Аудіо-зворотний зв'язок касового терміналу для мобільних пристроїв та POS-терміналів.
 * Забезпечує звукове підтвердження дій касира без необхідності постійно дивитися на екран:
 * - Сканування штрих-коду товару (короткий чіткий пік 1200 Гц);
 * - Успішна фіскалізація / оплата чека (подвійний мажорний тон);
 * - Помилка / невідомий штрих-код (низький попереджувальний сигнал 350 Гц).
 */
object PosAudioFeedback {

    var isSoundEnabled: Boolean = true

    /**
     * Відтворює стандартний роздрібний "біп" сканера штрих-кодів.
     */
    fun playScanBeep() {
        if (!isSoundEnabled) return
        playToneAsync(frequency = 1200.0, durationMs = 45, volume = 0.6)
    }

    /**
     * Відтворює сигнал успішної оплати або друку фіскального чека.
     */
    fun playSuccessBeep() {
        if (!isSoundEnabled) return
        playToneAsync(frequency = 1500.0, durationMs = 40, volume = 0.5)
        try {
            Thread.sleep(50)
        } catch (_: InterruptedException) { }
        playToneAsync(frequency = 1900.0, durationMs = 60, volume = 0.6)
    }

    /**
     * Відтворює сигнал помилки сканування або відхилення операції.
     */
    fun playErrorBeep() {
        if (!isSoundEnabled) return
        playToneAsync(frequency = 350.0, durationMs = 120, volume = 0.7)
    }

    /**
     * Генерує синусоїдальний тон у фоновому потоці через PCM/AudioSystem (Desktop)
     * або системний біп із повною ізоляцією від відсутності AWT/javax на мобільних ОС.
     */
    private fun playToneAsync(frequency: Double, durationMs: Int, volume: Double) {
        Thread {
            try {
                val sampleRate = 16000f
                val numSamples = ((durationMs / 1000.0) * sampleRate).toInt()
                val buffer = ByteArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                    val sinVal = Math.sin(angle)
                    val envelope = when {
                        i < 50 -> i / 50.0
                        i > numSamples - 50 -> (numSamples - i) / 50.0
                        else -> 1.0
                    }
                    buffer[i] = (sinVal * 127.0 * volume * envelope).toInt().toByte()
                }

                val formatClass = Class.forName("javax.sound.sampled.AudioFormat")
                val systemClass = Class.forName("javax.sound.sampled.AudioSystem")
                val format = formatClass.getConstructor(
                    Float::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType
                ).newInstance(sampleRate, 8, 1, true, false)

                val line = systemClass.getMethod("getSourceDataLine", formatClass).invoke(null, format)
                val lineClass = line.javaClass
                lineClass.getMethod("open", formatClass, Int::class.javaPrimitiveType).invoke(line, format, buffer.size)
                lineClass.getMethod("start").invoke(line)
                lineClass.getMethod("write", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(line, buffer, 0, buffer.size)
                lineClass.getMethod("drain").invoke(line)
                lineClass.getMethod("close").invoke(line)
            } catch (_: Throwable) {
                try {
                    val toolkitClass = Class.forName("java.awt.Toolkit")
                    val toolkit = toolkitClass.getMethod("getDefaultToolkit").invoke(null)
                    toolkitClass.getMethod("beep").invoke(toolkit)
                } catch (_: Throwable) {
                    // Мобільна платформа Android / iOS або headless середовище
                }
            }
        }.start()
    }
}
