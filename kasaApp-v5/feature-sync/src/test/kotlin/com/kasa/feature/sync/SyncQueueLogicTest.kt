package com.kasa.feature.sync

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncQueueLogicTest {

    @Test
    fun testEnqueueAndFlushAllSuccess() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.Created
            )
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val queueLogic = SyncQueueLogic(client)

        val order1 = createDummyOrder("rec-1")
        val order2 = createDummyOrder("rec-2")

        queueLogic.enqueue(order1)
        queueLogic.enqueue(order2)

        assertEquals(2, queueLogic.pendingCount)

        val flushSummary = queueLogic.flush("mock-token")

        assertEquals(2, flushSummary.uploadedCount)
        assertEquals(0, flushSummary.remainingCount)
        assertNull(flushSummary.lastError)
        assertEquals(0, queueLogic.pendingCount)
    }

    @Test
    fun testFlushStopsOnNetworkErrorAndPreservesRemaining() = runTest {
        var callCount = 0
        val mockEngine = MockEngine {
            callCount++
            if (callCount == 1) {
                respond(content = """{"status":"ok"}""", status = HttpStatusCode.Created)
            } else {
                throw IOException("Network dropped")
            }
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val queueLogic = SyncQueueLogic(client)

        val order1 = createDummyOrder("rec-1")
        val order2 = createDummyOrder("rec-2")
        val order3 = createDummyOrder("rec-3")

        queueLogic.enqueue(order1)
        queueLogic.enqueue(order2)
        queueLogic.enqueue(order3)

        assertEquals(3, queueLogic.pendingCount)

        val flushSummary = queueLogic.flush("mock-token")

        assertEquals(1, flushSummary.uploadedCount)
        assertEquals(2, flushSummary.remainingCount)
        assertEquals(SyncError.NETWORK_UNAVAILABLE, flushSummary.lastError)
        assertEquals(2, queueLogic.pendingCount)
        assertEquals("rec-2", queueLogic.pendingOrders.value.first().receiptId)
    }

    private fun createDummyOrder(receiptId: String) = OrderCreateDto(
        cashRegisterId = 1,
        receiptId = receiptId,
        fiscalCheckNumber = "0001",
        totalPrice = 100.0,
        paymentType = 0,
        createdAt = 1700000000L,
        items = emptyList()
    )
}
