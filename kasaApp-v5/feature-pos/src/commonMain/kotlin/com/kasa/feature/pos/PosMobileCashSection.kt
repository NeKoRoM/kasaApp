package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface CashQuickOption {
    data object Exact : CashQuickOption
    data class Bill(val uah: Long) : CashQuickOption
    data object Custom : CashQuickOption
}

/**
 * Секція вибору купюр та розрахунку решти для мобільного вікна оплати.
 * Адаптована для компактних смартфонів, включаючи екран 320-360dp.
 */
@Composable
fun PosMobileCashSection(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isCustomAmountOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Швидкі купюри:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val bills = remember(state.totalPayableKopecks) { calculateSuggestedBills(state.totalPayableKopecks) }
            val options = remember(bills, state.totalPayableKopecks) {
                buildList {
                    add(CashQuickOption.Exact)
                    bills.take(4).forEach { add(CashQuickOption.Bill(it)) }
                    add(CashQuickOption.Custom)
                }
            }
            val rows = remember(options) { options.chunked(3) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = when (option) {
                                is CashQuickOption.Exact -> state.receivedCashKopecks == state.totalPayableKopecks
                                is CashQuickOption.Bill -> state.receivedCashKopecks == option.uah * 100L
                                is CashQuickOption.Custom -> isCustomAmountOpen
                            }
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            }

                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (option) {
                                        is CashQuickOption.Exact -> onEvent(PosEvent.SetReceivedCash(state.totalPayableKopecks))
                                        is CashQuickOption.Bill -> onEvent(PosEvent.SetReceivedCash(option.uah * 100L))
                                        is CashQuickOption.Custom -> isCustomAmountOpen = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = bgColor,
                                border = border,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = when (option) {
                                            is CashQuickOption.Exact -> "Рівно"
                                            is CashQuickOption.Bill -> "${option.uah} ₴"
                                            is CashQuickOption.Custom -> "🔢 Інша"
                                        },
                                        fontWeight = if (isSelected || option is CashQuickOption.Exact) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = contentColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Індикатор отриманих коштів та розрахунок решти з адаптацією для вузьких екранів
            state.receivedCashKopecks?.let { received ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 280.dp

                    val receivedRow = @Composable {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Отримано: ${PosCurrencyFormatter.formatKopecks(received)} грн",
                                fontSize = if (isNarrow) 11.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(PosEvent.SetReceivedCash(null))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    val changeBadge = @Composable {
                        when {
                            state.changeKopecks > 0L -> {
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "РЕШТА: ${PosCurrencyFormatter.formatKopecks(state.changeKopecks)} грн",
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isNarrow) 11.sp else 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            received < state.totalPayableKopecks -> {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "Не вистачає: ${PosCurrencyFormatter.formatKopecks(state.totalPayableKopecks - received)} грн",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isNarrow) 10.sp else 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            else -> Text(
                                text = "✓ Без решти",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            receivedRow()
                            changeBadge()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            receivedRow()
                            changeBadge()
                        }
                    }
                }
            }
        }
    }

    if (isCustomAmountOpen) {
        PosCustomCashInputDialog(
            totalPayableKopecks = state.totalPayableKopecks,
            initialCashKopecks = state.receivedCashKopecks ?: 0L,
            onApply = { kopecks ->
                onEvent(PosEvent.SetReceivedCash(kopecks))
                isCustomAmountOpen = false
            },
            onDismiss = { isCustomAmountOpen = false }
        )
    }
}
