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
import java.util.Locale

/**
 * Картка товару у сітці каталогу (Grid View):
 * - Оптимізована під швидкий вибір пальцем на сенсорних смартфонах;
 * - Підтримка короткого кліку для додавання та довгого кліку (Long Press) для дій;
 * - Підсвічування та бейдж кількості в кошику (✓ Qty);
 * - Кольорова індикація залишку товару (зелений, низький - помаранчевий, немає - червоний);
 * - Меню швидких дій над товаром (редагування, швидкий склад, цінник).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosCatalogGridItemCard(
    product: Product,
    inCartQty: Double,
    onAddProduct: (Product) -> Unit,
    onOpenActions: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasInCart = inCartQty > 0.0

    Card(
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
            ),
        shape = RoundedCornerShape(10.dp),
        border = if (hasInCart) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasInCart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                product.isOutOfStock && product.minRemainder > 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasInCart) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "#${product.id}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.isFavorite) Text("⭐", fontSize = 9.sp)
                    if (product.isWeighted) Text("⚖", fontSize = 9.sp)
                    if (product.requiresExciseStamp) Text("🍾", fontSize = 9.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasInCart) {
                        val badgeText = if (inCartQty % 1.0 == 0.0) "${inCartQty.toInt()}" else String.format(Locale.US, "%.2f", inCartQty)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "✓ $badgeText",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenActions(product)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("⋮", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.printableName,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2,
                lineHeight = 15.sp,
                fontSize = 12.sp,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val priceKop = Math.round(product.price * 100)
                Text(
                    text = "${PosCurrencyFormatter.formatKopecks(priceKop)} грн",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val formattedRem = if (product.remainder % 1.0 == 0.0) {
                    "${product.remainder.toLong()}"
                } else {
                    String.format(Locale.US, "%.2f", product.remainder).trimEnd('0').trimEnd('.')
                }
                val isCriticalOut = product.isOutOfStock && product.minRemainder > 0.0
                val stockText = when {
                    isCriticalOut -> "Немає"
                    product.isLowStock -> "⚠️ $formattedRem ${product.unitOfMeasurement}"
                    else -> "$formattedRem ${product.unitOfMeasurement}"
                }
                val stockColor = when {
                    isCriticalOut -> MaterialTheme.colorScheme.error
                    product.isLowStock -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = stockText,
                    color = stockColor,
                    fontWeight = if (isCriticalOut || product.isLowStock) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
