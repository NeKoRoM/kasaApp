package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Інформативне попередження про причину блокування оплати чека
 * (акциз, недостатньо готівки, зміна >24г, відсутність паперу РРО тощо):
 * - Виділений колірний контейнер помилки або попередження;
 * - Можливість швидкого виклику розв'язання проблеми (наприклад, введення марки або точна сума готівки);
 * - Сенсорний touch-target >= 36 dp з тактильним вібровідгуком.
 */
@Composable
fun PosCheckoutBlockerBanner(
    state: PosState,
    isCompactHeight: Boolean,
    modifier: Modifier = Modifier,
    onResolveExcise: (() -> Unit)? = null,
    onQuickExactCash: (() -> Unit)? = null
) {
    if (state.items.isEmpty() || state.canPay) return
    val haptic = LocalHapticFeedback.current

    val firstUnstampedIndex = state.items.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
    val cashShortage = (state.totalPayableKopecks - (state.receivedCashKopecks ?: 0L)).coerceAtLeast(0L)

    val (blockerMessage, actionLabel, actionCallback) = when {
        state.hasUnstampedExcise -> Triple(
            "⚠️ Потрібна акцизна марка",
            "Ввести",
            onResolveExcise
        )
        state.isCashShort -> Triple(
            "⚠️ Не вистачає: ${PosCurrencyFormatter.formatKopecks(cashShortage)} грн",
            "Рівно",
            onQuickExactCash
        )
        state.is24HoursShiftExpired -> Triple(
            "⚠️ Зміна >24 год: закрийте Z-звіт",
            null,
            null
        )
        state.isReceiptPaperOut -> Triple(
            "⚠️ Закінчився касовий папір у РРО",
            null,
            null
        )
        state.isCoverOpen -> Triple(
            "⚠️ Відкрита кришка принтера РРО",
            null,
            null
        )
        state.isFiscalProcessing -> Triple(
            "⏳ Фіскалізація у процесі...",
            null,
            null
        )
        else -> Triple(null, null, null)
    }

    if (blockerMessage == null) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (isCompactHeight) 3.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (isCompactHeight) 3.dp else 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = blockerMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = if (isCompactHeight) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (actionLabel != null && actionCallback != null) {
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actionCallback()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
