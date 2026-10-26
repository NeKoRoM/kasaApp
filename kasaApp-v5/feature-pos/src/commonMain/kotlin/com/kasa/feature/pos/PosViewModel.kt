package com.kasa.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasa.model.CashierShift
import com.kasa.model.Customer
import com.kasa.model.HeldReceipt
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.plu.FiscalPluEngine
import com.kasa.protocol.scale.ScaleProtocolParser
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Чорна скринька бізнес-логіки робочого місця касира (FSM ViewModel).
 * Координує роботу доменних модулів: PosCartLogic, PosCatalogLogic, PosMultiCartLogic,
 * PosBarcodeLogic, PosFiscalLogic, PosFiscalCoordinator, PosServiceCoordinator та PosOperationsCoordinator.
 */
class PosViewModel(
    private val receiptEngine: FiscalReceiptEngine,
    private val serviceEngine: FiscalServiceEngine,
    private val transport: KasaTransport,
    initialCatalog: List<Product> = emptyList(),
    private val onReceiptFiscalized: (suspend (Receipt) -> Unit)? = null,
    private val pluEngine: FiscalPluEngine = FiscalPluEngine(transport),
    private val onCatalogUpdated: (suspend (List<Product>) -> Unit)? = null,
    initialHeldReceipts: List<HeldReceipt> = emptyList(),
    private val onHeldReceiptsUpdated: (suspend (List<HeldReceipt>) -> Unit)? = null,
    initialDrawerBalanceKopecks: Long = 0L,
    private val onDrawerBalanceUpdated: (suspend (Long) -> Unit)? = null,
    initialCustomers: List<Customer> = emptyList(),
    initialShift: CashierShift? = null,
    private val onShiftUpdated: (suspend (CashierShift?) -> Unit)? = null,
    private val onShiftClosed: (suspend (CashierShift) -> Unit)? = null
) : ViewModel() {

    private val _state = MutableStateFlow(
        PosState(
            catalogProducts = initialCatalog,
            heldReceipts = initialHeldReceipts,
            cashDrawerBalanceKopecks = initialDrawerBalanceKopecks,
            customersList = initialCustomers,
            currentShift = initialShift
        )
    )
    val state: StateFlow<PosState> = _state.asStateFlow()

    private val _effect = Channel<PosEffect>(Channel.BUFFERED)
    val effect: Flow<PosEffect> = _effect.receiveAsFlow()

    private val serviceCoordinator = PosServiceCoordinator(
        scope = viewModelScope, state = _state, effect = _effect, transport = transport,
        serviceEngine = serviceEngine, pluEngine = pluEngine, onCatalogUpdated = onCatalogUpdated
    )

    private val operationsCoordinator = PosOperationsCoordinator(
        scope = viewModelScope, state = _state, serviceCoordinator = serviceCoordinator,
        transport = transport, pluEngine = pluEngine, onCatalogUpdated = onCatalogUpdated,
        onHeldReceiptsUpdated = onHeldReceiptsUpdated
    )

    private val fiscalCoordinator = PosFiscalCoordinator(
        scope = viewModelScope, state = _state, effect = _effect, transport = transport,
        receiptEngine = receiptEngine, serviceEngine = serviceEngine, serviceCoordinator = serviceCoordinator,
        onReceiptFiscalized = onReceiptFiscalized, onDrawerBalanceUpdated = onDrawerBalanceUpdated,
        onCatalogUpdated = onCatalogUpdated, onShiftUpdated = onShiftUpdated, onShiftClosed = onShiftClosed
    )

    init {
        viewModelScope.launch {
            transport.state.collect { transportState ->
                val isOnline = transportState is TransportState.Connected
                _state.update { it.copy(isKasaOnline = isOnline) }
                if (isOnline && !transport.isScannerRegistrationMode) {
                    serviceCoordinator.checkKasaHealth()
                }
            }
        }
    }

    fun onEvent(event: PosEvent) {
        when (event) {
            is PosEvent.AddProduct -> operationsCoordinator.addProduct(event.product, event.customQuantity)
            is PosEvent.UpdateQuantity -> _state.update { PosCartLogic.updateQuantity(it, event.itemIndex, event.quantity) }
            is PosEvent.RemoveItem -> _state.update { PosCartLogic.removeItem(it, event.itemIndex) }
            is PosEvent.SetPaymentType -> _state.update { PosCartLogic.setPaymentType(it, event.paymentType) }
            is PosEvent.SetReceivedCash -> _state.update { it.copy(receivedCashKopecks = event.amountKopecks) }
            is PosEvent.SetReceiptType -> _state.update { it.copy(receiptType = event.receiptType) }
            is PosEvent.EnterExciseStamp -> _state.update { PosCartLogic.enterExciseStamp(it, event.itemIndex, event.stamp) }
            is PosEvent.PromptExciseStamp -> _state.update { it.copy(activeExcisePromptIndex = event.itemIndex) }
            is PosEvent.DismissExcisePrompt -> _state.update { it.copy(activeExcisePromptIndex = null) }
            is PosEvent.ClearReceipt -> {
                _state.update { PosCartLogic.clearReceipt(it) }
                serviceCoordinator.displayCustomerText("Ласкаво просимо!", "Каса вільна")
            }
            is PosEvent.UpdateSearchQuery -> _state.update { it.copy(searchQuery = event.query) }
            is PosEvent.ScanBarcode -> handleScanBarcode(event.barcode)
            is PosEvent.ReceiveScaleMessage -> ScaleProtocolParser.parse(event.rawMessage)?.let { p ->
                _state.update { it.copy(scaleWeight = p) }
            }
            is PosEvent.TareScale -> viewModelScope.launch { transport.sendFrame(ScaleProtocolParser.TARE_COMMAND_BYTES) }
            is PosEvent.FiscalizeReceipt -> fiscalCoordinator.fiscalizeReceipt()
            is PosEvent.PrintXReport -> serviceCoordinator.printXReport()
            is PosEvent.PrintZReport -> fiscalCoordinator.printZReport()
            is PosEvent.ToggleServiceDialog -> _state.update { it.copy(isServiceDialogOpen = !it.isServiceDialogOpen) }
            is PosEvent.SyncKasaTime -> serviceCoordinator.syncKasaTime()
            is PosEvent.ReadKasaTime -> serviceCoordinator.readKasaTime()
            is PosEvent.QueryKasaHardwareInfo -> serviceCoordinator.queryKasaHardwareInfo()
            is PosEvent.PerformCashInOut -> fiscalCoordinator.cashInOut(event.amountKopecks)
            is PosEvent.OpenCashDrawer -> serviceCoordinator.openCashDrawer()
            is PosEvent.ToggleJournalDialog -> _state.update { it.copy(isJournalDialogOpen = !it.isJournalDialogOpen) }
            is PosEvent.TogglePluDialog -> _state.update { it.copy(isPluDialogOpen = !it.isPluDialogOpen, pluSyncStatusMessage = null) }
            is PosEvent.StartPluDumpFromKasa -> serviceCoordinator.startPluDump()
            is PosEvent.StartPluUploadToKasa -> serviceCoordinator.startPluUpload()
            is PosEvent.UpdateCatalog -> _state.update { it.copy(catalogProducts = event.products) }
            is PosEvent.UpdateJournal -> _state.update { it.copy(journalReceipts = event.receipts) }
            is PosEvent.LoadReceiptForReturn -> _state.update { PosMultiCartLogic.loadReceiptForReturn(it, event.receipt) }
            is PosEvent.SetItemDiscount -> _state.update { PosCartLogic.setItemDiscount(it, event.itemIndex, event.discountPercent) }
            is PosEvent.SetReceiptDiscount -> _state.update { PosCartLogic.setReceiptDiscount(it, event.discountPercent) }
            is PosEvent.PromptItemDiscount -> _state.update { it.copy(activeDiscountPromptItemIndex = event.itemIndex) }
            is PosEvent.PromptQuantity -> _state.update { it.copy(activeQuantityPromptIndex = event.itemIndex) }
            is PosEvent.PromptReceiptDiscount -> _state.update { it.copy(isReceiptDiscountPromptOpen = event.isOpen) }
            is PosEvent.UpdateHeldReceipts -> _state.update { it.copy(heldReceipts = event.heldReceipts) }
            is PosEvent.PromptHoldReceipt -> _state.update { it.copy(isHoldPromptOpen = event.isOpen) }
            is PosEvent.ToggleHeldReceiptsDialog -> _state.update { it.copy(isHeldReceiptsDialogOpen = !it.isHeldReceiptsDialogOpen) }
            is PosEvent.HoldCurrentReceipt -> operationsCoordinator.holdCurrentReceipt(event.note)
            is PosEvent.RestoreHeldReceipt -> operationsCoordinator.restoreHeldReceipt(event.heldReceiptId)
            is PosEvent.DeleteHeldReceipt -> operationsCoordinator.deleteHeldReceipt(event.heldReceiptId)
            is PosEvent.UpdateCashDrawerBalance -> _state.update { it.copy(cashDrawerBalanceKopecks = event.balanceKopecks) }
            is PosEvent.DisplayCustomerText -> serviceCoordinator.displayCustomerText(event.line1, event.line2)
            is PosEvent.ClearCustomerDisplay -> serviceCoordinator.clearCustomerDisplay()
            is PosEvent.VoidReceipt -> serviceCoordinator.voidReceipt()
            is PosEvent.CheckKasaHealth -> serviceCoordinator.checkKasaHealth()
            is PosEvent.DismissStatusMessage -> _state.update { it.copy(statusMessage = null) }
            is PosEvent.PromptSplitPayment -> _state.update { it.copy(isSplitPaymentDialogOpen = event.isOpen) }
            is PosEvent.SetSplitPayment -> _state.update { PosCartLogic.setSplitPayment(it, event.cashKopecks, event.cardKopecks) }
            is PosEvent.AttachCustomer -> {
                _state.update { PosCartLogic.attachCustomer(it, event.customer) }
                serviceCoordinator.displayCustomerText(event.customer.name.take(20), "Знижка ${event.customer.discountPercent}%")
            }
            is PosEvent.DetachCustomer -> {
                _state.update { PosCartLogic.detachCustomer(it) }
                serviceCoordinator.displayCustomerText("Клієнт відкріплений", "")
            }
            is PosEvent.PromptLoyaltyDialog -> _state.update { it.copy(isLoyaltyDialogOpen = event.isOpen) }
            is PosEvent.UpdateCustomersList -> _state.update { it.copy(customersList = event.customers) }
            is PosEvent.OpenShift -> fiscalCoordinator.openShift(event.cashierId, event.cashierName)
            is PosEvent.PromptShiftReportDialog -> _state.update { it.copy(isShiftReportDialogOpen = event.isOpen) }
            is PosEvent.ToggleShiftReportDialog -> _state.update { it.copy(isShiftReportDialogOpen = !it.isShiftReportDialogOpen) }
            is PosEvent.PromptProductEdit -> _state.update {
                it.copy(isProductEditDialogOpen = true, editingProduct = event.product, prefilledBarcode = event.prefilledBarcode)
            }
            is PosEvent.DismissProductEdit -> _state.update {
                it.copy(isProductEditDialogOpen = false, editingProduct = null, prefilledBarcode = null)
            }
            is PosEvent.SaveProduct -> operationsCoordinator.saveProduct(event.product, event.addToReceiptOnCreate)
            is PosEvent.DeleteProduct -> operationsCoordinator.deleteProduct(event.productId)
            is PosEvent.DuplicateProduct -> _state.update { PosCatalogLogic.duplicateProduct(it, event.product) }
            is PosEvent.SetCatalogCategoryFilter -> _state.update { it.copy(selectedCatalogCategory = event.category) }
            is PosEvent.ToggleCatalogManagementDialog -> _state.update { it.copy(isCatalogManagementDialogOpen = event.isOpen) }
            is PosEvent.AdjustProductStock -> operationsCoordinator.adjustProductStock(event.productId, event.deltaQuantity)
            is PosEvent.SetProductStockExact -> operationsCoordinator.setProductStockExact(event.productId, event.exactQuantity)
            is PosEvent.ToggleProductFavorite -> operationsCoordinator.toggleProductFavorite(event.productId)
            is PosEvent.ImportProductsFromCsv -> operationsCoordinator.importProductsFromCsv(event.csvContent)
            is PosEvent.PromptPriceTag -> _state.update { it.copy(isPriceTagDialogOpen = event.product != null, priceTagProduct = event.product) }
            is PosEvent.ToggleBatchRepricingDialog -> _state.update { it.copy(isBatchRepricingDialogOpen = event.isOpen) }
            is PosEvent.BatchAdjustPrices -> operationsCoordinator.batchAdjustPrices(event.category, event.percentDelta, event.fixedDelta)
            is PosEvent.ToggleScannerDialog -> _state.update { it.copy(isScannerDialogOpen = event.isOpen) }
            is PosEvent.PromptItemPrice -> _state.update { it.copy(activePricePromptIndex = event.itemIndex) }
            is PosEvent.UpdateItemPrice -> _state.update { PosCartLogic.updateItemPrice(it, event.itemIndex, event.priceKopecks) }
            is PosEvent.ToggleNumpadDialog -> _state.update { it.copy(isNumpadDialogOpen = event.isOpen) }
            is PosEvent.SetCatalogViewMode -> _state.update { it.copy(isCatalogTableView = event.isTableView) }
            is PosEvent.PromptQuickStock -> _state.update { it.copy(quickStockProduct = event.product) }
            is PosEvent.PerformQuickStock -> {
                operationsCoordinator.adjustProductStock(event.productId, event.deltaQuantity)
                _state.update { it.copy(quickStockProduct = null) }
            }
            is PosEvent.SwitchCart -> _state.update { PosMultiCartLogic.switchCart(it, event.cartIndex) }
            is PosEvent.ToggleBarcodeSheetDialog -> _state.update { it.copy(isBarcodeSheetDialogOpen = event.isOpen) }
        }
    }

    private fun handleScanBarcode(barcode: String) {
        val action = PosBarcodeLogic.parseBarcodeAction(_state.value, barcode) ?: return
        when (action) {
            is PosBarcodeLogic.BarcodeScanAction.SetPrefilledBarcode -> _state.update {
                it.copy(prefilledBarcode = action.barcode, statusMessage = action.message)
            }
            is PosBarcodeLogic.BarcodeScanAction.OpenProductEdit -> _state.update {
                it.copy(isProductEditDialogOpen = true, editingProduct = action.product, prefilledBarcode = action.prefilledBarcode, statusMessage = action.message)
            }
            is PosBarcodeLogic.BarcodeScanAction.EnterExciseStamp -> _state.update {
                PosCartLogic.enterExciseStamp(it, action.itemIndex, action.stamp)
            }
            is PosBarcodeLogic.BarcodeScanAction.AttachCustomer -> {
                _state.update { PosCartLogic.attachCustomer(it, action.customer) }
                serviceCoordinator.displayCustomerText(action.customer.name.take(20), "Знижка ${action.customer.discountPercent}%")
            }
            is PosBarcodeLogic.BarcodeScanAction.LoadReturnReceipt -> _state.update {
                PosMultiCartLogic.loadReceiptForReturn(it, action.receipt)
            }
            is PosBarcodeLogic.BarcodeScanAction.IncrementExistingItem -> _state.update {
                PosCartLogic.updateQuantity(it, action.itemIndex, action.newQuantity)
            }
            is PosBarcodeLogic.BarcodeScanAction.AddProduct -> {
                operationsCoordinator.addProduct(action.product, action.quantity)
            }
            is PosBarcodeLogic.BarcodeScanAction.ProductNotFound -> _state.update {
                it.copy(
                    statusMessage = "Товар '${action.cleanBarcode}' не знайдено. Введіть дані для реєстрації.",
                    isProductEditDialogOpen = true,
                    editingProduct = if (action.hasLetters) Product(id = action.nextPlu, name = action.cleanBarcode, price = 0.0) else null,
                    prefilledBarcode = if (!action.hasLetters) action.cleanBarcode else null
                )
            }
        }
    }
}
