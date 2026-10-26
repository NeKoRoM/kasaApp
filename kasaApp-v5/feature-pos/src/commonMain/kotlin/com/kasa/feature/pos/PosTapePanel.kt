package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import com.kasa.model.ReceiptType

/**
 * Ліва панель касового терміналу: чекова стрічка, підсумки, знижки та оплата.
 */
@Composable
fun PosTapePanel(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier,
    onSelectCatalogTab: (() -> Unit)? = null,
    onOpenConnectionDialog: (() -> Unit)? = null,
    showRroStatus: Boolean = true,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null
) {
    var isCurrentReceiptPreviewOpen by remember { mutableStateOf(false) }
    var isPaymentSheetOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideTape = maxWidth >= 520.dp
            val isNarrow = maxWidth < 360.dp
            val innerPadding = if (isNarrow || maxHeight < 500.dp) 6.dp else 8.dp

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Вкладки мульти-чека (Чек 1, Чек 2, Чек 3)
                PosCartTabsRow(
                activeCartIndex = state.activeCartIndex,
                carts = state.carts,
                onSelectCart = { onEvent(PosEvent.SwitchCart(it)) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Верхній рядок: тип чека та статус РРО
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = state.receiptType == ReceiptType.SALE,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(PosEvent.SetReceiptType(ReceiptType.SALE))
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = { Text("Продаж", fontSize = if (isNarrow) 11.sp else 12.sp, maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = state.receiptType == ReceiptType.RETURN,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(PosEvent.SetReceiptType(ReceiptType.RETURN))
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = { Text(if (isNarrow) "Поверн." else "Повернення", fontSize = if (isNarrow) 11.sp else 12.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error,
                            selectedLabelColor = MaterialTheme.colorScheme.onError
                        )
                    )
                }
                if (showRroStatus) {
                    val kasaContainer = if (state.isKasaOnline) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    val kasaColor = if (state.isKasaOnline) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    val kasaText = if (state.isKasaOnline) (if (isNarrow) "РРО ON" else "РРО онлайн ▾") else (if (isNarrow) "РРО OFF" else "РРО офлайн ▾")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = kasaContainer,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clickable {
                                if (onOpenConnectionDialog != null) {
                                    onOpenConnectionDialog()
                                } else {
                                    onEvent(PosEvent.ToggleServiceDialog)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text(
                                text = kasaText,
                                color = kasaColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(PosEvent.ToggleJournalDialog)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📋 Журнал",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список товарів у чеку або мобільна ергономічна заглушка порожнього чека
            if (state.items.isEmpty()) {
                PosReceiptEmptyView(
                    heldReceiptsCount = state.heldReceipts.size,
                    receiptType = state.receiptType,
                    onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) },
                    onOpenNumpad = { onEvent(PosEvent.ToggleNumpadDialog(true)) },
                    onOpenHeldReceipts = { onEvent(PosEvent.ToggleHeldReceiptsDialog) },
                    onOpenCatalog = onSelectCatalogTab,
                    onOpenJournal = { onEvent(PosEvent.ToggleJournalDialog) },
                    onCancelReturn = { onEvent(PosEvent.SetReceiptType(ReceiptType.SALE)) },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            } else if (isWideTape) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PosReceiptTapeList(
                        items = state.items,
                        catalogProducts = state.catalogProducts,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1.1f).fillMaxHeight()
                    )

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        PosCheckoutSection(
                            state = state,
                            onEvent = onEvent,
                            onOpenPreview = { isCurrentReceiptPreviewOpen = true }
                        )
                    }
                }
            } else {
                PosReceiptTapeList(
                    items = state.items,
                    catalogProducts = state.catalogProducts,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                PosMobileCartBottomBar(
                    state = state,
                    onEvent = onEvent,
                    onOpenPayment = { isPaymentSheetOpen = true },
                    onOpenPreview = { isCurrentReceiptPreviewOpen = true }
                )
            }
        }
    }
}

    if (isPaymentSheetOpen && state.items.isNotEmpty()) {
        PosMobilePaymentSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { isPaymentSheetOpen = false }
        )
    }

    if (isCurrentReceiptPreviewOpen && state.items.isNotEmpty()) {
        val previewReceipt = Receipt(
            id = "preview", createdAt = System.currentTimeMillis(), cashierId = 1L,
            items = state.items, paymentType = state.paymentType, receiptType = state.receiptType,
            discountPercent = state.receiptDiscountPercent, customer = state.selectedCustomer
        )
        PosReceiptPreviewDialog(
            receipt = previewReceipt,
            onDismiss = { isCurrentReceiptPreviewOpen = false },
            onOpenTaxUrl = onOpenTaxUrl,
            onShareReceipt = onShareReceipt
        )
    }
}

