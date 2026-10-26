package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Головний контейнер робочого місця касира (Landscape-орієнтація).
 * Координує взаємодію стрічки чека, каталогу товарів, швидких касових діалогів та управлінських оверлеїв.
 */
@Composable
fun PosScreen(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null,
    onOpenConnectionDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompactDevice = maxWidth < 720.dp || maxHeight < 500.dp
        if (isCompactDevice) {
            PosCompactLayout(
                state = state,
                onEvent = onEvent,
                onOpenConnectionDialog = onOpenConnectionDialog,
                onOpenTaxUrl = onOpenTaxUrl,
                onShareReceipt = onShareReceipt
            )
        } else {
            PosExpandedLayout(
                state = state,
                onEvent = onEvent,
                onOpenConnectionDialog = onOpenConnectionDialog,
                onOpenTaxUrl = onOpenTaxUrl,
                onShareReceipt = onShareReceipt,
                showShortcutsBar = maxHeight >= 560.dp
            )

            // Спливаючий банер статусів для десктопного макета
            PosStatusBanner(
                statusMessage = state.statusMessage,
                onDismiss = { onEvent(PosEvent.DismissStatusMessage) },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }

    // 1. Діалог введення акцизної марки
    if (state.activeExcisePromptIndex != null) {
        val item = state.items.getOrNull(state.activeExcisePromptIndex)
        if (item != null) {
            PosExciseDialog(
                productName = item.productName,
                onConfirm = { stamp ->
                    onEvent(PosEvent.EnterExciseStamp(state.activeExcisePromptIndex, stamp))
                },
                onDismiss = { onEvent(PosEvent.DismissExcisePrompt) },
                onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) }
            )
        }
    }

    // 2. Діалог встановлення знижки на конкретний товар
    if (state.activeDiscountPromptItemIndex != null) {
        val itemIndex = state.activeDiscountPromptItemIndex
        val item = state.items.getOrNull(itemIndex)
        if (item != null) {
            PosDiscountDialog(
                title = "Знижка на позицію",
                targetName = "${item.productName} (${PosCurrencyFormatter.formatKopecks(item.rawTotalKopecks)} грн)",
                currentDiscountPercent = item.discountPercent,
                onApply = { percent ->
                    onEvent(PosEvent.SetItemDiscount(itemIndex, percent))
                },
                onDismiss = {
                    onEvent(PosEvent.PromptItemDiscount(null))
                },
                rawTotalKopecks = item.rawTotalKopecks
            )
        }
    }

    // 3. Діалог швидкого введення кількості та ваги товару
    if (state.activeQuantityPromptIndex != null) {
        val itemIndex = state.activeQuantityPromptIndex
        val item = state.items.getOrNull(itemIndex)
        if (item != null) {
            PosQuantityDialog(
                item = item,
                onConfirm = { newQty ->
                    onEvent(PosEvent.UpdateQuantity(itemIndex, newQty))
                    onEvent(PosEvent.PromptQuantity(null))
                },
                onDismiss = {
                    onEvent(PosEvent.PromptQuantity(null))
                }
            )
        }
    }

    // 4. Діалог вільної ціни або зміни ціни товару
    if (state.activePricePromptIndex != null) {
        val itemIndex = state.activePricePromptIndex
        val item = state.items.getOrNull(itemIndex)
        if (item != null) {
            PosPricePromptDialog(
                productName = item.productName,
                currentPriceKopecks = item.priceKopecks,
                onConfirm = { newPriceKopecks ->
                    onEvent(PosEvent.UpdateItemPrice(itemIndex, newPriceKopecks))
                    onEvent(PosEvent.PromptItemPrice(null))
                },
                onDismiss = {
                    onEvent(PosEvent.PromptItemPrice(null))
                }
            )
        }
    }

    // 5. Сенсорна цифрова клавіатура касира (Нумпад)
    if (state.isNumpadDialogOpen) {
        PosTouchNumpadDialog(
            onScanBarcode = { onEvent(PosEvent.ScanBarcode(it)) },
            onUpdateQuantity = { qty ->
                val lastIdx = state.items.lastIndex
                if (lastIdx != -1) onEvent(PosEvent.UpdateQuantity(lastIdx, qty))
            },
            onSearchPlu = { query ->
                onEvent(PosEvent.UpdateSearchQuery(query))
                onEvent(PosEvent.ScanBarcode(query))
            },
            onDismiss = { onEvent(PosEvent.ToggleNumpadDialog(false)) }
        )
    }

    // 6. Оверлей вторинних та управлінських діалогів
    PosManagementDialogsOverlay(
        state = state,
        onEvent = onEvent,
        onOpenTaxUrl = onOpenTaxUrl ?: {},
        onOpenConnectionDialog = onOpenConnectionDialog,
        onShareReceipt = onShareReceipt
    )
}
