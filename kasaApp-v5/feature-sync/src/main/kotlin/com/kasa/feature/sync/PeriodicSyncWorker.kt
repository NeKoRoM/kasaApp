package com.kasa.feature.sync

import com.kasa.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Стан фонового синхронізатора даних із сервером.
 */
data class SyncWorkerState(
    val isRunning: Boolean = false,
    val isFlushing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStatus: String? = null
)

/**
 * Фоновий воркер періодичної синхронізації з бекендом FastAPI.
 * Автоматично відправляє накопичені офлайн-чеки та оновлює локальний каталог товарів.
 */
class PeriodicSyncWorker(
    private val syncQueue: SyncQueueLogic,
    private val syncClient: SyncClient,
    private val scope: CoroutineScope,
    private val onCatalogDownloaded: (List<Product>) -> Unit = {},
    private val syncIntervalMs: Long = 30_000L
) {
    private val _state = MutableStateFlow(SyncWorkerState())
    val state: StateFlow<SyncWorkerState> = _state.asStateFlow()

    private var workerJob: Job? = null

    fun start(token: String) {
        if (workerJob != null) return

        _state.value = _state.value.copy(isRunning = true)
        workerJob = scope.launch {
            while (isActive) {
                performSyncCycle(token)
                delay(syncIntervalMs)
            }
        }
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
        _state.value = _state.value.copy(isRunning = false, isFlushing = false)
    }

    suspend fun performSyncCycle(token: String): FlushSummary {
        _state.value = _state.value.copy(isFlushing = true)
        val summary = syncQueue.flush(token)

        val catalogMsg = when (val catalogResult = syncClient.fetchProducts(token)) {
            is SyncResult.Success -> {
                if (catalogResult.data.isNotEmpty()) {
                    onCatalogDownloaded(catalogResult.data)
                    "Каталог (${catalogResult.data.size} тов.)"
                } else {
                    "Каталог на сервері порожній"
                }
            }
            is SyncResult.Failure -> "Помилка каталогу: ${catalogResult.message}"
        }


        val statusMsg = "Відправлено: ${summary.uploadedCount}, в черзі: ${summary.remainingCount}. $catalogMsg"
        _state.value = _state.value.copy(
            isFlushing = false,
            lastSyncTimestamp = System.currentTimeMillis(),
            lastSyncStatus = statusMsg
        )
        return summary
    }
}
