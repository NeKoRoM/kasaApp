package com.kasa.feature.sync

import com.kasa.model.Product
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Ktor 3 HTTP-клієнт для зв'язку з FastAPI бекендом.
 */
class SyncClient(
    private val baseUrl: String,
    engine: HttpClientEngine = OkHttp.create()
) {

    val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    /**
     * Авторизація касира та отримання JWT Bearer токена.
     */
    suspend fun login(credentials: LoginRequest): SyncResult<LoginResponse> {
        return safeApiCall {
            val response: HttpResponse = httpClient.post("$baseUrl/jwt/login/") {
                contentType(ContentType.Application.Json)
                setBody(credentials)
            }

            when (response.status) {
                HttpStatusCode.OK -> SyncResult.Success(response.body<LoginResponse>())
                HttpStatusCode.Unauthorized -> SyncResult.Failure(
                    SyncError.UNAUTHORIZED_401, "Невірний логін або пароль"
                )
                else -> SyncResult.Failure(
                    SyncError.SERVER_ERROR_5XX, "Помилка сервера: ${response.status}"
                )
            }
        }
    }

    /**
     * Отримання номенклатурного довідника товарів для магазину/каси.
     */
    suspend fun fetchProducts(token: String, storeId: Long? = null): SyncResult<List<Product>> {
        return safeApiCall {
            val response: HttpResponse = httpClient.get("$baseUrl/products/") {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (storeId != null) {
                    parameter("store_id", storeId)
                }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val dtos = response.body<List<ProductDto>>()
                    val domainProducts = dtos.map { it.toDomainProduct() }
                    SyncResult.Success(domainProducts)
                }
                HttpStatusCode.Unauthorized -> SyncResult.Failure(
                    SyncError.UNAUTHORIZED_401, "Сесія застаріла"
                )
                else -> SyncResult.Failure(
                    SyncError.SERVER_ERROR_5XX, "Помилка отримання товарів: ${response.status}"
                )
            }
        }
    }

    /**
     * Відправка збереженого фіскалізованого чека на сервер.
     */
    suspend fun uploadOrder(token: String, orderDto: OrderCreateDto): SyncResult<String> {
        return safeApiCall {
            val response: HttpResponse = httpClient.post("$baseUrl/orders/") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(orderDto)
            }

            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> SyncResult.Success(response.bodyAsText())
                HttpStatusCode.Unauthorized -> SyncResult.Failure(
                    SyncError.UNAUTHORIZED_401, "Необхідна повторна авторизація"
                )
                else -> SyncResult.Failure(
                    SyncError.SERVER_ERROR_5XX, "Помилка відправки замовлення: ${response.status}"
                )
            }
        }
    }

    /**
     * Створення нового товару в базі сервера.
     */
    suspend fun createProduct(token: String, product: Product): SyncResult<Product> {
        val dto = ProductDto.fromDomainProduct(product)
        return safeApiCall {
            val response: HttpResponse = httpClient.post("$baseUrl/products/") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val created = response.body<ProductDto>().toDomainProduct()
                    SyncResult.Success(created)
                }
                HttpStatusCode.Unauthorized -> SyncResult.Failure(SyncError.UNAUTHORIZED_401, "Необхідна повторна авторизація")
                else -> SyncResult.Failure(SyncError.SERVER_ERROR_5XX, "Помилка створення товару: ${response.status}")
            }
        }
    }

    /**
     * Оновлення існуючого товару на сервері.
     */
    suspend fun updateProduct(token: String, product: Product): SyncResult<Product> {
        val dto = ProductDto.fromDomainProduct(product)
        return safeApiCall {
            val response: HttpResponse = httpClient.put("$baseUrl/products/${product.id}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val updated = response.body<ProductDto>().toDomainProduct()
                    SyncResult.Success(updated)
                }
                HttpStatusCode.Unauthorized -> SyncResult.Failure(SyncError.UNAUTHORIZED_401, "Необхідна повторна авторизація")
                else -> SyncResult.Failure(SyncError.SERVER_ERROR_5XX, "Помилка оновлення товару: ${response.status}")
            }
        }
    }

    /**
     * Видалення товару на сервері.
     */
    suspend fun deleteProduct(token: String, productId: Long): SyncResult<Unit> {
        return safeApiCall {
            val response: HttpResponse = httpClient.delete("$baseUrl/products/$productId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> SyncResult.Success(Unit)
                HttpStatusCode.Unauthorized -> SyncResult.Failure(SyncError.UNAUTHORIZED_401, "Необхідна повторна авторизація")
                else -> SyncResult.Failure(SyncError.SERVER_ERROR_5XX, "Помилка видалення товару: ${response.status}")
            }
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> SyncResult<T>): SyncResult<T> {
        return try {
            block()
        } catch (e: IOException) {
            SyncResult.Failure(SyncError.NETWORK_UNAVAILABLE, "Мережеве з'єднання недоступне: ${e.message}")
        } catch (e: Exception) {
            SyncResult.Failure(SyncError.DATA_CORRUPTION, "Помилка запиту: ${e.message}")
        }
    }
}
