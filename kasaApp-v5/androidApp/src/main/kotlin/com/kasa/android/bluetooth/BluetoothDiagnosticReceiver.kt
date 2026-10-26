package com.kasa.android.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.kasa.feature.pos.PosEvent
import com.kasa.feature.pos.PosViewModel
import com.kasa.protocol.frame.DatecsFrameBuilder
import com.kasa.protocol.status.KasaStatusDecoder
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Сервісний ADB BroadcastReceiver для покрокового апаратного тестування
 * команд протоколу Datecs v2 / Exellio DP-25 у реальному часі.
 *
 * Використання через ADB:
 * `adb shell am broadcast -a com.kasa.pos.ADB_CMD --es cmd "003>"`
 * `adb shell am broadcast -a com.kasa.pos.ADB_CMD --es cmd "00??IDnumber\t\t\t"`
 * `adb shell am broadcast -a com.kasa.pos.ADB_CMD --es action "sync_plu"`
 */
class BluetoothDiagnosticReceiver(
    private val context: Context,
    private val transportProvider: () -> KasaTransport?,
    private val viewModelProvider: () -> PosViewModel?
) : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cmdMutex = kotlinx.coroutines.sync.Mutex()
    private val frameBuilder = DatecsFrameBuilder()
    private var isRegistered = false

    fun register() {
        if (isRegistered) return
        val filter = IntentFilter(ACTION_ADB_CMD)
        ContextCompat.registerReceiver(
            context,
            this,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isRegistered = true
        Log.i(TAG, "BluetoothDiagnosticReceiver registered for $ACTION_ADB_CMD")
    }

    fun unregister() {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(this) }
        isRegistered = false
        scope.cancel()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_ADB_CMD) return

        val actionName = intent.getStringExtra(EXTRA_ACTION)
        if (!actionName.isNullOrBlank()) {
            handleHighLevelAction(actionName)
            return
        }

        val rawCmdArg = intent.getStringExtra(EXTRA_CMD) ?: return
        val timeoutMs = intent.getLongExtra(EXTRA_TIMEOUT_MS, 3000L)
        val unescapedCmd = rawCmdArg
            .replace("_T_", "\t")
            .replace("_GT_", ">")
            .replace("_LT_", "<")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\n", "\n")

        scope.launch {
            cmdMutex.lock()
            try {
                executeRawCommand(unescapedCmd, timeoutMs)
            } finally {
                cmdMutex.unlock()
            }
        }
    }

    private fun handleHighLevelAction(actionName: String) {
        if (actionName == "reconnect") {
            scope.launch {
                cmdMutex.lock()
                try {
                    val transport = transportProvider() ?: return@launch
                    Log.i(TAG, "Reconnecting Bluetooth transport to 98:DA:60:01:1A:D1...")
                    transport.disconnect()
                    kotlinx.coroutines.delay(1500L)
                    val ok = transport.connect("98:DA:60:01:1A:D1")
                    Log.i(TAG, "Reconnect result: $ok")
                } finally {
                    cmdMutex.unlock()
                }
            }
            return
        }
        val vm = viewModelProvider()
        if (vm == null) {
            Log.w(TAG, "ACTION '$actionName' ignored: PosViewModel is null")
            return
        }
        when (actionName) {
            "sync_plu" -> {
                Log.i(TAG, "Triggering PosEvent.StartPluDumpFromKasa")
                vm.onEvent(PosEvent.StartPluDumpFromKasa)
            }
            "sync_time" -> {
                Log.i(TAG, "Triggering PosEvent.SyncKasaTime")
                vm.onEvent(PosEvent.SyncKasaTime)
            }
            "read_info" -> {
                Log.i(TAG, "Triggering PosEvent.QueryKasaHardwareInfo")
                vm.onEvent(PosEvent.QueryKasaHardwareInfo)
            }
            "open_drawer" -> {
                Log.i(TAG, "Triggering PosEvent.OpenCashDrawer")
                vm.onEvent(PosEvent.OpenCashDrawer)
            }
            "x_report" -> {
                Log.i(TAG, "Triggering PosEvent.PrintXReport")
                vm.onEvent(PosEvent.PrintXReport)
            }
            else -> Log.w(TAG, "Unknown action: $actionName")
        }
    }

    private suspend fun executeRawCommand(payload: String, timeoutMs: Long) {
        if (DatecsFrameBuilder.isCommandBlacklisted(payload)) {
            Log.e(TAG, "CMD_BLOCKED_BLACKLIST [$payload]: Заборонена сервісна команда РРО (чорний список)")
            return
        }
        val transport = transportProvider()
        if (transport == null || !transport.isConnected) {
            Log.e(TAG, "CMD_FAIL [$payload]: Transport not connected")
            return
        }

        val frame = frameBuilder.buildFrame(payload)
        val startMs = System.currentTimeMillis()
        val sent = transport.sendFrame(frame)
        if (!sent) {
            Log.e(TAG, "CMD_FAIL [$payload]: sendFrame returned false")
            return
        }

        val responseBytes = transport.receiveFrame(timeoutMs)
        val elapsedMs = System.currentTimeMillis() - startMs
        if (responseBytes == null) {
            Log.w(TAG, "CMD_TIMEOUT (${elapsedMs}ms) [$payload]")
            return
        }

        val decoded = frameBuilder.decodeFrameOrPartial(responseBytes)
        if (decoded == null) {
            Log.e(TAG, "CMD_DECODE_ERR (${elapsedMs}ms) [$payload]: rawSize=${responseBytes.size}")
            return
        }

        val statusSummary = if (decoded.rawPayloadBytes.size >= 6) {
            val st = KasaStatusDecoder.extractFromPayload(decoded.rawPayloadBytes)
            "syntaxErr=${st.hasSyntaxError}, invalidCmd=${st.isInvalidCommand}, notAllowed=${st.isCommandNotAllowed}, paperOut=${st.isReceiptPaperOut}, coverOpen=${st.isCoverOpen}, shift24h=${st.is24HoursShiftExpired}, receiptOpen=${st.isFiscalReceiptOpen}"
        } else {
            "no-status"
        }

        val printableText = decoded.payloadText
            .substringBefore('\u0004')
            .replace("\t", "\\t")
        Log.i(
            TAG,
            "CMD_OK (${elapsedMs}ms, bcc=${decoded.isBccValid}) TX=[${payload.replace("\t", "\\t")}] -> RX=[$printableText] | STATUS=[$statusSummary]"
        )
    }

    companion object {
        private const val TAG = "BtKasaTest"
        const val ACTION_ADB_CMD = "com.kasa.pos.ADB_CMD"
        const val EXTRA_CMD = "cmd"
        const val EXTRA_ACTION = "action"
        const val EXTRA_TIMEOUT_MS = "timeout"
    }
}
