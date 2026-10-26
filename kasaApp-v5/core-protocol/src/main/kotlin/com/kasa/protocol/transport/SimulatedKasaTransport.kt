package com.kasa.protocol.transport

import com.kasa.protocol.frame.DatecsFrameBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Програмний емулятор фіскального реєстратора Exellio / Datecs.
 * Дозволяє тестувати чековий пайплайн та UI на 100% без фізичного обладнання.
 */
class SimulatedKasaTransport(
    private val frameBuilder: DatecsFrameBuilder = DatecsFrameBuilder()
) : KasaTransport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val incomingQueue = ConcurrentLinkedQueue<ByteArray>()

    // Симуляція апаратних станів
    var simulatePaperOut: Boolean = false
    var simulateCoverOpen: Boolean = false
    var simulateShiftExpired: Boolean = false
    var simulateTimeout: Boolean = false
    var simulateCommandError: Boolean = false
    var simulateBccError: Boolean = false
    var simulateCustomerDisplayConnected: Boolean = true

    var isReceiptOpen: Boolean = false
        private set
    var lastFiscalCheckNumber: Int = 1001
        private set

    val simulatedPluTable = java.util.concurrent.ConcurrentHashMap<Long, String>().apply {
        put(1L, "1\tА\t1\t1\t0\t24.50\t0.00\t0.00\t10.00\t482000000001\t\t\t\tХліб білий\t0\t\tшт\t")
        put(2L, "2\tА\t1\t1\t0\t38.00\t0.00\t0.00\t15.00\t482000000002\t\t\t\tМолоко 2.5%\t0\t\tшт\t")
        put(3L, "3\tА\t1\t1\t0\t150.00\t0.00\t0.00\t5.00\t482000000003\t\t\t\tГорілка Преміум\t0\t2208600000\tшт\t")
    }
    private var lastReadPluId: Long = 0L

    val simulatedHeaderLines = java.util.concurrent.ConcurrentHashMap<Int, String>().apply {
        put(0, "ТОВ \"РОЗДРІБ ТРЕЙД\"")
        put(1, "м. Київ, вул. Хрещатик, 1")
        put(2, "ПН 123456789012  ФН 12345678")
    }

    val simulatedDisplayLines = java.util.concurrent.ConcurrentHashMap<Int, String>()
    private var nextDisplayLine: Int = 1

    override suspend fun connect(target: String): Boolean {
        _state.value = TransportState.Connecting(target)
        _state.value = TransportState.Connected(target)
        return true
    }

    override suspend fun sendFrame(frame: ByteArray): Boolean {
        if (!isConnected) return false
        if (simulateTimeout) return true

        val decoded = frameBuilder.decodeFrame(frame) ?: return false
        val responsePayload = processCommand(decoded.payloadText)

        val statusBytes = buildStatusBytes()
        val fullResponse = "$responsePayload\t${String(statusBytes, Charsets.ISO_8859_1)}"

        val responseFrame = if (simulateBccError) {
            val normalFrame = frameBuilder.buildFrame(fullResponse, decoded.sequence)
            normalFrame[normalFrame.size - 2] = (normalFrame[normalFrame.size - 2] + 1).toByte()
            normalFrame
        } else {
            frameBuilder.buildFrame(fullResponse, decoded.sequence)
        }

        incomingQueue.add(responseFrame)
        return true
    }

    override suspend fun receiveFrame(timeoutMs: Long): ByteArray? {
        if (!isConnected || simulateTimeout) return null
        return incomingQueue.poll()
    }

    override suspend fun disconnect() {
        incomingQueue.clear()
        _state.value = TransportState.Disconnected
    }

    private fun processCommand(payload: String): String {
        if (simulateCommandError) return "ERROR\t-1"

        return when {
            payload.startsWith("003>") -> "003>25-09-26 12:00:00"
            payload.startsWith("003=") -> "003=0"
            payload.startsWith("002?") -> {
                val text = payload.removePrefix("002?").substringBefore('\t')
                if (text.isBlank()) {
                    simulatedDisplayLines.clear()
                    nextDisplayLine = 1
                } else {
                    simulatedDisplayLines[nextDisplayLine] = text
                    nextDisplayLine = if (nextDisplayLine == 1) 2 else 1
                }
                "002?0"
            }
            payload.startsWith("0035") -> {
                val parts = payload.split('\t')
                val lineIdx = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val text = parts.getOrNull(2) ?: ""
                if (lineIdx == 0) {
                    simulatedDisplayLines.clear()
                } else {
                    simulatedDisplayLines[lineIdx] = text
                }
                "00350"
            }
            payload.startsWith("0045X") -> "0045X0"
            payload.startsWith("0045Z") -> {
                simulateShiftExpired = false
                "0045Z0"
            }
            payload.startsWith("0046") -> "00460"
            payload.startsWith("0047") -> "00470"
            payload.startsWith("0050") -> "00500"
            payload.startsWith("005:") -> "005:0\tDP-25\t684008\t14Jun21\t1500\t4072\t00000000\tDP05123456\t12345678\t"
            payload.startsWith("006:") -> "006:0"
            payload.startsWith("0048") -> {
                isReceiptOpen = true
                "00480"
            }
            payload.startsWith("0049") -> "00490"
            payload.startsWith("0051") -> "00510"
            payload.startsWith("0053") -> "00530\tА:20.00\tБ:0.00\tВ:7.00"
            payload.startsWith("0054") -> "00540"
            payload.startsWith("0056") -> {
                isReceiptOpen = false
                lastFiscalCheckNumber++
                "00560\t$lastFiscalCheckNumber"
            }
            payload.startsWith("0060") -> {
                isReceiptOpen = false
                "00600"
            }
            payload.startsWith("006;L") -> {
                val maxId = simulatedPluTable.keys.maxOrNull() ?: 0L
                "006;L0\t$maxId\t"
            }
            payload.startsWith("006;F") -> {
                val reqId = payload.substringAfter("006;F\t").substringBefore('\t').toLongOrNull() ?: 1L
                val entry = simulatedPluTable.entries.filter { it.key >= reqId }.minByOrNull { it.key }
                if (entry != null) {
                    lastReadPluId = entry.key
                    "006;F0\t${entry.value}"
                } else {
                    "006;-111004"
                }
            }
            payload.startsWith("006;N") -> {
                val nextEntry = simulatedPluTable.entries.filter { it.key > lastReadPluId }.minByOrNull { it.key }
                if (nextEntry != null) {
                    lastReadPluId = nextEntry.key
                    "006;N0\t${nextEntry.value}"
                } else {
                    "006;-111004"
                }
            }
            payload.startsWith("006;R") -> {
                val reqId = payload.substringAfter("006;R\t").substringBefore('\t').toLongOrNull() ?: 0L
                val item = simulatedPluTable[reqId]
                if (item != null) "006;R0\t$item" else "006;-111004"
            }
            payload.startsWith("006;D") -> {
                val reqId = payload.substringAfter("006;D\t").substringBefore('\t').toLongOrNull() ?: 0L
                simulatedPluTable.remove(reqId)
                "006;D0"
            }
            payload.startsWith("006;P") -> {
                val data = payload.substringAfter("006;P\t")
                val reqId = data.substringBefore('\t').toLongOrNull() ?: 0L
                if (reqId > 0L) {
                    simulatedPluTable[reqId] = data
                    "006;P0"
                } else {
                    "006;-1"
                }
            }
            payload.startsWith("00??Header") -> {
                val parts = payload.split('\t')
                val lineIdx = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val newText = parts.getOrNull(2)
                if (newText != null && newText.isNotEmpty()) {
                    simulatedHeaderLines[lineIdx] = newText
                    "00??Header0\t$lineIdx\t"
                } else {
                    val lineText = simulatedHeaderLines[lineIdx] ?: ""
                    "00??Header0\t$lineIdx\t$lineText\t"
                }
            }
            payload.startsWith("00??") -> "00??IDnumber\tDP05123456\tФН12345678"
            else -> "0"
        }
    }

    private fun buildStatusBytes(): ByteArray {
        val b0 = 0x40 or (if (simulateCommandError) 0x02 else 0x00)
        val b1 = 0x40 or (if (simulatePaperOut) 0x02 else 0x00) or (if (simulateCoverOpen) 0x04 else 0x00)
        val b2 = 0x40 or 0x01 or (if (simulateShiftExpired) 0x08 else 0x00)
        val b3 = 0x40
        val b4 = 0x40 or (if (isReceiptOpen) 0x02 else 0x00) or (if (simulateCustomerDisplayConnected) 0x04 else 0x00)
        val b5 = 0x40

        return byteArrayOf(
            b0.toByte(),
            b1.toByte(),
            b2.toByte(),
            b3.toByte(),
            b4.toByte(),
            b5.toByte()
        )
    }
}
