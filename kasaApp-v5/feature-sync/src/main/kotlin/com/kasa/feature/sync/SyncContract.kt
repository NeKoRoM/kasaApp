package com.kasa.feature.sync

import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.model.TaxGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer"
)

@Serializable
data class ProductDto(
    val id: Long? = null,
    @SerialName("product_id") val productId: Long? = 0,
    val name: String? = "",
    val price: Double? = 0.0,
    val section: String? = "1",
    val type: String? = "0",
    val remainder: Double? = 0.0,
    @SerialName("barcode_1") val barcode1: String? = "",
    @SerialName("barcode_2") val barcode2: String? = "",
    @SerialName("barcode_3") val barcode3: String? = "",
    @SerialName("barcode_4") val barcode4: String? = "",
    @SerialName("weight_or_unit") val weightOrUnit: String? = "0",
    @SerialName("unit_of_measurement") val unitOfMeasurement: String? = "шт",
    val uktzed: String? = ""
) {
    fun toDomainProduct(): Product {
        val allBarcodes = listOfNotNull(barcode1, barcode2, barcode3, barcode4).filter { it.isNotBlank() }
        val isWeighted = weightOrUnit == "1"
        return Product(
            id = productId ?: id ?: 0L,
            name = name.orEmpty(),
            price = price ?: 0.0,
            taxGroup = TaxGroup.GROUP_A,
            section = section?.toIntOrNull() ?: 1,
            barcodes = allBarcodes,
            isWeighted = isWeighted,
            uktzed = uktzed.orEmpty(),
            unitOfMeasurement = unitOfMeasurement ?: if (isWeighted) "кг" else "шт",
            remainder = remainder ?: 0.0
        )
    }

    companion object {
        fun fromDomainProduct(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                productId = product.id,
                name = product.name,
                price = product.price,
                section = product.section.toString(),
                remainder = product.remainder,
                barcode1 = product.barcodes.getOrNull(0).orEmpty(),
                barcode2 = product.barcodes.getOrNull(1).orEmpty(),
                barcode3 = product.barcodes.getOrNull(2).orEmpty(),
                barcode4 = product.barcodes.getOrNull(3).orEmpty(),
                weightOrUnit = if (product.isWeighted) "1" else "0",
                unitOfMeasurement = product.unitOfMeasurement,
                uktzed = product.uktzed
            )
        }
    }
}

@Serializable
data class OrderItemDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_name") val productName: String,
    @SerialName("product_price") val productPrice: Double,
    val quantity: Double,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("excise_stamp") val exciseStamp: String? = null,
    @SerialName("tax_group") val taxGroup: String = "А"
)

@Serializable
data class OrderCreateDto(
    @SerialName("cash_register_id") val cashRegisterId: Int = 1,
    @SerialName("receipt_id") val receiptId: String,
    @SerialName("fiscal_check_number") val fiscalCheckNumber: String? = null,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("payment_type") val paymentType: Int = 0,
    @SerialName("status") val status: String = "completed",
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<OrderItemDto> = emptyList()
) {
    companion object {
        fun fromReceipt(receipt: Receipt, cashRegisterId: Int = 1): OrderCreateDto {
            val dtoList = receipt.items.map { item ->
                OrderItemDto(
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.priceKopecks / 100.0,
                    quantity = item.quantity,
                    totalPrice = item.totalKopecks / 100.0,
                    exciseStamp = item.exciseStamp,
                    taxGroup = item.taxGroup.codeLetter
                )
            }
            return OrderCreateDto(
                cashRegisterId = cashRegisterId,
                receiptId = receipt.id,
                fiscalCheckNumber = receipt.fiscalCheckNumber,
                totalPrice = receipt.totalPayableKopecks / 100.0,
                paymentType = receipt.paymentType.code,
                status = if (receipt.receiptType == com.kasa.model.ReceiptType.RETURN) "refund" else "completed",
                createdAt = receipt.createdAt,
                customerName = receipt.customerName,
                items = dtoList
            )
        }
    }
}

enum class SyncError {
    NETWORK_UNAVAILABLE,
    UNAUTHORIZED_401,
    SERVER_ERROR_5XX,
    DATA_CORRUPTION,
    QUEUE_EMPTY
}

sealed interface SyncResult<out T> {
    data class Success<out T>(val data: T) : SyncResult<T>
    data class Failure(val error: SyncError, val message: String) : SyncResult<Nothing>
}
