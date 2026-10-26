package com.kasa.feature.pos

import com.kasa.model.Product
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.fiscal.ServiceResult
import com.kasa.protocol.plu.FiscalPluEngine
import com.kasa.protocol.plu.FiscalPluResult
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Асинхронний координатор сервісних функцій РРО, годинника RTC, дисплея покупця та синхронізації PLU (Service Operations Coordinator).
 */
class PosServiceCoordinator(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<PosState>,
    private val effect: Channel<PosEffect>,
    private val transport: KasaTransport,
    private val serviceEngine: FiscalServiceEngine,
    private val pluEngine: FiscalPluEngine,
    private val onCatalogUpdated: (suspend (List<Product>) -> Unit)? = null
) {

    fun printXReport() {
        scope.launch {
            when (val res = serviceEngine.printXReport()) {
                is ServiceResult.Success -> effect.send(PosEffect.ShowToast(res.message))
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Звіт", res.message))
            }
        }
    }

    fun syncKasaTime() {
        val formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"))
        scope.launch {
            when (val res = serviceEngine.setDateTime(formatted)) {
                is ServiceResult.Success -> {
                    state.update { it.copy(kasaRtcTime = formatted, statusMessage = res.message) }
                    effect.send(PosEffect.ShowToast(res.message))
                }
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Синхронізація часу", res.message))
            }
        }
    }

    fun readKasaTime() {
        scope.launch {
            when (val res = serviceEngine.readDateTime()) {
                is ServiceResult.Success -> {
                    state.update { it.copy(kasaRtcTime = res.payload, statusMessage = "Час РРО: ${res.payload}") }
                    effect.send(PosEffect.ShowToast("Час РРО: ${res.payload}"))
                }
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Годинник РРО", res.message))
            }
        }
    }

    fun queryKasaHardwareInfo() {
        scope.launch {
            when (val res = serviceEngine.getKasaId()) {
                is ServiceResult.Success -> {
                    state.update { it.copy(kasaHardwareInfo = res.payload, statusMessage = "РРО: ${res.payload}") }
                    effect.send(PosEffect.ShowToast("Інфо: ${res.payload}"))
                }
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Інфо РРО", res.message))
            }
        }
    }

    fun openCashDrawer() {
        scope.launch {
            when (val res = serviceEngine.openDrawer()) {
                is ServiceResult.Success -> effect.send(PosEffect.ShowToast(res.message))
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Скринька", res.message))
            }
        }
    }

    fun voidReceipt() {
        scope.launch {
            when (val res = serviceEngine.voidReceipt()) {
                is ServiceResult.Success -> {
                    state.update {
                        it.copy(statusMessage = res.message, kasaStatus = it.kasaStatus?.copy(isFiscalReceiptOpen = false))
                    }
                    effect.send(PosEffect.ShowToast(res.message))
                }
                is ServiceResult.Failure -> effect.send(PosEffect.ShowErrorDialog("Анулювання чека", res.message))
            }
        }
    }

    fun checkKasaHealth() {
        if (transport.isScannerRegistrationMode || state.value.isPluSyncing || state.value.isFiscalProcessing) return
        scope.launch {
            val status = serviceEngine.queryStatus()
            if (status != null) {
                state.update { it.copy(kasaStatus = status) }
            }
        }
    }

    fun startPluDump() {
        if (state.value.isPluSyncing || state.value.isFiscalProcessing) return
        scope.launch {
            state.update { it.copy(isPluSyncing = true, pluSyncStatusMessage = "Зчитування бази РРО...") }
            val result = pluEngine.dumpAllPlu { progress -> state.update { it.copy(pluSyncProgress = progress) } }
            when (result) {
                is FiscalPluResult.Success -> {
                    val downloaded = result.data
                    val message = if (downloaded.isNotEmpty()) "Успішно завантажено ${downloaded.size} товарів з РРО" else "База артикулів у РРО порожня"
                    state.update {
                        it.copy(
                            isPluSyncing = false,
                            catalogProducts = if (downloaded.isNotEmpty()) downloaded else it.catalogProducts,
                            pluSyncStatusMessage = message
                        )
                    }
                    if (downloaded.isNotEmpty()) onCatalogUpdated?.invoke(downloaded)
                }
                is FiscalPluResult.Failure -> {
                    state.update { it.copy(isPluSyncing = false, pluSyncStatusMessage = "Помилка дампу: ${result.message}") }
                }
            }
        }
    }

    fun startPluUpload() {
        if (state.value.isPluSyncing || state.value.isFiscalProcessing) return
        scope.launch {
            val catalog = state.value.catalogProducts
            state.update { it.copy(isPluSyncing = true, pluSyncStatusMessage = "Запис товарів у пам'ять РРО...") }
            val result = pluEngine.uploadCatalog(catalog) { progress -> state.update { it.copy(pluSyncProgress = progress) } }
            when (result) {
                is FiscalPluResult.Success -> {
                    state.update {
                        it.copy(isPluSyncing = false, pluSyncStatusMessage = "Успішно запрограмовано ${result.data} з ${catalog.size} товарів")
                    }
                }
                is FiscalPluResult.Failure -> {
                    state.update { it.copy(isPluSyncing = false, pluSyncStatusMessage = "Помилка програмування: ${result.message}") }
                }
            }
        }
    }

    fun displayCustomerText(line1: String, line2: String) {
        val l1 = line1.take(20)
        val l2 = line2.take(20)
        state.update { it.copy(customerDisplayLine1 = l1, customerDisplayLine2 = l2) }
        if (transport.isScannerRegistrationMode || state.value.isPluSyncing || state.value.isFiscalProcessing) return
        scope.launch {
            if (l1.isNotBlank()) serviceEngine.displayText(1, l1)
            if (l2.isNotBlank()) serviceEngine.displayText(2, l2)
        }
    }

    fun clearCustomerDisplay() {
        state.update { it.copy(customerDisplayLine1 = "", customerDisplayLine2 = "") }
        if (transport.isScannerRegistrationMode || state.value.isPluSyncing || state.value.isFiscalProcessing) return
        scope.launch { serviceEngine.clearDisplay() }
    }
}
