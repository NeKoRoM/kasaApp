package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * 1-колонковий ергономічний список товарів каталогу для смартфонів (< 600dp).
 * Оптимізований для керування однією рукою, швидкого додавання в чек та перегляду залишків.
 */
@Composable
fun PosCatalogCompactListView(
    products: List<Product>,
    cartItems: List<ReceiptItem>,
    onAddProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onQuickStock: ((Product) -> Unit)? = null,
    onPriceTag: ((Product) -> Unit)? = null,
    onDuplicateProduct: ((Product) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeActionProduct by remember { mutableStateOf<Product?>(null) }
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
                    text = "Спробуйте змінити фільтр або очистити пошук",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(products, key = { it.id }) { product ->
                val inCartQty = cartQtyMap[product.id] ?: 0.0
                PosCatalogCompactListItem(
                    product = product,
                    inCartQty = inCartQty,
                    onAddProduct = onAddProduct,
                    onOpenActions = { activeActionProduct = it }
                )
            }
        }
    }

    activeActionProduct?.let { product ->
        PosCatalogProductActionDialog(
            product = product,
            onDismiss = { activeActionProduct = null },
            onEditProduct = onEditProduct,
            onQuickStock = onQuickStock,
            onPriceTag = onPriceTag,
            onDuplicateProduct = onDuplicateProduct
        )
    }
}
