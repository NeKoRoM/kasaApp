package com.kasa.transport

import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Динамічний перемикач транспорту РРО між симулятором та фізичним обладнанням (Bluetooth / COM).
 * Дозволяє гаряче перемикання каналів зв'язку без перезапуску касового терміналу.
 */
class SwitchableKasaTransport(
    initialDelegate: KasaTransport,
    private val scope: CoroutineScope
) : KasaTransport {

    var currentDelegate: KasaTransport = initialDelegate
        private set

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    override val isConnected: Boolean
        get() = currentDelegate.isConnected

    override val isScannerRegistrationMode: Boolean
        get() = currentDelegate.isScannerRegistrationMode

    private var stateForwardingJob: Job? = null

    init {
        forwardState()
    }

    /**
     * Відключає поточний транспорт та перемикає активний канал на новий делегат.
     */
    suspend fun switchTo(newDelegate: KasaTransport) {
        currentDelegate.disconnect()
        currentDelegate = newDelegate
        forwardState()
    }

    private fun forwardState() {
        stateForwardingJob?.cancel()
        stateForwardingJob = scope.launch {
            currentDelegate.state.collect { st ->
                _state.value = st
            }
        }
    }

    override suspend fun connect(target: String): Boolean {
        return currentDelegate.connect(target)
    }

    override suspend fun sendFrame(frame: ByteArray): Boolean {
        return currentDelegate.sendFrame(frame)
    }

    override suspend fun receiveFrame(timeoutMs: Long): ByteArray? {
        return currentDelegate.receiveFrame(timeoutMs)
    }

    override suspend fun disconnect() {
        currentDelegate.disconnect()
    }
}
