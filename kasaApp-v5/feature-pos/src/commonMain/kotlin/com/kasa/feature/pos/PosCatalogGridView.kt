package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * Візуальна сітка карток товарів (Grid View) для сенсорних смартфонів та POS-моніторів:
 * - Великі сенсорні плитки для швидкого вибору товару пальцем;
 * - Візуальні бейджи наявності в активному кошику (✓ Qty) та підсвічування;
 * - Візуальні бейджи типу товару (обране, вага, акциз);
 * - Контекстне мобільне меню дій (Редагування, залишок, цінник, дублювання).
 */
@Composable
fun PosCatalogGridView(
    products: List<Product>,
    onAddProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onQuickStock: (Product) -> Unit,
    onPriceTag: (Product) -> Unit,
    onDuplicateProduct: (Product) -> Unit,
    modifier: Modifier = Modifier,
    cartItems: List<ReceiptItem> = emptyList()
) {
    var activeActionProduct by remember { mutableStateOf<Product?>(null) }
    val cartQtyMap = remember(cartItems) {
        cartItems.groupBy { it.productId }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    }

    if (products.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔍", fontSize = 32.sp)
                Text("Товарів не знайдено", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Спробуйте змінити категорію або пошуковий запит",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val gridColumns = if (maxWidth < 600.dp) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 140.dp)
            LazyVerticalGrid(
                columns = gridColumns,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val inCartQty = cartQtyMap[product.id] ?: 0.0
                    PosCatalogGridItemCard(
                        product = product,
                        inCartQty = inCartQty,
                        onAddProduct = onAddProduct,
                        onOpenActions = { activeActionProduct = it }
                    )
                }
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
