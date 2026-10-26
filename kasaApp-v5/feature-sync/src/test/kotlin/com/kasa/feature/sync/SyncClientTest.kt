package com.kasa.feature.sync

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyncClientTest {

    @Test
    fun testLoginSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/jwt/login/", request.url.encodedPath)
            respond(
                content = """{"access_token":"mock-jwt-token-123","token_type":"Bearer"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val result = client.login(LoginRequest("kasa1", "secret"))

        assertIs<SyncResult.Success<LoginResponse>>(result)
        assertEquals("mock-jwt-token-123", result.data.accessToken)
    }

    @Test
    fun testLoginUnauthorized() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"detail":"Invalid credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val result = client.login(LoginRequest("kasa1", "wrong_pass"))

        assertIs<SyncResult.Failure>(result)
        assertEquals(SyncError.UNAUTHORIZED_401, result.error)
    }

    @Test
    fun testFetchProductsMapping() = runTest {
        val jsonPayload = """
            [
                {
                    "product_id": 101,
                    "name": "Молоко 2.5%",
                    "price": 38.50,
                    "section": "1",
                    "barcode_1": "482000000001",
                    "weight_or_unit": "0",
                    "unit_of_measurement": "шт",
                    "remainder": 15.0
                },
                {
                    "product_id": 102,
                    "name": "Яблука Голден",
                    "price": 25.00,
                    "section": "2",
                    "barcode_1": "482000000002",
                    "weight_or_unit": "1",
                    "unit_of_measurement": "кг",
                    "remainder": 42.3
                }
            ]
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/products/", request.url.encodedPath)
            assertTrue(request.headers[HttpHeaders.Authorization]?.contains("mock-token") == true)
            respond(
                content = jsonPayload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val result = client.fetchProducts("mock-token")

        assertTrue(result is SyncResult.Success)
        val products = result.data
        assertEquals(2, products.size)
        assertEquals("Молоко 2.5%", products[0].name)
        assertEquals(false, products[0].isWeighted)
        assertEquals(38.50, products[0].price)

        assertEquals("Яблука Голден", products[1].name)
        assertEquals(true, products[1].isWeighted)
        assertEquals(25.00, products[1].price)
    }

    @Test
    fun testUploadOrderSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/orders/", request.url.encodedPath)
            respond(
                content = """{"status":"ok","order_id":"ord-999"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = SyncClient("http://localhost:8000", mockEngine)
        val orderDto = OrderCreateDto(
            cashRegisterId = 1,
            receiptId = "rec-1",
            fiscalCheckNumber = "0001",
            totalPrice = 50.0,
            paymentType = 0,
            createdAt = 1700000000L,
            items = listOf(
                OrderItemDto(
                    productId = 101,
                    productName = "Хліб",
                    productPrice = 25.0,
                    quantity = 2.0,
                    totalPrice = 50.0
                )
            )
        )

        val result = client.uploadOrder("mock-token", orderDto)
        assertIs<SyncResult.Success<String>>(result)
    }
}
