package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.PaymentType

/**
 * Рядок перемикання способів оплати (Готівка / Картка / Змішана) та виклику знижки на чек.
 */
@Composable
fun PosCheckoutPaymentTypeRow(
    selectedPaymentType: PaymentType,
    hasItems: Boolean,
    receiptDiscountPercent: Double,
    payBtnMinHeight: Dp,
    isNarrow: Boolean,
    isCompactHeight: Boolean,
    onSetPaymentType: (PaymentType) -> Unit,
    onPromptSplitPayment: () -> Unit,
    onPromptReceiptDiscount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val paymentTypes = listOf(
        Triple(PaymentType.CASH, if (isNarrow) "💵 Гот" else "💵 Готівка") { onSetPaymentType(PaymentType.CASH) },
        Triple(PaymentType.CARD, if (isNarrow) "💳 Карт" else "💳 Картка") { onSetPaymentType(PaymentType.CARD) },
        Triple(PaymentType.COMBINED, if (isNarrow) "⚖ Зміш" else "⚖ Змішана") { onPromptSplitPayment() }
    )

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        paymentTypes.forEach { (type, label, action) ->
            val isSelected = selectedPaymentType == type
            val isSplit = type == PaymentType.COMBINED
            val container = when {
                isSelected && isSplit -> MaterialTheme.colorScheme.secondary
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val content = when {
                isSelected && isSplit -> MaterialTheme.colorScheme.onSecondary
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    action()
                },
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = payBtnMinHeight),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = if (isNarrow || isCompactHeight) 10.sp else 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPromptReceiptDiscount()
            },
            enabled = hasItems,
            modifier = Modifier.weight(0.85f).defaultMinSize(minHeight = payBtnMinHeight),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val pctStr = if (receiptDiscountPercent % 1.0 == 0.0) "${receiptDiscountPercent.toInt()}%" else "$receiptDiscountPercent%"
            Text(
                text = if (receiptDiscountPercent > 0.0) "% $pctStr" else if (isNarrow) "% Чек" else "% Знижка",
                fontSize = if (isNarrow || isCompactHeight) 10.sp else 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
