package com.kasa.feature.pos

import androidx.compose.runtime.Composable
import com.kasa.model.Receipt

/**
 * Оверлей вторинних та управлінських діалогових вікон (Secondary & Management POS Dialogs).
 * Обслуговує сервісне меню РРО, журнал чеків, змінні звіти, програми лояльності та складський облік.
 */
@Composable
fun PosManagementDialogsOverlay(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenTaxUrl: (String) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null
) {
    // Знижка на весь чек
    if (state.isReceiptDiscountPromptOpen) {
        PosDiscountDialog(
            title = "Знижка на весь чек",
            targetName = "Сума без знижки: ${PosCurrencyFormatter.formatKopecks(state.rawTotalKopecks)} грн",
            currentDiscountPercent = state.receiptDiscountPercent,
            onApply = { onEvent(PosEvent.SetReceiptDiscount(it)) },
            onDismiss = { onEvent(PosEvent.PromptReceiptDiscount(false)) },
            rawTotalKopecks = state.rawTotalKopecks
        )
    }

    // Сервісне меню РРО
    if (state.isServiceDialogOpen) {
        PosServiceDialog(
            state = state,
            onEvent = onEvent,
            onOpenConnectionDialog = onOpenConnectionDialog
        )
    }

    // Журнал збережених фіскальних чеків
    if (state.isJournalDialogOpen) {
        PosReceiptJournalDialog(
            state = state,
            onEvent = onEvent,
            onOpenTaxUrl = onOpenTaxUrl,
            onShareReceipt = onShareReceipt,
            onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) }
        )
    }

    // Синхронізація артикулів (PLU) РРО
    if (state.isPluDialogOpen) {
        PosPluDialog(state = state, onEvent = onEvent)
    }

    // Відкладання чека з приміткою
    if (state.isHoldPromptOpen) {
        PosHoldNoteDialog(
            onConfirm = { onEvent(PosEvent.HoldCurrentReceipt(it)) },
            onDismiss = { onEvent(PosEvent.PromptHoldReceipt(false)) }
        )
    }

    // Список відкладених чеків
    if (state.isHeldReceiptsDialogOpen) {
        PosHeldReceiptsDialog(
            heldReceipts = state.heldReceipts,
            onRestore = { onEvent(PosEvent.RestoreHeldReceipt(it)) },
            onDelete = { onEvent(PosEvent.DeleteHeldReceipt(it)) },
            onDismiss = { onEvent(PosEvent.ToggleHeldReceiptsDialog) }
        )
    }

    // Роздільна оплата
    if (state.isSplitPaymentDialogOpen) {
        PosSplitPaymentDialog(
            subtotalKopecks = state.totalPayableKopecks,
            initialCashKopecks = state.splitCashKopecks,
            initialCardKopecks = state.splitCardKopecks,
            onApply = { cash, card -> onEvent(PosEvent.SetSplitPayment(cash, card)) },
            onDismiss = { onEvent(PosEvent.PromptSplitPayment(false)) }
        )
    }

    // Програма лояльності
    if (state.isLoyaltyDialogOpen) {
        PosLoyaltyDialog(
            customers = state.customersList,
            currentCustomer = state.selectedCustomer,
            onSelectCustomer = {
                onEvent(PosEvent.AttachCustomer(it))
                onEvent(PosEvent.PromptLoyaltyDialog(false))
            },
            onDetachCustomer = { onEvent(PosEvent.DetachCustomer) },
            onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) },
            onDismiss = { onEvent(PosEvent.PromptLoyaltyDialog(false)) }
        )
    }

    // Підсумок касової зміни
    if (state.isShiftReportDialogOpen) {
        PosShiftReportDialog(
            shift = state.currentShift,
            onPrintZReport = { onEvent(PosEvent.PrintZReport) },
            onPrintXReport = { onEvent(PosEvent.PrintXReport) },
            onDismiss = { onEvent(PosEvent.ToggleShiftReportDialog) },
            onOpenShift = { onEvent(PosEvent.OpenShift()) },
            onPerformCashInOut = { onEvent(PosEvent.PerformCashInOut(it)) }
        )
    }

    // Створення та редагування товару
    if (state.isProductEditDialogOpen) {
        PosProductEditDialog(
            product = state.editingProduct,
            prefilledBarcode = state.prefilledBarcode,
            suggestedPlu = state.nextAvailablePlu,
            availableCategories = state.availableCategories,
            existingProducts = state.catalogProducts,
            onSave = { onEvent(PosEvent.SaveProduct(it, addToReceiptOnCreate = state.editingProduct == null)) },
            onSaveAndNew = { onEvent(PosEvent.SaveProduct(it, addToReceiptOnCreate = false)) },
            onDelete = { onEvent(PosEvent.DeleteProduct(it)) },
            onDuplicate = { onEvent(PosEvent.DuplicateProduct(it)) },
            onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) },
            onDismiss = { onEvent(PosEvent.DismissProductEdit) }
        )
    }

    // Управління складом та номенклатурою
    if (state.isCatalogManagementDialogOpen) {
        PosCatalogManagementDialog(
            products = state.catalogProducts,
            onEvent = onEvent,
            onDismiss = { onEvent(PosEvent.ToggleCatalogManagementDialog(false)) }
        )
    }

    // Друк поличкового цінника
    if (state.isPriceTagDialogOpen && state.priceTagProduct != null) {
        PosPriceTagDialog(
            product = state.priceTagProduct,
            onPrintToKasa = { onEvent(PosEvent.PromptPriceTag(null)) },
            onDismiss = { onEvent(PosEvent.PromptPriceTag(null)) }
        )
    }

    // Масова переоцінка
    if (state.isBatchRepricingDialogOpen) {
        PosBatchRepricingDialog(
            availableCategories = state.availableCategories,
            onApply = { cat, pct, fix -> onEvent(PosEvent.BatchAdjustPrices(cat, pct, fix)) },
            onDismiss = { onEvent(PosEvent.ToggleBatchRepricingDialog(false)) }
        )
    }

    // Емулятор сканера штрих-кодів
    if (state.isScannerDialogOpen) {
        PosBarcodeScannerDialog(
            catalogProducts = state.catalogProducts,
            onScan = { onEvent(PosEvent.ScanBarcode(it)) },
            onDismiss = { onEvent(PosEvent.ToggleScannerDialog(false)) }
        )
    }

    // Швидке оприбуткування товару в 1 клік
    state.quickStockProduct?.let { prod ->
        PosQuickStockDialog(
            product = prod,
            onAdjustStock = { id, delta -> onEvent(PosEvent.PerformQuickStock(id, delta)) },
            onDismiss = { onEvent(PosEvent.PromptQuickStock(null)) }
        )
    }

    // Друк аркуша штрих-кодів для каси
    if (state.isBarcodeSheetDialogOpen) {
        PosBarcodeSheetDialog(
            products = state.catalogProducts,
            availableCategories = state.availableCategories,
            onPrintToKasa = { onEvent(PosEvent.ToggleBarcodeSheetDialog(false)) },
            onDismiss = { onEvent(PosEvent.ToggleBarcodeSheetDialog(false)) }
        )
    }
}

