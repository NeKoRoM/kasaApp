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

import com.kasa.model.ReceiptType

/**
 * Липка нижня панель кошика для вітрини товарів на смартфонах (Android та iOS):
 * - Розміщується безпосередньо в зоні доступності великого пальця (Thumb Zone);
 * - Постійна стабільна висота: усуває стрибки верстки при додаванні першого товару (No Layout Shift);
 * - Порожній стан: індикатор "Чек порожній" та швидка кнопка запуску сканера [📷 Сканер];
 * - Активний стан: кількість позицій, сума чека, кнопка камери та перехід до оплати [ОПЛАТА ➔];
 * - Запобігає перекриттю нижніх карток товарів плаваючими FAB-кнопками.
 */
@Composable
fun PosCompactCatalogBottomBar(
    itemsCount: Int,
    totalPayableKopecks: Long,
    receiptType: ReceiptType = ReceiptType.SALE,
    onOpenScanner: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscapeCompact: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val isSale = receiptType == ReceiptType.SALE
    val isEmpty = itemsCount == 0

    val vertPad = if (isLandscapeCompact) 3.dp else 8.dp
    val btnMinHeight = if (isLandscapeCompact) 36.dp else 44.dp
    val scannerBtnSize = if (isLandscapeCompact) 36.dp else 44.dp

    val surfaceColor = when {
        isEmpty -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isSale -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    Surface(
        tonalElevation = if (isEmpty) 2.dp else 8.dp,
        shadowElevation = if (isEmpty) 2.dp else 8.dp,
        color = surfaceColor,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 440.dp
            val horizontalPadding = if (isNarrow) 8.dp else 14.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = vertPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEmpty) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToCart()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSale) "🛒" else "↩️",
                            fontSize = if (isNarrow || isLandscapeCompact) 15.sp else 18.sp
                        )
                        Column {
                            Text(
                                text = if (isSale) "Чек порожній" else "Повернення",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isNarrow || isLandscapeCompact) 11.sp else 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Виберіть товар або скануйте",
                                fontSize = if (isNarrow || isLandscapeCompact) 9.5.sp else 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenScanner()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (isNarrow) 10.dp else 14.dp,
                            vertical = if (isLandscapeCompact) 3.dp else 8.dp
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)
                    ) {
                        Text("📷", fontSize = if (isLandscapeCompact) 14.sp else 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Сканер",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isNarrow) 11.sp else 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToCart()
                            }
                    ) {
                        val countLabel = if (isSale) {
                            if (isNarrow) "🛒 Позицій: $itemsCount" else "🛒 Позицій у чеку: $itemsCount"
                        } else {
                            if (isNarrow) "↩️ Повернення: $itemsCount" else "↩️ До повернення: $itemsCount"
                        }
                        Text(
                            text = countLabel,
                            fontSize = if (isNarrow || isLandscapeCompact) 10.sp else 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSale) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        val pricePrefix = if (isSale) "" else "-"
                        Text(
                            text = "$pricePrefix${PosCurrencyFormatter.formatKopecks(totalPayableKopecks)} грн",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isNarrow || isLandscapeCompact) 14.sp else 17.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenScanner()
                            },
                            modifier = Modifier.size(scannerBtnSize),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📷", fontSize = if (isLandscapeCompact) 15.sp else 18.sp)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToCart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                contentColor = if (isSale) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(
                                horizontal = if (isNarrow) 8.dp else 14.dp,
                                vertical = if (isLandscapeCompact) 3.dp else 8.dp
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)
                        ) {
                            val btnLabel = if (isSale) {
                                if (isNarrow) "ЧЕК ➔" else "ДО ЧЕКУ ➔"
                            } else {
                                if (isNarrow) "ПОВЕРН. ➔" else "ДО ЧЕКУ ➔"
                            }
                            Text(
                                text = btnLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
