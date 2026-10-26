package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Модальне вікно перегляду локального журналу фіскалізованих чеків касира.
 * Підтримує пошук за номером чека/товаром та фільтрацію за формою оплати.
 */
@Composable
fun PosReceiptJournalDialog(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf<PaymentType?>(null) }
    var previewReceipt by remember { mutableStateOf<Receipt?>(null) }

    val filteredReceipts = remember(state.journalReceipts, searchQuery, selectedPaymentFilter) {
        state.journalReceipts.filter { receipt ->
            val matchesPayment = selectedPaymentFilter == null || receipt.paymentType == selectedPaymentFilter
            val matchesSearch = searchQuery.isBlank() ||
                (receipt.fiscalCheckNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                receipt.id.contains(searchQuery, ignoreCase = true) ||
                receipt.items.any { it.productName.contains(searchQuery, ignoreCase = true) }
            matchesPayment && matchesSearch
        }
    }

    val totalCashKopecks = filteredReceipts.sumOf { r ->
        when (r.paymentType) {
            PaymentType.CASH -> r.totalPayableKopecks
            PaymentType.CARD -> 0L
            PaymentType.COMBINED -> r.splitCashKopecks ?: 0L
        }
    }
    val totalCardKopecks = filteredReceipts.sumOf { r ->
        when (r.paymentType) {
            PaymentType.CASH -> 0L
            PaymentType.CARD -> r.totalPayableKopecks
            PaymentType.COMBINED -> r.splitCardKopecks ?: 0L
        }
    }

    Dialog(
        onDismissRequest = { onEvent(PosEvent.ToggleJournalDialog) },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isMobile = maxWidth < 600.dp
            val isNarrow = maxWidth < 440.dp
            val summaryPadding = if (maxHeight < 500.dp) 6.dp else 8.dp
            val searchPlaceholder = if (isNarrow) "Пошук чека або товару..." else "Пошук за № чека або назвою товару..."

            Surface(
                modifier = if (isMobile) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.92f).widthIn(max = 640.dp)
                },
                shape = if (isMobile) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = if (isMobile) 10.dp else 16.dp, vertical = if (isMobile) 8.dp else 14.dp)
                ) {
                    // Верхня панель журналу чеків
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { onEvent(PosEvent.ToggleJournalDialog) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(
                                    text = "🧾 Журнал чеків (${filteredReceipts.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Всього збережено: ${state.journalReceipts.size} фіскальних чеків",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Поле швидкого пошуку чека
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = searchPlaceholder,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(38.dp)) {
                                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (onOpenScanner != null) {
                                    IconButton(onClick = onOpenScanner, modifier = Modifier.size(38.dp)) {
                                        Text("📷", fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PosJournalPaymentFilterChips(
                        selectedPaymentFilter = selectedPaymentFilter,
                        totalReceiptsCount = state.journalReceipts.size,
                        onSelectFilter = { selectedPaymentFilter = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PosJournalSummaryRow(
                        receiptsCount = filteredReceipts.size,
                        totalCashKopecks = totalCashKopecks,
                        totalCardKopecks = totalCardKopecks,
                        isNarrow = isNarrow,
                        summaryPadding = summaryPadding
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredReceipts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🧾", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "За запитом '$searchQuery' чеків не знайдено" else "Журнал фіскальних чеків порожній",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredReceipts) { receipt ->
                                PosJournalReceiptRow(
                                    receipt = receipt,
                                    onClick = { previewReceipt = receipt },
                                    onReturnReceipt = { onEvent(PosEvent.LoadReceiptForReturn(receipt)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewReceipt != null) {
        PosReceiptPreviewDialog(
            receipt = previewReceipt!!,
            onDismiss = { previewReceipt = null },
            onOpenTaxUrl = onOpenTaxUrl,
            onShareReceipt = onShareReceipt
        )
    }
}

