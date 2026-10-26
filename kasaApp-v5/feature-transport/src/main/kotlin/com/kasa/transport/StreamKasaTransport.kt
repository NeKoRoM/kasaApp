package com.kasa.transport

import com.kasa.protocol.frame.DatecsByteFramer
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Універсальний потік-адаптер для взаємодії з РРО через стандартні
 * вхідні/вихідні потоки (Bluetooth SPP, Serial COM-порт, TCP).
 */
class StreamKasaTransport(
    private var inputStream: InputStream? = null,
    private var outputStream: OutputStream? = null,
    private val targetName: String = "stream"
) : KasaTransport {

    private val _state = MutableStateFlow<TransportState>(
        if (inputStream != null && outputStream != null) {
            TransportState.Connected(targetName)
        } else {
            TransportState.Disconnected
        }
    )
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val framer = DatecsByteFramer()
    private val framesQueue = mutableListOf<ByteArray>()
    private val sendMutex = Mutex()
    private val receiveMutex = Mutex()
    @Volatile
    private var readerThread: Thread? = null
    @Volatile
    private var lastByteReceivedAtMs: Long = 0L

    init {
        val initialIn = inputStream
        if (initialIn != null && outputStream != null) {
            startReaderThread(initialIn)
        }
    }

    fun attachStreams(input: InputStream, output: OutputStream, target: String) {
        stopReaderThread()
        framer.clear()
        synchronized(framesQueue) { framesQueue.clear() }
        lastByteReceivedAtMs = 0L
        inputStream = input
        outputStream = output
        _state.value = TransportState.Connected(target)
        startReaderThread(input)
    }

    private fun startReaderThread(input: InputStream) {
        stopReaderThread()
        val thread = Thread({
            val buffer = ByteArray(4096)
            while (!Thread.currentThread().isInterrupted && inputStream === input) {
                try {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    if (bytesRead > 0) {
                        lastByteReceivedAtMs = System.currentTimeMillis()
                        val newFrames = framer.appendAndExtractFrames(buffer.copyOf(bytesRead))
                        if (newFrames.isNotEmpty()) {
                            synchronized(framesQueue) {
                                framesQueue.addAll(newFrames)
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (!Thread.currentThread().isInterrupted && inputStream === input) {
                        _state.value = TransportState.Error("Stream read error: ${e.message}")
                    }
                    break
                }
            }
        }, "KasaStreamReader-$targetName")
        thread.isDaemon = true
        readerThread = thread
        thread.start()
    }

    private fun stopReaderThread() {
        readerThread?.interrupt()
        readerThread = null
    }

    override suspend fun connect(target: String): Boolean {
        return isConnected
    }

    override suspend fun sendFrame(frame: ByteArray): Boolean = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val out = outputStream ?: run {
                _state.value = TransportState.Error("Output stream is null")
                return@withContext false
            }
            try {
                framer.clear()
                synchronized(framesQueue) { framesQueue.clear() }
                out.write(frame)
                out.flush()
                true
            } catch (e: IOException) {
                _state.value = TransportState.Error("Send error: ${e.message}")
                false
            }
        }
    }

    override suspend fun receiveFrame(timeoutMs: Long): ByteArray? = withContext(Dispatchers.IO) {
        receiveMutex.withLock {
            val startMs = System.currentTimeMillis()
            withTimeoutOrNull(MAX_SYN_WAIT_MS.coerceAtLeast(timeoutMs)) {
                while (isActive) {
                    synchronized(framesQueue) {
                        if (framesQueue.isNotEmpty()) {
                            return@withTimeoutOrNull framesQueue.removeAt(0)
                        }
                    }
                    val now = System.currentTimeMillis()
                    if (framer.hasPendingFrame()) {
                        val silenceMs = now - lastByteReceivedAtMs
                        if (lastByteReceivedAtMs > 0L && silenceMs >= PARTIAL_FRAME_SILENCE_MS) {
                            val partial = framer.flushIncompleteFrame()
                            if (partial != null) {
                                return@withTimeoutOrNull partial
                            }
                        }
                    }
                    if (inputStream == null || _state.value is TransportState.Error) {
                        return@withTimeoutOrNull null
                    }
                    val elapsedMs = now - startMs
                    val isPrinterBusySyn = lastByteReceivedAtMs >= startMs && (now - lastByteReceivedAtMs) < SYN_ACTIVE_WINDOW_MS
                    if (elapsedMs >= timeoutMs && !isPrinterBusySyn) {
                        return@withTimeoutOrNull null
                    }
                    delay(15)
                }
                null
            }
        }
    }

    companion object {
        private const val PARTIAL_FRAME_SILENCE_MS = 320L
        private const val SYN_ACTIVE_WINDOW_MS = 650L
        private const val MAX_SYN_WAIT_MS = 15_000L
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        stopReaderThread()
        try {
            inputStream?.close()
        } catch (_: IOException) {}
        try {
            outputStream?.close()
        } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        framer.clear()
        synchronized(framesQueue) { framesQueue.clear() }
        _state.value = TransportState.Disconnected
    }
}
