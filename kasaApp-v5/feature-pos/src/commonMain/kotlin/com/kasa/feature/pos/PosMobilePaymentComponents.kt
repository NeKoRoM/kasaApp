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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.PaymentType

/**
 * Велика картка підсумку до сплати / повернення з урахуванням заокруглення НБУ.
 */
@Composable
fun PosMobilePaymentTotalCard(
    totalPayableKopecks: Long,
    roundingDifferenceKopecks: Long,
    isSale: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSale) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSale) "ДО СПЛАТИ" else "ДО ПОВЕРНЕННЯ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = "${PosCurrencyFormatter.formatKopecks(totalPayableKopecks)} грн",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (roundingDifferenceKopecks != 0L) {
                Text(
                    text = "Округлення НБУ: ${if (roundingDifferenceKopecks > 0) "+" else ""}${roundingDifferenceKopecks} коп.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Рядок вибору типу оплати: Готівка, Картка, Змішана.
 */
@Composable
fun PosMobilePaymentTypeSelector(
    selectedType: PaymentType,
    onSelectCash: () -> Unit,
    onSelectCard: () -> Unit,
    onSelectSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectCash()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedType == PaymentType.CASH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selectedType == PaymentType.CASH) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Text("💵 Готівка", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectCard()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedType == PaymentType.CARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selectedType == PaymentType.CARD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Text("💳 Картка", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectSplit()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedType == PaymentType.COMBINED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selectedType == PaymentType.COMBINED) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Text("⚖ Змішана", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/**
 * Секція попереджень та блокувань фіскалізації (акциз, готівка, РРО зміна, папір).
 */
@Composable
fun PosMobileFiscalBlockersSection(
    state: PosState,
    onEnterExcise: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstUnstampedIndex = state.items.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
    val blockerMessage = when {
        state.isCashShort -> "⚠️ Недостатньо готівки від покупця"
        state.is24HoursShiftExpired -> "⚠️ Зміна > 24 год! Виконайте Z-звіт"
        state.isReceiptPaperOut -> "⚠️ Закінчився касовий папір у РРО"
        state.isCoverOpen -> "⚠️ Відкрита кришка принтера РРО"
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.hasUnstampedExcise && firstUnstampedIndex != -1) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Потрібна акцизна марка",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Button(
                        onClick = { onEnterExcise(firstUnstampedIndex) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("🍾 Ввести", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (blockerMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = blockerMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
