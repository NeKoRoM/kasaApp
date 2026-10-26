package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Права панель робочого місця касира: вітрина товарів, швидкі фаворити, пошук та перемикання режимів відображення.
 * Адаптується під смартфони (isMobile) та широкоекранні планшети/ПК.
 */
@Composable
fun PosCatalogPanel(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    var sortMode by remember { mutableStateOf("PLU") }
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val isCompactWidth = maxWidth < 600.dp
        val isCompactHeight = maxHeight < 500.dp
        val isMobileDevice = isMobile || isCompactWidth || isCompactHeight
        val horizontalPadding = if (isMobileDevice) 4.dp else 8.dp
        val verticalPadding = if (isCompactHeight) 2.dp else 6.dp
        val itemSpacing = if (isCompactHeight) 3.dp else 6.dp
        val btnMinHeight = if (isCompactHeight) 34.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            // 1. Верхня панель / Пошуковий рядок з швидкими діями касира
            if (isMobileDevice) {
                PosMobileCatalogSearchBar(
                    searchQuery = state.searchQuery,
                    onUpdateSearchQuery = { onEvent(PosEvent.UpdateSearchQuery(it)) },
                    onScanBarcode = { onEvent(PosEvent.ScanBarcode(it)) },
                    onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) },
                    onOpenNumpad = { onEvent(PosEvent.ToggleNumpadDialog(true)) },
                    onAddProduct = { onEvent(PosEvent.PromptProductEdit(null, null)) },
                    focusManager = focusManager,
                    isCompactHeight = isCompactHeight
                )
            } else {
                PosDesktopCatalogHeader(
                    scaleWeight = state.scaleWeight,
                    onEvent = onEvent,
                    searchQuery = state.searchQuery,
                    focusManager = focusManager,
                    btnMinHeight = btnMinHeight
                )
            }

            // 2. Швидкі фаворити касира (на мобільних фаворити доступні через чіп "⭐ Швидкі")
            if (state.favoriteProducts.isNotEmpty() && !isMobileDevice) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(state.favoriteProducts) { fav ->
                        FilledTonalButton(
                            onClick = { onEvent(PosEvent.AddProduct(fav)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Text(
                                text = "⭐ ${fav.printableName.take(15)} (${String.format(java.util.Locale.US, "%.2f", fav.price)}₴)",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Панель фільтрації за категоріями
            PosCatalogCategoryChipsRow(
                selectedCategory = state.selectedCatalogCategory,
                availableCategories = state.availableCategories,
                onSelectCategory = { onEvent(PosEvent.SetCatalogCategoryFilter(it)) },
                isCompactHeight = isCompactHeight
            )

            // Фільтрація та сортування номенклатури
            val baseFiltered = state.catalogProducts.filter { product ->
                when (state.selectedCatalogCategory) {
                    "FAVORITES" -> product.isFavorite
                    "IN_STOCK" -> product.remainder > 0.0
                    "LOW_STOCK" -> product.isLowStock
                    "OUT_OF_STOCK" -> product.isOutOfStock
                    "PIECE" -> !product.isWeighted
                    "WEIGHTED" -> product.isWeighted
                    "EXCISE" -> product.requiresExciseStamp || product.taxGroup.hasExcise
                    "ALL" -> true
                    else -> product.categoryName.equals(state.selectedCatalogCategory, ignoreCase = true)
                }
            }
            val searchFiltered = if (state.searchQuery.isBlank()) {
                baseFiltered
            } else {
                val q = state.searchQuery.trim()
                baseFiltered.filter { product ->
                    com.kasa.model.KeyboardLayoutCorrector.matchesQuery(product.name, q) ||
                    com.kasa.model.KeyboardLayoutCorrector.matchesQuery(product.categoryName, q) ||
                    product.id.toString().contains(q) ||
                    product.barcodes.any { it.contains(q) } ||
                    product.uktzed.contains(q)
                }
            }
            val sortedList = when (sortMode) {
                "NAME" -> searchFiltered.sortedBy { it.name.lowercase() }
                "PRICE" -> searchFiltered.sortedBy { it.price }
                else -> searchFiltered.sortedBy { it.id }
            }

            // 4. Рядок сортування, перемикання вигляду та лічильник
            PosCatalogSortBar(
                isCatalogTableView = state.isCatalogTableView,
                onToggleViewMode = { onEvent(PosEvent.SetCatalogViewMode(it)) },
                sortMode = sortMode,
                onSortModeChange = { sortMode = it },
                filteredCount = sortedList.size,
                totalCount = state.catalogProducts.size,
                isCompact = isMobileDevice
            )

            // 5. Відображення каталогу: Таблиця або Сітка
            if (state.isCatalogTableView) {
                PosCatalogTableView(
                    products = sortedList,
                    cartItems = state.items,
                    onAddProduct = { onEvent(PosEvent.AddProduct(it)) },
                    onEditProduct = { onEvent(PosEvent.PromptProductEdit(it)) },
                    onQuickStock = { onEvent(PosEvent.PromptQuickStock(it)) },
                    onPriceTag = { onEvent(PosEvent.PromptPriceTag(it)) },
                    onDuplicateProduct = { onEvent(PosEvent.DuplicateProduct(it)) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PosCatalogGridView(
                    products = sortedList,
                    cartItems = state.items,
                    onAddProduct = { onEvent(PosEvent.AddProduct(it)) },
                    onEditProduct = { onEvent(PosEvent.PromptProductEdit(it)) },
                    onQuickStock = { onEvent(PosEvent.PromptQuickStock(it)) },
                    onPriceTag = { onEvent(PosEvent.PromptPriceTag(it)) },
                    onDuplicateProduct = { onEvent(PosEvent.DuplicateProduct(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
