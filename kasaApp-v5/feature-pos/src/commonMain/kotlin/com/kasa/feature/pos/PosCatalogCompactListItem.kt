package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.kasa.model.Product

/**
 * Елемент списку товарів у мобільному каталозі (1 колонка):
 * - Індикація наявності в кошику, залишку та ціни;
 * - Підтримка короткого кліку для додавання та довгого кліку (Long Press) для дій;
 * - Кнопка меню додаткових дій (редагування, швидкий склад, цінник);
 * - Швидка кнопка додавання (+1 / +вага) із тактильним відгуком.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosCatalogCompactListItem(
    product: Product,
    inCartQty: Double,
    onAddProduct: (Product) -> Unit,
    onOpenActions: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasInCart = inCartQty > 0.0
    val inCartBadgeText = if (inCartQty % 1.0 == 0.0) inCartQty.toInt().toString() else inCartQty.toString()

    Surface(
        color = when {
            hasInCart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            product.isOutOfStock && product.minRemainder > 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (hasInCart) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (hasInCart) 3.dp else 1.dp,
        shadowElevation = if (hasInCart) 1.dp else 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddProduct(product)
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenActions(product)
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${product.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = product.categoryName.ifBlank { "Загальна" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.isFavorite) Text("⭐", fontSize = 10.sp)
                    if (product.isWeighted) Text("⚖", fontSize = 10.sp)
                    if (product.requiresExciseStamp) Text("🍾", fontSize = 10.sp)
                }

                Text(
                    text = product.printableName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val remainderColor = when {
                    product.isOutOfStock && product.minRemainder > 0.0 -> MaterialTheme.colorScheme.error
                    product.isLowStock -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
                val formattedRem = if (product.remainder % 1.0 == 0.0) {
                    "${product.remainder.toLong()}"
                } else {
                    String.format(java.util.Locale.US, "%.2f", product.remainder).trimEnd('0').trimEnd('.')
                }
                val stockText = "Залишок: $formattedRem ${product.unitOfMeasurement}"
                val barcodeText = product.barcodes.firstOrNull()
                val subtitleText = if (!barcodeText.isNullOrBlank()) "$stockText • ШК: $barcodeText" else stockText
                Text(
                    text = subtitleText,
                    fontSize = 11.sp,
                    color = remainderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val priceKop = Math.round(product.price * 100)
                Text(
                    text = "${PosCurrencyFormatter.formatKopecks(priceKop)} грн",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenActions(product)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("⋮", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddProduct(product)
                        },
                        colors = if (hasInCart) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        val addLabel = when {
                            hasInCart && product.isWeighted -> "✓ $inCartBadgeText вага"
                            hasInCart -> "✓ $inCartBadgeText (+1)"
                            product.isWeighted -> "+вага ⚖"
                            else -> "+1 🛒"
                        }
                        Text(
                            text = addLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
