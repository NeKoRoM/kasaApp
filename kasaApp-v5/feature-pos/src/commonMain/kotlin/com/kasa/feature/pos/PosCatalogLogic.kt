package com.kasa.feature.pos

import com.kasa.model.Product
import com.kasa.model.ProductCsvParser

/**
 * Чиста бізнес-логіка керування номенклатурою товарів та залишками (Catalog Domain Logic).
 */
object PosCatalogLogic {

    data class SaveProductResult(
        val newState: PosState,
        val updatedCatalog: List<Product>,
        val isNewProduct: Boolean
    )

    fun saveProduct(current: PosState, product: Product): SaveProductResult {
        val currentCatalog = current.catalogProducts
        val exists = currentCatalog.any { it.id == product.id }
        val updatedCatalog = if (exists) {
            currentCatalog.map { if (it.id == product.id) product else it }
        } else {
            currentCatalog + product
        }
        val barcodeConflict = currentCatalog.find { existing ->
            existing.id != product.id && existing.barcodes.any { b -> product.barcodes.contains(b) }
        }
        val saveStatus = if (barcodeConflict != null) {
            "Товар '${product.name}' збережено (⚠️ спільний код з #${barcodeConflict.id})"
        } else {
            "Товар '${product.name}' збережено"
        }
        val newState = current.copy(
            catalogProducts = updatedCatalog,
            isProductEditDialogOpen = false,
            editingProduct = null,
            prefilledBarcode = null,
            statusMessage = saveStatus
        )
        return SaveProductResult(
            newState = newState,
            updatedCatalog = updatedCatalog,
            isNewProduct = !exists
        )
    }

    data class DeleteProductResult(
        val newState: PosState,
        val updatedCatalog: List<Product>
    )

    fun deleteProduct(current: PosState, productId: Long): DeleteProductResult {
        val updatedCatalog = current.catalogProducts.filterNot { it.id == productId }
        val newState = current.copy(
            catalogProducts = updatedCatalog,
            isProductEditDialogOpen = false,
            editingProduct = null,
            prefilledBarcode = null,
            statusMessage = "Товар видалено"
        )
        return DeleteProductResult(
            newState = newState,
            updatedCatalog = updatedCatalog
        )
    }

    fun duplicateProduct(current: PosState, product: Product): PosState {
        val nextPlu = (current.catalogProducts.maxOfOrNull { it.id } ?: 0L) + 1L
        val duplicate = product.copy(
            id = nextPlu,
            name = "${product.name} (копія)".take(50),
            barcodes = emptyList()
        )
        return current.copy(
            isProductEditDialogOpen = true,
            editingProduct = duplicate,
            prefilledBarcode = null,
            statusMessage = "Створення копії товару #${product.id}"
        )
    }

    data class StockAdjustResult(
        val newState: PosState,
        val updatedCatalog: List<Product>
    )

    fun adjustProductStock(current: PosState, productId: Long, deltaQuantity: Double): StockAdjustResult? {
        val currentCatalog = current.catalogProducts
        val target = currentCatalog.find { it.id == productId } ?: return null
        val newRemainder = (target.remainder + deltaQuantity).coerceAtLeast(0.0)
        val updatedTarget = target.copy(remainder = newRemainder)
        val updatedCatalog = currentCatalog.map { if (it.id == productId) updatedTarget else it }

        val newState = current.copy(
            catalogProducts = updatedCatalog,
            statusMessage = "Залишок '${target.name}': $newRemainder ${target.unitOfMeasurement}"
        )
        return StockAdjustResult(newState, updatedCatalog)
    }

    fun setProductStockExact(current: PosState, productId: Long, exactQuantity: Double): StockAdjustResult? {
        val currentCatalog = current.catalogProducts
        val target = currentCatalog.find { it.id == productId } ?: return null
        val newRemainder = exactQuantity.coerceAtLeast(0.0)
        val updatedTarget = target.copy(remainder = newRemainder)
        val updatedCatalog = currentCatalog.map { if (it.id == productId) updatedTarget else it }

        val newState = current.copy(
            catalogProducts = updatedCatalog,
            statusMessage = "Інвентаризація '${target.name}': $newRemainder ${target.unitOfMeasurement}"
        )
        return StockAdjustResult(newState, updatedCatalog)
    }

    fun toggleProductFavorite(current: PosState, productId: Long): StockAdjustResult? {
        val currentCatalog = current.catalogProducts
        val target = currentCatalog.find { it.id == productId } ?: return null
        val updatedTarget = target.copy(isFavorite = !target.isFavorite)
        val updatedCatalog = currentCatalog.map { if (it.id == productId) updatedTarget else it }

        val newState = current.copy(
            catalogProducts = updatedCatalog,
            statusMessage = if (updatedTarget.isFavorite) "Додано в обране: ${target.name}" else "Вилучено з обраного: ${target.name}"
        )
        return StockAdjustResult(newState, updatedCatalog)
    }

    data class ImportCsvResult(
        val newState: PosState,
        val updatedCatalog: List<Product>?
    )

    fun importProductsFromCsv(current: PosState, csvContent: String): ImportCsvResult {
        val imported = ProductCsvParser.importFromCsv(csvContent)
        if (imported.isEmpty()) {
            return ImportCsvResult(
                newState = current.copy(statusMessage = "Помилка імпорту: не знайдено валідних рядків у CSV"),
                updatedCatalog = null
            )
        }
        val currentCatalog = current.catalogProducts.toMutableList()
        var addedCount = 0
        var updatedCount = 0

        for (newProd in imported) {
            val idx = currentCatalog.indexOfFirst { it.id == newProd.id }
            if (idx >= 0) {
                currentCatalog[idx] = newProd
                updatedCount++
            } else {
                currentCatalog.add(newProd)
                addedCount++
            }
        }

        val newState = current.copy(
            catalogProducts = currentCatalog,
            statusMessage = "Імпортовано з CSV: +$addedCount нових, $updatedCount оновлено"
        )
        return ImportCsvResult(newState, currentCatalog)
    }

    data class BatchAdjustPricesResult(
        val newState: PosState,
        val updatedCatalog: List<Product>
    )

    fun batchAdjustPrices(current: PosState, category: String, percentDelta: Double, fixedDelta: Double): BatchAdjustPricesResult {
        val currentCatalog = current.catalogProducts
        var count = 0
        val updatedCatalog = currentCatalog.map { prod ->
            val matchesCategory = category == "ALL" || prod.categoryName.equals(category, ignoreCase = true)
            if (matchesCategory) {
                count++
                val newPrice = if (percentDelta != 0.0) {
                    (prod.price * (1.0 + percentDelta / 100.0)).coerceAtLeast(0.01)
                } else {
                    (prod.price + fixedDelta).coerceAtLeast(0.01)
                }
                val roundedPrice = Math.round(newPrice * 100.0) / 100.0
                prod.copy(price = roundedPrice)
            } else {
                prod
            }
        }

        val newState = current.copy(
            catalogProducts = updatedCatalog,
            statusMessage = "Переоцінено $count товарів (категорія '$category')"
        )
        return BatchAdjustPricesResult(newState, updatedCatalog)
    }
}
