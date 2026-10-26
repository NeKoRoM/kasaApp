package com.kasa.feature.sync

import com.kasa.model.Product
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodicSyncWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testPeriodicSyncCycleUploadsOrdersAndUpdatesCatalog() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/orders/" -> respond(
                    content = """{"status":"ok"}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/products/" -> respond(
                    content = """
                        [
                            {"product_id":1,"name":"Хліб","price":24.0,"barcode_1":"482000001"},
                            {"product_id":2,"name":"Молоко","price":38.0,"barcode_1":"482000002"}
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val syncClient = SyncClient("http://localhost:8000", mockEngine)
        val syncQueue = SyncQueueLogic(syncClient)

        // Додаємо одне замовлення в чергу
        val order = OrderCreateDto(
            cashRegisterId = 1,
            receiptId = "rec-101",
            totalPrice = 50.0,
            items = emptyList()
        )
        syncQueue.enqueue(order)
        assertEquals(1, syncQueue.pendingCount)

        var downloadedCatalog: List<Product>? = null
        val worker = PeriodicSyncWorker(
            syncQueue = syncQueue,
            syncClient = syncClient,
            scope = testScope,
            onCatalogDownloaded = { downloadedCatalog = it }
        )

        // Запуск одного циклу синхронізації
        val summary = worker.performSyncCycle("auth-token-xyz")

        assertEquals(1, summary.uploadedCount)
        assertEquals(0, summary.remainingCount)
        assertEquals(0, syncQueue.pendingCount)

        assertNotNull(downloadedCatalog)
        assertEquals(2, downloadedCatalog!!.size)
        assertEquals("Хліб", downloadedCatalog!![0].name)

        val state = worker.state.value
        assertFalse(state.isFlushing)
        assertNotNull(state.lastSyncStatus)
        assertTrue(state.lastSyncStatus!!.contains("Відправлено: 1"))
        assertTrue(state.lastSyncStatus!!.contains("Каталог (2 тов.)"))
    }

    @Test
    fun testStartAndStopLifecycle() = runTest {
        val mockEngine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val syncClient = SyncClient("http://localhost:8000", mockEngine)
        val syncQueue = SyncQueueLogic(syncClient)

        val worker = PeriodicSyncWorker(
            syncQueue = syncQueue,
            syncClient = syncClient,
            scope = testScope
        )

        assertFalse(worker.state.value.isRunning)
        worker.start("auth-token")
        assertTrue(worker.state.value.isRunning)

        worker.stop()
        assertFalse(worker.state.value.isRunning)
    }
}
