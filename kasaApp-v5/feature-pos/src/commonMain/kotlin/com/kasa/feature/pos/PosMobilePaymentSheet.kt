package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.PaymentType
import com.kasa.model.ReceiptType

/**
 * Мобільний лист розрахунку та фіскалізації (Square / Poster / Loyverse standard):
 * - Виділений екранний простір для швидкого вибору способу оплати
 * - Великі купюри НБУ та автоматичний калькулятор решти
 * - Фіскалізація в 1 дотик із тактильним вібровідгуком
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMobilePaymentSheet(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isSale = state.receiptType == ReceiptType.SALE

    LaunchedEffect(state.items.isEmpty()) {
        if (state.items.isEmpty()) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isFiscalProcessing) onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Заголовок та закриття
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSale) "Оплата замовлення" else "Повернення коштів",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Чек №${state.activeCartIndex + 1} • Позицій: ${state.items.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        if (!state.isFiscalProcessing) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Велика картка підсумку до сплати
            PosMobilePaymentTotalCard(
                totalPayableKopecks = state.totalPayableKopecks,
                roundingDifferenceKopecks = state.roundingDifferenceKopecks,
                isSale = isSale
            )

            // Вибір типу оплати (Готівка / Картка / Змішана)
            PosMobilePaymentTypeSelector(
                selectedType = state.paymentType,
                onSelectCash = { onEvent(PosEvent.SetPaymentType(PaymentType.CASH)) },
                onSelectCard = { onEvent(PosEvent.SetPaymentType(PaymentType.CARD)) },
                onSelectSplit = { onEvent(PosEvent.PromptSplitPayment(true)) }
            )

            // Деталі активного способу оплати
            when (state.paymentType) {
                PaymentType.CASH -> PosMobileCashSection(state = state, onEvent = onEvent)
                PaymentType.CARD -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💳 Безготівковий розрахунок", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Піднесіть картку або телефон до банківського POS-термінала", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                PaymentType.COMBINED -> {
                    if (state.splitCashKopecks != null && state.splitCardKopecks != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💵 ${PosCurrencyFormatter.formatKopecks(state.splitCashKopecks)} грн + 💳 ${PosCurrencyFormatter.formatKopecks(state.splitCardKopecks)} грн",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                OutlinedButton(
                                    onClick = { onEvent(PosEvent.PromptSplitPayment(true)) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) { Text("Змінити", fontSize = 11.sp) }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onEvent(PosEvent.PromptSplitPayment(true)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("🔀 Налаштувати суми частинами", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Попередження та блокування фіскалізації
            PosMobileFiscalBlockersSection(
                state = state,
                onEnterExcise = { index ->
                    onDismiss()
                    onEvent(PosEvent.PromptExciseStamp(index))
                }
            )

            // Головна велика кнопка фіскалізації
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    PosAudioFeedback.playSuccessBeep()
                    onEvent(PosEvent.FiscalizeReceipt)
                },
                enabled = state.canPay,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (isSale) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isFiscalProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
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
                            text = "${PosCurrencyFormatter.formatKopecks(state.totalPayableKopecks)} грн ↵",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
