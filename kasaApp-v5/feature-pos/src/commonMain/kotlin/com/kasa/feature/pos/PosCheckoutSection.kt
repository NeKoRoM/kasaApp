package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
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
import com.kasa.model.PaymentType
import com.kasa.model.ReceiptType

/**
 * Сенсорна панель розрахунку, оплат та фіскалізації для смартфонів та POS-терміналів:
 * - Підсумок суми, знижок та округлення НБУ;
 * - Вибір типу оплати (готівка з калькулятором решти, картка, змішана, знижка на чек);
 * - Кнопки керування (очистити з підтвердженням, відкласти чек, перегляд стрічки);
 * - Велика фіскальна кнопка «ОПЛАТИТИ ТА НАДРУКУВАТИ» з тактильним вібровідгуком.
 */
@Composable
fun PosCheckoutSection(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isConfirmingClear by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 440.dp
        val isCompactHeight = maxHeight < 500.dp
        val isSale = state.receiptType == ReceiptType.SALE
        val payBtnMinHeight = if (isCompactHeight) 38.dp else 44.dp
        val spacerGap = if (isCompactHeight) 3.dp else 6.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Блок підсумку чека та знижок
            PosReceiptTotalSummary(
                selectedCustomer = state.selectedCustomer,
                receiptDiscountPercent = state.receiptDiscountPercent,
                rawTotalKopecks = state.rawTotalKopecks,
                roundingDifferenceKopecks = state.roundingDifferenceKopecks,
                receiptType = state.receiptType,
                totalPayableKopecks = state.totalPayableKopecks,
                onDetachCustomer = { onEvent(PosEvent.DetachCustomer) }
            )

            Spacer(modifier = Modifier.height(spacerGap))

            // 2. Кнопки вибору оплати та знижки на чек
            PosCheckoutPaymentTypeRow(
                selectedPaymentType = state.paymentType,
                hasItems = state.items.isNotEmpty(),
                receiptDiscountPercent = state.receiptDiscountPercent,
                payBtnMinHeight = payBtnMinHeight,
                isNarrow = isNarrow,
                isCompactHeight = isCompactHeight,
                onSetPaymentType = { onEvent(PosEvent.SetPaymentType(it)) },
                onPromptSplitPayment = { onEvent(PosEvent.PromptSplitPayment(true)) },
                onPromptReceiptDiscount = { onEvent(PosEvent.PromptReceiptDiscount(true)) }
            )

            // 3. Інтерактивний калькулятор решти для готівки
            if (state.paymentType == PaymentType.CASH && state.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 2.dp else 4.dp))
                PosCashChangeCalculatorView(state = state, onEvent = onEvent)
            }

            // 4. Підсумок комбінованої оплати
            if (state.paymentType == PaymentType.COMBINED && state.splitCashKopecks != null && state.splitCardKopecks != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💵 ${PosCurrencyFormatter.formatKopecks(state.splitCashKopecks)} грн + 💳 ${PosCurrencyFormatter.formatKopecks(state.splitCardKopecks)} грн",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(spacerGap))

            // 5. Кнопки керування чеком (Очистити / Відкласти / Відкладені / Перегляд)
            val actBtnMinHeight = if (isCompactHeight) 34.dp else 40.dp
            val fiscalBtnHeight = if (isCompactHeight) 42.dp else 48.dp

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isConfirmingClear = true
                    },
                    enabled = state.items.isNotEmpty() && !state.isFiscalProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = actBtnMinHeight),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) { Text("Очистити", fontSize = if (isNarrow || isCompactHeight) 10.sp else 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(PosEvent.PromptHoldReceipt(true))
                    },
                    enabled = state.items.isNotEmpty() && !state.isFiscalProcessing,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = actBtnMinHeight),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) { Text("⏸ Відкласти", fontSize = if (isNarrow || isCompactHeight) 10.sp else 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }

                if (state.heldReceipts.isNotEmpty()) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(PosEvent.ToggleHeldReceiptsDialog)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = actBtnMinHeight),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("📂 ${state.heldReceipts.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenPreview()
                    },
                    enabled = state.items.isNotEmpty() && !state.isFiscalProcessing,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = actBtnMinHeight),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) { Text("👁 Стрічка", fontSize = if (isNarrow || isCompactHeight) 10.sp else 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 8.dp))

            // Попередження про причину блокування оплати (акциз, готівка, зміна >24г, папір РРО)
            PosCheckoutBlockerBanner(
                state = state,
                isCompactHeight = isCompactHeight
            )

            // 6. Велика кнопка фіскалізації
            val fiscalButtonText = if (isSale) {
                if (isNarrow) "ОПЛАТИТИ" else "ОПЛАТИТИ ТА НАДРУКУВАТИ"
            } else {
                if (isNarrow) "ПОВЕРНУТИ" else "ПОВЕРНУТИ ТА ВИДАТИ КОШТИ"
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    PosAudioFeedback.playSuccessBeep()
                    onEvent(PosEvent.FiscalizeReceipt)
                },
                enabled = state.canPay,
                modifier = Modifier.fillMaxWidth().height(fiscalBtnHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (isSale) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                )
            ) {
                if (state.isFiscalProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(text = fiscalButtonText, fontWeight = FontWeight.Bold, fontSize = if (isNarrow) 12.sp else 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
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
