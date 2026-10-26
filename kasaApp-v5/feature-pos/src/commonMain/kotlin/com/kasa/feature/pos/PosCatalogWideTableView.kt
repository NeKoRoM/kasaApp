package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * Повнорозмірна таблиця номенклатури з горизонтальним скролом для планшетів та десктопів (>= 600dp).
 */
@Composable
fun PosCatalogWideTableView(
    products: List<Product>,
    cartItems: List<ReceiptItem>,
    onAddProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onQuickStock: (Product) -> Unit,
    onPriceTag: (Product) -> Unit,
    onDuplicateProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val cartQtyMap = remember(cartItems) {
        cartItems.groupBy { it.productId }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    }

    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔍", fontSize = 32.sp)
                Text(
                    text = "Товарів не знайдено",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Спробуйте змінити критерії пошуку або обрати іншу категорію",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Box(modifier = modifier.horizontalScroll(scrollState)) {
            Column(modifier = Modifier.width(890.dp).fillMaxHeight()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PLU", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                        Text("Назва товару", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(220.dp))
                        Text("Штрих-код", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                        Text("Категорія", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                        Text("Залишок", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                        Text("Ціна", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                        Text("Дії", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(210.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val inCartQty = cartQtyMap[product.id] ?: 0.0
                        val hasInCart = inCartQty > 0.0
                        val inCartBadgeText = if (inCartQty % 1.0 == 0.0) inCartQty.toInt().toString() else inCartQty.toString()

                        Surface(
                            color = when {
                                hasInCart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                product.isOutOfStock && product.minRemainder > 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = if (hasInCart) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth().clickable { onAddProduct(product) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "#${product.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(50.dp)
                                )

                                Row(modifier = Modifier.width(220.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (hasInCart) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                "✓$inCartBadgeText",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        product.printableName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (product.isFavorite) Text(" ⭐", fontSize = 9.sp)
                                    if (product.isWeighted) Text(" ⚖", fontSize = 9.sp)
                                    if (product.requiresExciseStamp) Text(" 🍾", fontSize = 9.sp)
                                }

                                Text(
                                    product.barcodes.firstOrNull() ?: "—",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(130.dp)
                                )

                                Text(
                                    product.categoryName.ifBlank { "—" },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(120.dp)
                                )

                                val formattedRem = if (product.remainder % 1.0 == 0.0) "${product.remainder.toLong()}" else String.format(java.util.Locale.US, "%.2f", product.remainder).trimEnd('0').trimEnd('.')
                                val remainderText = "$formattedRem ${product.unitOfMeasurement}"
                                val remainderColor = when {
                                    product.isOutOfStock && product.minRemainder > 0.0 -> MaterialTheme.colorScheme.error
                                    product.isLowStock -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = remainderText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = remainderColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(80.dp)
                                )

                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.2f", product.price)} ₴",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(80.dp)
                                )

                                Row(
                                    modifier = Modifier.width(210.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { onAddProduct(product) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) { Text("🛒", fontSize = 13.sp, maxLines = 1, softWrap = false) }

                                    OutlinedButton(
                                        onClick = { onQuickStock(product) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) { Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }

                                    IconButton(onClick = { onEditProduct(product) }, modifier = Modifier.size(40.dp)) {
                                        Text("✎", fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { onPriceTag(product) }, modifier = Modifier.size(40.dp)) {
                                        Text("🖨", fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { onDuplicateProduct(product) }, modifier = Modifier.size(40.dp)) {
                                        Text("📋", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
