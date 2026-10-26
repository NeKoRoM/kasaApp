package com.kasa.feature.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Результат спроби вивантаження черги чеків.
 */
data class FlushSummary(
    val uploadedCount: Int,
    val remainingCount: Int,
    val lastError: SyncError? = null
)

/**
 * Інтерфейс для збереження черги в персистентне сховище (файл/БД/кеш).
 */
interface SyncStoragePersistence {
    fun loadQueue(): List<OrderCreateDto>
    fun saveQueue(queue: List<OrderCreateDto>)
}

/**
 * Реалізація за замовчуванням (у пам'яті для швидкого запуску та тестів).
 */
class InMemorySyncStorage : SyncStoragePersistence {
    private val memoryQueue = mutableListOf<OrderCreateDto>()

    override fun loadQueue(): List<OrderCreateDto> = synchronized(memoryQueue) {
        memoryQueue.toList()
    }

    override fun saveQueue(queue: List<OrderCreateDto>) {
        synchronized(memoryQueue) {
            memoryQueue.clear()
            memoryQueue.addAll(queue)
        }
    }
}

/**
 * Логіка керування офлайн-чергою фіскальних чеків та їх синхронізацією.
 */
class SyncQueueLogic(
    private val syncClient: SyncClient,
    private val persistence: SyncStoragePersistence = InMemorySyncStorage()
) {
    private val mutex = Mutex()
    private val _pendingOrders = MutableStateFlow<List<OrderCreateDto>>(emptyList())
    val pendingOrders: StateFlow<List<OrderCreateDto>> = _pendingOrders.asStateFlow()

    init {
        _pendingOrders.value = persistence.loadQueue()
    }

    suspend fun enqueue(order: OrderCreateDto) {
        mutex.withLock {
            val updated = _pendingOrders.value + order
            _pendingOrders.value = updated
            persistence.saveQueue(updated)
        }
    }

    suspend fun flush(token: String): FlushSummary {
        mutex.withLock {
            val currentList = _pendingOrders.value.toMutableList()
            if (currentList.isEmpty()) {
                return FlushSummary(0, 0, null)
            }

            var uploaded = 0
            var failureReason: SyncError? = null

            val iterator = currentList.iterator()
            while (iterator.hasNext()) {
                val order = iterator.next()
                when (val uploadResult = syncClient.uploadOrder(token, order)) {
                    is SyncResult.Success -> {
                        iterator.remove()
                        uploaded++
                    }
                    is SyncResult.Failure -> {
                        failureReason = uploadResult.error
                        // Зупиняємо відправку при першій помилці мережі чи авторизації
                        break
                    }
                }
            }

            _pendingOrders.value = currentList.toList()
            persistence.saveQueue(currentList.toList())

            return FlushSummary(
                uploadedCount = uploaded,
                remainingCount = currentList.size,
                lastError = failureReason
            )
        }
    }

    val pendingCount: Int
        get() = _pendingOrders.value.size
}
