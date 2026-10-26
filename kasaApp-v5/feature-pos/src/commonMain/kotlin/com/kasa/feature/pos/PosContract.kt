package com.kasa.feature.pos

import com.kasa.model.PaymentType
import com.kasa.model.Product
import com.kasa.model.ReceiptItem
import com.kasa.protocol.scale.ScaleWeight

/**
 * Стан окремого паралельного чека касира (мульти-чек / вкладки).
 */
data class PosCart(
    val id: Int,
    val name: String,
    val items: List<ReceiptItem> = emptyList(),
    val receiptType: com.kasa.model.ReceiptType = com.kasa.model.ReceiptType.SALE,
    val paymentType: PaymentType = PaymentType.CASH,
    val receiptDiscountPercent: Double = 0.0,
    val selectedCustomer: com.kasa.model.Customer? = null,
    val splitCashKopecks: Long? = null,
    val splitCardKopecks: Long? = null
)

/**
 * Стан робочого місця касира (POS-термінал).
 */
data class PosState(
    val items: List<ReceiptItem> = emptyList(),
    val rawTotalKopecks: Long = 0L,
    val totalPayableKopecks: Long = 0L,
    val roundingDifferenceKopecks: Long = 0L,
    val paymentType: PaymentType = PaymentType.CASH,
    val receiptType: com.kasa.model.ReceiptType = com.kasa.model.ReceiptType.SALE,
    val isKasaOnline: Boolean = false,
    val scaleWeight: ScaleWeight? = null,
    val searchQuery: String = "",
    val catalogProducts: List<Product> = emptyList(),
    val isFiscalProcessing: Boolean = false,
    val lastFiscalCheckNumber: String? = null,
    val activeExcisePromptIndex: Int? = null,
    val statusMessage: String? = null,
    val isServiceDialogOpen: Boolean = false,
    val kasaRtcTime: String? = null,
    val kasaHardwareInfo: String? = null,
    val isJournalDialogOpen: Boolean = false,
    val journalReceipts: List<com.kasa.model.Receipt> = emptyList(),
    val isPluDialogOpen: Boolean = false,
    val isPluSyncing: Boolean = false,
    val pluSyncProgress: com.kasa.protocol.plu.PluSyncProgress? = null,
    val pluSyncStatusMessage: String? = null,
    val receiptDiscountPercent: Double = 0.0,
    val activeDiscountPromptItemIndex: Int? = null,
    val activeQuantityPromptIndex: Int? = null,
    val isReceiptDiscountPromptOpen: Boolean = false,
    val heldReceipts: List<com.kasa.model.HeldReceipt> = emptyList(),
    val isHeldReceiptsDialogOpen: Boolean = false,
    val isHoldPromptOpen: Boolean = false,
    val cashDrawerBalanceKopecks: Long = 0L,
    val customerDisplayLine1: String = "",
    val customerDisplayLine2: String = "",
    val kasaStatus: com.kasa.protocol.status.KasaStatus? = null,
    val splitCashKopecks: Long? = null,
    val splitCardKopecks: Long? = null,
    val isSplitPaymentDialogOpen: Boolean = false,
    val selectedCustomer: com.kasa.model.Customer? = null,
    val isLoyaltyDialogOpen: Boolean = false,
    val customersList: List<com.kasa.model.Customer> = emptyList(),
    val currentShift: com.kasa.model.CashierShift? = null,
    val isShiftReportDialogOpen: Boolean = false,
    val lastClosedShift: com.kasa.model.CashierShift? = null,
    val isProductEditDialogOpen: Boolean = false,
    val editingProduct: Product? = null,
    val prefilledBarcode: String? = null,
    val selectedCatalogCategory: String = "ALL",
    val isCatalogManagementDialogOpen: Boolean = false,
    val isPriceTagDialogOpen: Boolean = false,
    val priceTagProduct: Product? = null,
    val isBatchRepricingDialogOpen: Boolean = false,
    val isScannerDialogOpen: Boolean = false,
    val activePricePromptIndex: Int? = null,
    val isNumpadDialogOpen: Boolean = false,
    val isCatalogTableView: Boolean = false,
    val quickStockProduct: Product? = null,
    val activeCartIndex: Int = 0,
    val carts: List<PosCart> = listOf(
        PosCart(id = 0, name = "Чек 1"),
        PosCart(id = 1, name = "Чек 2"),
        PosCart(id = 2, name = "Чек 3")
    ),
    val isBarcodeSheetDialogOpen: Boolean = false,
    val receivedCashKopecks: Long? = null
) {


    val isReceiptEmpty: Boolean
        get() = items.isEmpty()

    val hasUnstampedExcise: Boolean
        get() = items.any { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }

    val isReceiptPaperOut: Boolean
        get() = kasaStatus?.isReceiptPaperOut == true

    val isCoverOpen: Boolean
        get() = kasaStatus?.isCoverOpen == true

    val is24HoursShiftExpired: Boolean
        get() = (kasaStatus?.is24HoursShiftExpired == true) || (currentShift?.isExpired24h == true)

    val isStuckReceiptInRro: Boolean
        get() = kasaStatus?.isFiscalReceiptOpen == true && isReceiptEmpty

    val nextAvailablePlu: Long
        get() = (catalogProducts.maxOfOrNull { it.id } ?: 0L) + 1L

    val availableCategories: List<String>
        get() = catalogProducts.map { it.categoryName.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    val favoriteProducts: List<Product>
        get() = catalogProducts.filter { it.isFavorite }

    val changeKopecks: Long
        get() = if (receivedCashKopecks != null && receivedCashKopecks > totalPayableKopecks) receivedCashKopecks - totalPayableKopecks else 0L

    val isCashShort: Boolean
        get() = paymentType == PaymentType.CASH && receivedCashKopecks != null && receivedCashKopecks < totalPayableKopecks

    val canPay: Boolean
        get() = items.isNotEmpty() && !hasUnstampedExcise && !isFiscalProcessing && totalPayableKopecks > 0L && !isReceiptPaperOut && !isCoverOpen && !is24HoursShiftExpired && !isCashShort
}


/**
 * Події касового терміналу.
 */
sealed interface PosEvent {
    data class AddProduct(val product: Product, val customQuantity: Double? = null) : PosEvent
    data class UpdateQuantity(val itemIndex: Int, val quantity: Double) : PosEvent
    data class RemoveItem(val itemIndex: Int) : PosEvent
    data class SetPaymentType(val paymentType: PaymentType) : PosEvent
    data class SetReceivedCash(val amountKopecks: Long?) : PosEvent
    data class SetReceiptType(val receiptType: com.kasa.model.ReceiptType) : PosEvent
    data class LoadReceiptForReturn(val receipt: com.kasa.model.Receipt) : PosEvent
    data class EnterExciseStamp(val itemIndex: Int, val stamp: String) : PosEvent
    data class PromptExciseStamp(val itemIndex: Int?) : PosEvent
    data object DismissExcisePrompt : PosEvent
    data object ClearReceipt : PosEvent
    data class UpdateSearchQuery(val query: String) : PosEvent
    data class ScanBarcode(val barcode: String) : PosEvent
    data class ReceiveScaleMessage(val rawMessage: String) : PosEvent
    data object TareScale : PosEvent
    data object FiscalizeReceipt : PosEvent
    data object PrintXReport : PosEvent
    data object PrintZReport : PosEvent
    data object ToggleServiceDialog : PosEvent
    data object SyncKasaTime : PosEvent
    data object ReadKasaTime : PosEvent
    data object QueryKasaHardwareInfo : PosEvent
    data class PerformCashInOut(val amountKopecks: Long) : PosEvent
    data class UpdateCashDrawerBalance(val balanceKopecks: Long) : PosEvent
    data object OpenCashDrawer : PosEvent
    data object ToggleJournalDialog : PosEvent
    data object TogglePluDialog : PosEvent
    data object StartPluDumpFromKasa : PosEvent
    data object StartPluUploadToKasa : PosEvent
    data class SetItemDiscount(val itemIndex: Int, val discountPercent: Double) : PosEvent
    data class SetReceiptDiscount(val discountPercent: Double) : PosEvent
    data class PromptItemDiscount(val itemIndex: Int?) : PosEvent
    data class PromptQuantity(val itemIndex: Int?) : PosEvent
    data class PromptReceiptDiscount(val isOpen: Boolean) : PosEvent
    data class UpdateCatalog(val products: List<Product>) : PosEvent
    data class UpdateJournal(val receipts: List<com.kasa.model.Receipt>) : PosEvent
    data class UpdateHeldReceipts(val heldReceipts: List<com.kasa.model.HeldReceipt>) : PosEvent
    data class HoldCurrentReceipt(val note: String = "") : PosEvent
    data class RestoreHeldReceipt(val heldReceiptId: String) : PosEvent
    data class DeleteHeldReceipt(val heldReceiptId: String) : PosEvent
    data object ToggleHeldReceiptsDialog : PosEvent
    data class PromptHoldReceipt(val isOpen: Boolean) : PosEvent
    data class DisplayCustomerText(val line1: String, val line2: String = "") : PosEvent
    data object ClearCustomerDisplay : PosEvent
    data object VoidReceipt : PosEvent
    data object CheckKasaHealth : PosEvent
    data object DismissStatusMessage : PosEvent
    data class PromptSplitPayment(val isOpen: Boolean) : PosEvent
    data class SetSplitPayment(val cashKopecks: Long, val cardKopecks: Long) : PosEvent
    data class AttachCustomer(val customer: com.kasa.model.Customer) : PosEvent
    data object DetachCustomer : PosEvent
    data class PromptLoyaltyDialog(val isOpen: Boolean) : PosEvent
    data class UpdateCustomersList(val customers: List<com.kasa.model.Customer>) : PosEvent
    data class OpenShift(val cashierId: Long = 1L, val cashierName: String = "Головний касир") : PosEvent
    data class PromptShiftReportDialog(val isOpen: Boolean) : PosEvent
    data object ToggleShiftReportDialog : PosEvent
    data class PromptProductEdit(val product: Product? = null, val prefilledBarcode: String? = null) : PosEvent
    data class DuplicateProduct(val product: Product) : PosEvent
    data object DismissProductEdit : PosEvent
    data class SaveProduct(val product: Product, val addToReceiptOnCreate: Boolean = false) : PosEvent
    data class DeleteProduct(val productId: Long) : PosEvent
    data class SetCatalogCategoryFilter(val category: String) : PosEvent
    data class ToggleCatalogManagementDialog(val isOpen: Boolean) : PosEvent
    data class AdjustProductStock(val productId: Long, val deltaQuantity: Double) : PosEvent
    data class SetProductStockExact(val productId: Long, val exactQuantity: Double) : PosEvent
    data class ToggleProductFavorite(val productId: Long) : PosEvent
    data class ImportProductsFromCsv(val csvContent: String) : PosEvent
    data class PromptPriceTag(val product: Product?) : PosEvent
    data class ToggleBatchRepricingDialog(val isOpen: Boolean) : PosEvent
    data class BatchAdjustPrices(val category: String, val percentDelta: Double, val fixedDelta: Double) : PosEvent
    data class ToggleScannerDialog(val isOpen: Boolean) : PosEvent
    data class PromptItemPrice(val itemIndex: Int?) : PosEvent
    data class UpdateItemPrice(val itemIndex: Int, val priceKopecks: Long) : PosEvent
    data class ToggleNumpadDialog(val isOpen: Boolean) : PosEvent
    data class SetCatalogViewMode(val isTableView: Boolean) : PosEvent
    data class PromptQuickStock(val product: Product?) : PosEvent
    data class PerformQuickStock(val productId: Long, val deltaQuantity: Double) : PosEvent
    data class SwitchCart(val cartIndex: Int) : PosEvent
    data class ToggleBarcodeSheetDialog(val isOpen: Boolean) : PosEvent
}




/**
 * Одноразові побічні ефекти інтерфейсу касира.
 */
sealed interface PosEffect {
    data class ShowToast(val message: String) : PosEffect
    data class ShowErrorDialog(val title: String, val message: String) : PosEffect
    data class ReceiptPrintedSuccess(val checkNumber: String) : PosEffect
}
