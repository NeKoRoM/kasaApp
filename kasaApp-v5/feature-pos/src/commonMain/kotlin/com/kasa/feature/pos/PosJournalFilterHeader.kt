package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.PaymentType

/**
 * Панель фільтрів форми оплати для журналу чеків.
 */
@Composable
fun PosJournalPaymentFilterChips(
    selectedPaymentFilter: PaymentType?,
    totalReceiptsCount: Int,
    onSelectFilter: (PaymentType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val filters = remember(totalReceiptsCount) {
        listOf(
            Triple(null, "Всі ($totalReceiptsCount)", "Всі"),
            Triple(PaymentType.CASH, "Готівка", "Готівка"),
            Triple(PaymentType.CARD, "Картка", "Картка"),
            Triple(PaymentType.COMBINED, "Змішана", "Змішана")
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { (type, label, _) ->
            val isSelected = selectedPaymentFilter == type
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (type == null) {
                        onSelectFilter(null)
                    } else {
                        onSelectFilter(if (isSelected) null else type)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 36.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Зведений підсумок чеків за результатами пошуку та фільтрації.
 */
@Composable
fun PosJournalSummaryRow(
    receiptsCount: Int,
    totalCashKopecks: Long,
    totalCardKopecks: Long,
    isNarrow: Boolean,
    summaryPadding: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(summaryPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(0.8f, fill = false)) {
            Text("Знайдено:", fontSize = if (isNarrow) 10.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            Text("$receiptsCount", fontWeight = FontWeight.Bold, fontSize = if (isNarrow) 12.sp else 13.sp, maxLines = 1, softWrap = false)
        }
        Column(modifier = Modifier.weight(1.1f, fill = false)) {
            Text("Готівка:", fontSize = if (isNarrow) 10.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            Text("${PosCurrencyFormatter.formatKopecks(totalCashKopecks)} грн", fontWeight = FontWeight.Bold, fontSize = if (isNarrow) 11.sp else 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
        Column(modifier = Modifier.weight(1.1f, fill = false)) {
            Text("Картка:", fontSize = if (isNarrow) 10.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            Text("${PosCurrencyFormatter.formatKopecks(totalCardKopecks)} грн", fontWeight = FontWeight.Bold, fontSize = if (isNarrow) 11.sp else 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}
