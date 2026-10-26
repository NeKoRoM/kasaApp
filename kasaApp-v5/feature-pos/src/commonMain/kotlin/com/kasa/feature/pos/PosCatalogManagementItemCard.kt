package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product

/**
 * Картка товару в модулі складського обліку (PosCatalogManagementDialog).
 * Оптимізована під дисплеї смартфонів: без злипання тексту та зі зручними кнопками.
 */
@Composable
fun PosCatalogManagementItemCard(
    product: Product,
    onAdjustStock: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPrintPriceTag: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                product.isOutOfStock -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                product.isLowStock -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#${product.id}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Text(product.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (product.categoryName.isNotBlank()) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraSmall) {
                            Text(
                                text = product.categoryName,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (product.isFavorite) Text("⭐", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${String.format("%.2f", product.price)} ₴",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Собів: %.2f₴ (націнка %.0f%%)".format(product.costPrice, product.marginPercent),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val formattedRem = if (product.remainder % 1.0 == 0.0) "${product.remainder.toLong()}" else String.format(java.util.Locale.US, "%.2f", product.remainder).trimEnd('0').trimEnd('.')
                Text(
                    "Залишок: $formattedRem ${product.unitOfMeasurement}",
                    color = if (product.isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAdjustStock,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                ) {
                    Text("± Склад", fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onAddToCart,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                ) {
                    Text("🛒 В чек", fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                    Text(if (product.isFavorite) "★" else "☆", fontSize = 14.sp)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Text("✏", fontSize = 13.sp)
                }
                Box {
                    IconButton(onClick = { isMenuExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Text("⋮", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📋 Створити копію", fontSize = 12.sp) },
                            onClick = {
                                isMenuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🖨 Друк цінника", fontSize = 12.sp) },
                            onClick = {
                                isMenuExpanded = false
                                onPrintPriceTag()
                            }
                        )
                    }
                }
            }
        }
    }
}
