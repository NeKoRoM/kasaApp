package com.kasa.transport

import com.kasa.protocol.scale.ScaleProtocolParser
import com.kasa.protocol.scale.ScaleWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

/**
 * Транспортний клієнт зчитування безперервного потоку показань торгових ваг
 * через послідовний COM-порт або Bluetooth SPP потік.
 */
class ScaleStreamTransport(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _currentWeight = MutableStateFlow<ScaleWeight?>(null)
    val currentWeight: StateFlow<ScaleWeight?> = _currentWeight.asStateFlow()

    private var outputStream: OutputStream? = null
    private var listeningJob: Job? = null

    /**
     * Запускає фонове зчитування рядків із вхідного потоку ваг.
     */
    fun startListening(input: InputStream, output: OutputStream) {
        stopListening()
        outputStream = output
        listeningJob = coroutineScope.launch {
            val reader = BufferedReader(InputStreamReader(input, Charsets.US_ASCII))
            while (isActive) {
                try {
                    val line = reader.readLine() ?: break
                    val parsed = ScaleProtocolParser.parse(line)
                    if (parsed != null) {
                        _currentWeight.value = parsed
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    /**
     * Відправляє команду тарування (обнулення поточної ваги тари).
     */
    suspend fun tare(): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext false
        try {
            out.write(ScaleProtocolParser.TARE_COMMAND_BYTES)
            out.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Зупиняє зчитування та звільняє ресурси.
     */
    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        outputStream = null
    }
}
