package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import com.kasa.model.ReceiptType
import java.util.Locale

/**
 * Компактна нижня панель дій кошика для смартфонів:
 * - Швидкі касові дії (очистити з підтвердженням, відкласти, знижка, перегляд стрічки);
 * - Попередження про блокування (акциз);
 * - Інформативний підсумок чека (позиції, одиниці, клієнт, знижка, заокруглення);
 * - Велика ергономічна кнопка переходу до оплати в зоні великого пальця.
 */
@Composable
fun PosMobileCartBottomBar(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenPayment: () -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSale = state.receiptType == ReceiptType.SALE
    val haptic = LocalHapticFeedback.current
    val hasItems = state.items.isNotEmpty()
    var isConfirmingClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Рядок швидких дій над чеком
        PosMobileCartActionsRow(
            hasItems = hasItems,
            isFiscalProcessing = state.isFiscalProcessing,
            receiptDiscountPercent = state.receiptDiscountPercent,
            onClearClick = { isConfirmingClear = true },
            onHoldClick = { onEvent(PosEvent.PromptHoldReceipt(true)) },
            onDiscountClick = { onEvent(PosEvent.PromptReceiptDiscount(true)) },
            onPreviewClick = onOpenPreview
        )

        // 2. Інформативний рядок підсумку складу кошика та знижок
        if (hasItems) {
            val totalUnits = remember(state.items) { state.items.sumOf { it.quantity } }
            val unitsStr = if (totalUnits % 1.0 == 0.0) "${totalUnits.toInt()} шт" else String.format(Locale.US, "%.2f", totalUnits)
            val hasDiscount = state.receiptDiscountPercent > 0.0
            val hasRounding = state.roundingDifferenceKopecks != 0L
            val hasCustomer = state.selectedCustomer != null

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 ${state.items.size} поз. ($unitsStr)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (hasCustomer) {
                        val cName = state.selectedCustomer?.name?.trim().orEmpty()
                        val parts = cName.split(Regex("\\s+"))
                        val shortName = if (parts.size >= 2) "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}." else cName.take(10)
                        Text(
                            text = "• 👤 $shortName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(PosEvent.PromptLoyaltyDialog(true))
                            }
                        )
                    } else {
                        Text(
                            text = "• 👤 +Клієнт",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(PosEvent.PromptLoyaltyDialog(true))
                            }
                        )
                    }

                    if (hasDiscount) {
                        val discountSum = Math.round(state.rawTotalKopecks * (state.receiptDiscountPercent / 100.0))
                        val pctStr = if (state.receiptDiscountPercent % 1.0 == 0.0) "${state.receiptDiscountPercent.toInt()}%" else "${state.receiptDiscountPercent}%"
                        Text(
                            text = "• -$pctStr: -${PosCurrencyFormatter.formatKopecks(discountSum)} ₴",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(PosEvent.PromptReceiptDiscount(true))
                            }
                        )
                    }

                    if (hasRounding) {
                        val roundSign = if (state.roundingDifferenceKopecks > 0) "+" else ""
                        Text(
                            text = "• Заокр: $roundSign${PosCurrencyFormatter.formatKopecks(state.roundingDifferenceKopecks)} ₴",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 3. Попередження про блокування оплати (акциз)
        if (state.hasUnstampedExcise) {
            val firstUnstamped = state.items.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (firstUnstamped != -1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(PosEvent.PromptExciseStamp(firstUnstamped))
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Введіть акцизну марку для товару перед оплатою",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ввести 🍾",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Головна велика кнопка переходу до оплати
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (state.hasUnstampedExcise) {
                    val firstUnstamped = state.items.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
                    if (firstUnstamped != -1) {
                        onEvent(PosEvent.PromptExciseStamp(firstUnstamped))
                    } else {
                        onOpenPayment()
                    }
                } else {
                    onOpenPayment()
                }
            },
            enabled = !state.isFiscalProcessing && state.items.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                contentColor = if (isSale) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSale) "ОПЛАТИТИ" else "ПОВЕРНУТИ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${PosCurrencyFormatter.formatKopecks(state.totalPayableKopecks)} грн ➔",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }

    if (isConfirmingClear && state.items.isNotEmpty()) {
        PosCartClearConfirmDialog(
            itemsCount = state.items.size,
            totalPayableKopecks = state.totalPayableKopecks,
            onConfirm = {
                isConfirmingClear = false
                onEvent(PosEvent.ClearReceipt)
            },
            onHoldOrder = {
                isConfirmingClear = false
                onEvent(PosEvent.PromptHoldReceipt(true))
            },
            onDismiss = { isConfirmingClear = false }
        )
    }
}
