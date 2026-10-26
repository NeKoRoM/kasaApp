package com.kasa.feature.pos

import com.kasa.model.HeldReceipt
import com.kasa.model.Product
import com.kasa.protocol.plu.FiscalPluEngine
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Асинхронний координатор операцій над кошиком, збереженням чеків та каталогом товарів (Operations Coordinator).
 */
class PosOperationsCoordinator(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<PosState>,
    private val serviceCoordinator: PosServiceCoordinator,
    private val transport: KasaTransport,
    private val pluEngine: FiscalPluEngine,
    private val onCatalogUpdated: (suspend (List<Product>) -> Unit)? = null,
    private val onHeldReceiptsUpdated: (suspend (List<HeldReceipt>) -> Unit)? = null
) {

    fun addProduct(product: Product, customQuantity: Double?) {
        val result = PosCartLogic.addProduct(state.value, product, customQuantity)
        state.value = result.newState
        serviceCoordinator.displayCustomerText(line1 = result.displayLine1, line2 = result.displayLine2)
    }

    fun holdCurrentReceipt(note: String) {
        val result = PosMultiCartLogic.holdCurrentReceipt(state.value, note) ?: return
        state.value = result.newState
        serviceCoordinator.displayCustomerText("Ласкаво просимо!", "Каса вільна")
        scope.launch { onHeldReceiptsUpdated?.invoke(result.updatedHeldList) }
    }

    fun restoreHeldReceipt(heldReceiptId: String) {
        val result = PosMultiCartLogic.restoreHeldReceipt(state.value, heldReceiptId) ?: return
        state.value = result.newState
        scope.launch { onHeldReceiptsUpdated?.invoke(result.updatedHeldList) }
    }

    fun deleteHeldReceipt(heldReceiptId: String) {
        val result = PosMultiCartLogic.deleteHeldReceipt(state.value, heldReceiptId)
        state.value = result.newState
        scope.launch { onHeldReceiptsUpdated?.invoke(result.updatedHeldList) }
    }

    fun saveProduct(product: Product, addToReceiptOnCreate: Boolean) {
        val result = PosCatalogLogic.saveProduct(state.value, product)
        state.value = result.newState
        scope.launch {
            onCatalogUpdated?.invoke(result.updatedCatalog)
            if (state.value.isKasaOnline && !transport.isScannerRegistrationMode && !state.value.isPluSyncing && !state.value.isFiscalProcessing) {
                pluEngine.programPlu(product)
            }
        }
        if (addToReceiptOnCreate) {
            addProduct(product, null)
        }
    }

    fun deleteProduct(productId: Long) {
        val result = PosCatalogLogic.deleteProduct(state.value, productId)
        state.value = result.newState
        scope.launch {
            onCatalogUpdated?.invoke(result.updatedCatalog)
            if (state.value.isKasaOnline && !transport.isScannerRegistrationMode && !state.value.isPluSyncing && !state.value.isFiscalProcessing) {
                pluEngine.deletePlu(productId)
            }
        }
    }

    fun adjustProductStock(productId: Long, deltaQuantity: Double) {
        val result = PosCatalogLogic.adjustProductStock(state.value, productId, deltaQuantity) ?: return
        state.value = result.newState
        scope.launch { onCatalogUpdated?.invoke(result.updatedCatalog) }
    }

    fun setProductStockExact(productId: Long, exactQuantity: Double) {
        val result = PosCatalogLogic.setProductStockExact(state.value, productId, exactQuantity) ?: return
        state.value = result.newState
        scope.launch { onCatalogUpdated?.invoke(result.updatedCatalog) }
    }

    fun toggleProductFavorite(productId: Long) {
        val result = PosCatalogLogic.toggleProductFavorite(state.value, productId) ?: return
        state.value = result.newState
        scope.launch { onCatalogUpdated?.invoke(result.updatedCatalog) }
    }

    fun importProductsFromCsv(csvContent: String) {
        val result = PosCatalogLogic.importProductsFromCsv(state.value, csvContent)
        state.value = result.newState
        result.updatedCatalog?.let { updated ->
            scope.launch { onCatalogUpdated?.invoke(updated) }
        }
    }

    fun batchAdjustPrices(category: String, percentDelta: Double, fixedDelta: Double) {
        val result = PosCatalogLogic.batchAdjustPrices(state.value, category, percentDelta, fixedDelta)
        state.value = result.newState
        scope.launch { onCatalogUpdated?.invoke(result.updatedCatalog) }
    }
}
