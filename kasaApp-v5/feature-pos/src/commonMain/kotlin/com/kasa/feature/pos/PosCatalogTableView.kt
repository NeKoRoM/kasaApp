package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * Адаптивний вигляд каталогу товарів:
 * - Смартфони (< 600dp): 1-колонковий ергономічний список без горизонтального скролу з бейджами кошика;
 * - Планшети / Десктоп (>= 600dp): Повноцінна фіксована таблиця з розширеними діями;
 * - Дружня заглушка при порожньому списку товарів або невдалому пошуку.
 */
@Composable
fun PosCatalogTableView(
    products: List<Product>,
    onAddProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onQuickStock: (Product) -> Unit,
    onPriceTag: (Product) -> Unit,
    onDuplicateProduct: (Product) -> Unit,
    modifier: Modifier = Modifier,
    cartItems: List<ReceiptItem> = emptyList()
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📋", fontSize = 36.sp)
                Text(
                    text = "Каталог порожній",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Не знайдено товарів у цій категорії. Скиньте пошук або додайте новий товар через меню керування.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            PosCatalogCompactListView(
                products = products,
                cartItems = cartItems,
                onAddProduct = onAddProduct,
                onEditProduct = onEditProduct,
                onQuickStock = onQuickStock,
                onPriceTag = onPriceTag,
                onDuplicateProduct = onDuplicateProduct,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PosCatalogWideTableView(
                products = products,
                cartItems = cartItems,
                onAddProduct = onAddProduct,
                onEditProduct = onEditProduct,
                onQuickStock = onQuickStock,
                onPriceTag = onPriceTag,
                onDuplicateProduct = onDuplicateProduct,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
