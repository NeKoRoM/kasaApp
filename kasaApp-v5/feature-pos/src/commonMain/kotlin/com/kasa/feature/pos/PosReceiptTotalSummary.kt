package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Customer
import com.kasa.model.ReceiptType

/**
 * Блок підсумку чека: сума без знижки, знижка на чек, заокруглення НБУ,
 * дані клієнта лояльності та фінальна сума "ДО СПЛАТИ" / "ДО ПОВЕРНЕННЯ".
 */
@Composable
fun PosReceiptTotalSummary(
    selectedCustomer: Customer?,
    receiptDiscountPercent: Double,
    rawTotalKopecks: Long,
    roundingDifferenceKopecks: Long,
    receiptType: ReceiptType,
    totalPayableKopecks: Long,
    onDetachCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 440.dp
        val isCompactHeight = maxHeight < 500.dp
        val totalFontSize = if (isNarrow || isCompactHeight) 18.sp else 22.sp
        val titleFontSize = if (isNarrow || isCompactHeight) 12.sp else 15.sp
        val boxPadding = if (isCompactHeight) 6.dp else (if (isNarrow) 8.dp else 10.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(boxPadding)
        ) {
        selectedCustomer?.let { customer ->
            val custDiscount = if (customer.discountPercent % 1.0 == 0.0) "${customer.discountPercent.toInt()}" else "${customer.discountPercent}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${customer.name} (-$custDiscount%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDetachCustomer()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (receiptDiscountPercent > 0.0) {
            val discLabel = if (receiptDiscountPercent % 1.0 == 0.0) "${receiptDiscountPercent.toInt()}" else "$receiptDiscountPercent"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сума без знижки:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${PosCurrencyFormatter.formatKopecks(rawTotalKopecks)} грн",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Знижка на чек ($discLabel%):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                val discountSum = Math.round(rawTotalKopecks * (receiptDiscountPercent / 100.0))
                Text(
                    text = "-${PosCurrencyFormatter.formatKopecks(discountSum)} грн",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (roundingDifferenceKopecks != 0L) {
            val roundSign = if (roundingDifferenceKopecks > 0L) "+" else ""
            Text(
                text = "Заокруглення НБУ: $roundSign${PosCurrencyFormatter.formatKopecks(roundingDifferenceKopecks)} грн",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRet = receiptType == ReceiptType.RETURN
            val accColor = if (isRet) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Text(
                text = if (isRet) "ДО ПОВЕРНЕННЯ:" else "ДО СПЛАТИ:",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = if (isRet) accColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${PosCurrencyFormatter.formatKopecks(totalPayableKopecks)} грн",
                fontSize = totalFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = accColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
}
