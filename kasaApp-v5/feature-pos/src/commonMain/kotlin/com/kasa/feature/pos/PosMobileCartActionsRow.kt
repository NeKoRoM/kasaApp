package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
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

/**
 * Рядок швидких касових дій над відкритим чеком на мобільному (Android та iOS):
 * - [🗑] Швидке очищення чека із підтвердженням
 * - [⏸ Відкласти] Відкладення чека в чергу з можливістю нотатки
 * - [% Знижка] Встановлення знижки на чек з індикацією поточного %
 * - [👁] Попередній перегляд чекової стрічки
 */
@Composable
fun PosMobileCartActionsRow(
    hasItems: Boolean,
    isFiscalProcessing: Boolean,
    receiptDiscountPercent: Double,
    onClearClick: () -> Unit,
    onHoldClick: () -> Unit,
    onDiscountClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (hasItems) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = if (hasItems) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClearClick()
            },
            enabled = hasItems && !isFiscalProcessing,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🗑",
                    fontSize = 15.sp,
                    color = if (hasItems) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }

        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHoldClick()
            },
            enabled = hasItems && !isFiscalProcessing,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Text("⏸ Відкласти", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }

        val hasDiscount = receiptDiscountPercent > 0.0
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDiscountClick()
            },
            enabled = hasItems && !isFiscalProcessing,
            shape = RoundedCornerShape(8.dp),
            border = if (hasDiscount) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) else ButtonDefaults.outlinedButtonBorder(enabled = hasItems && !isFiscalProcessing),
            colors = if (hasDiscount) {
                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            val discountLabel = if (hasDiscount) {
                val pct = if (receiptDiscountPercent % 1.0 == 0.0) receiptDiscountPercent.toInt().toString() else receiptDiscountPercent.toString()
                "%$pct Знижка"
            } else {
                "% Знижка"
            }
            Text(
                text = discountLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hasDiscount) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPreviewClick()
            },
            enabled = hasItems && !isFiscalProcessing,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("👁", fontSize = 15.sp)
            }
        }
    }
}
