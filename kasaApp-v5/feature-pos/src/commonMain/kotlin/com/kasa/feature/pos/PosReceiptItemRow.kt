package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.ReceiptItem

/**
 * Рядок товару в чековій стрічці касового терміналу.
 * Оптимізований під роботу одним пальцем на екранах смартфонів (Android / iPhone):
 * - Сенсорний степер кількості [-] QTY [+] прямо на рядку без модальних вікон;
 * - Тактильний вібровідгук (Haptic Feedback) на кожне натискання;
 * - Швидкий доступ до зміни ціни, знижки та сканування акцизу.
 */
@Composable
fun PosReceiptItemRow(
    item: ReceiptItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onPromptExcise: () -> Unit,
    onPromptDiscount: () -> Unit,
    onPromptQuantity: () -> Unit = {},
    onPromptPrice: () -> Unit = {},
    onEditProduct: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 360.dp
        val btnSize = if (isNarrow) 36.dp else 40.dp
        val priceFontSize = if (isNarrow) 13.sp else 14.sp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Рівень 1: Назва товару (ліворуч) та підсумок позиції з можливою закресленою старою ціною (праворуч)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.productName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isNarrow) 13.sp else 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = (if (onEditProduct != null) Modifier.clickable { onEditProduct() } else Modifier).weight(1f, fill = false)
                    )
                    if (onEditProduct != null) {
                        Text(
                            text = "✎",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onEditProduct() }
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (item.discountPercent > 0.0) {
                        Text(
                            text = "${PosCurrencyFormatter.formatKopecks(item.rawTotalKopecks)} грн",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${PosCurrencyFormatter.formatKopecks(item.totalKopecks)} грн",
                        fontWeight = FontWeight.Bold,
                        fontSize = priceFontSize,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Проміжний рядок: Акцизна марка (якщо товар підакцизний)
            if (item.requiresExciseStamp) {
                PosReceiptItemExciseBadge(
                    exciseStamp = item.exciseStamp,
                    onPromptExcise = onPromptExcise,
                    onOpenScanner = onOpenScanner
                )
            }

            // Рівень 2: Степер кількості, ціна за одиницю та кнопки дій
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Степер кількості: [-] [ Qty ] [+]
                    PosReceiptItemStepper(
                        item = item,
                        onQuantityChange = onQuantityChange,
                        onRemove = onRemove,
                        onPromptQuantity = onPromptQuantity
                    )

                    // Ціна за 1 од. (клік відкриває діалог зміни ціни)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPromptPrice()
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "x ${PosCurrencyFormatter.formatKopecks(item.priceKopecks)} ₴ ✎",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Дії: Знижка (%) та Видалення (✕)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.discountPercent > 0.0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPromptDiscount()
                        },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val discStr = if (item.discountPercent % 1.0 == 0.0) "${item.discountPercent.toInt()}%" else "${item.discountPercent}%"
                            Text(
                                text = if (item.discountPercent > 0.0) "-$discStr" else "%",
                                color = if (item.discountPercent > 0.0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRemove()
                        },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✕",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
