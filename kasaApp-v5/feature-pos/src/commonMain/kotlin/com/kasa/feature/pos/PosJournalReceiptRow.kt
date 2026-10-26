package com.kasa.feature.pos

import androidx.compose.foundation.clickable
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
import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import com.kasa.model.ReceiptType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Рядок відображення чека в журналі змін.
 * Підтримує розрізнення чеків продажу та видаткових чеків повернення,
 * перегляд електронної стрічки та кнопку швидкого оформлення повернення.
 */
@Composable
fun PosJournalReceiptRow(
    receipt: Receipt,
    onClick: () -> Unit,
    onReturnReceipt: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val timeFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(receipt.createdAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 360.dp
            Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Чек №${receipt.fiscalCheckNumber ?: "Б/Н"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (receipt.receiptType == ReceiptType.RETURN) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "ПОВЕРНЕННЯ",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val (badgeContainer, badgeContent, badgeTitle) = when (receipt.paymentType) {
                    PaymentType.CASH -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, if (isNarrow) "ГОТ." else "ГОТІВКА")
                    PaymentType.CARD -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, if (isNarrow) "КАРТ." else "КАРТКА")
                    PaymentType.COMBINED -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, if (isNarrow) "ЗМІШ." else "ЗМІШАНА")
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeContainer
                ) {
                    Text(
                        text = badgeTitle,
                        color = badgeContent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false),
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${PosCurrencyFormatter.formatKopecks(receipt.totalPayableKopecks)} грн",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (receipt.receiptType == ReceiptType.RETURN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = receipt.items.joinToString("; ") { itm ->
                    val qStr = if (itm.quantity % 1.0 == 0.0) itm.quantity.toInt().toString() else String.format(Locale.US, "%.3f", itm.quantity)
                    val uStr = if (itm.unitOfMeasurement.isNotBlank()) " ${itm.unitOfMeasurement}" else ""
                    "${itm.productName} ($qStr$uStr)"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Деталі чека",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (receipt.fiscalCheckNumber != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ДПС ✓",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (receipt.receiptType == ReceiptType.SALE && onReturnReceipt != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReturnReceipt()
                        },
                        contentPadding = PaddingValues(horizontal = if (isNarrow) 6.dp else 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                    ) {
                        Text(if (isNarrow) "↩ Поверн." else "↩ Повернення", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
}
