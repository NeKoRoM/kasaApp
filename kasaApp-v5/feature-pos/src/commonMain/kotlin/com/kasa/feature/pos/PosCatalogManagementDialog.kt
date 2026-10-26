package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasa.model.Product
import com.kasa.model.ProductCsvParser

/**
 * Повнорозмірний модуль управління номенклатурою, складом, собівартістю та імпортом/експортом CSV.
 */
@Composable
fun PosCatalogManagementDialog(
    products: List<Product>,
    onEvent: (PosEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var stockAdjustProductId by remember { mutableStateOf<Long?>(null) }
    var isCsvImportOpen by remember { mutableStateOf(false) }
    var isCsvExportOpen by remember { mutableStateOf(false) }
    var csvExportContent by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val filtered = products.filter { p ->
        val matchesCategory = when (selectedCategory) {
            "PIECE" -> !p.isWeighted
            "WEIGHTED" -> p.isWeighted
            "EXCISE" -> p.requiresExciseStamp || p.taxGroup.hasExcise
            "OUT_OF_STOCK" -> p.isOutOfStock
            "LOW_STOCK" -> p.isLowStock
            "ALL" -> true
            else -> p.categoryName.equals(selectedCategory, ignoreCase = true)
        }
        val q = searchQuery.trim()
        val matchesSearch = if (q.isBlank()) true else {
            com.kasa.model.KeyboardLayoutCorrector.matchesQuery(p.name, q) || p.id.toString() == q ||
            p.barcodes.any { it.contains(q) } || p.uktzed.contains(q) || com.kasa.model.KeyboardLayoutCorrector.matchesQuery(p.categoryName, q)
        }
        matchesCategory && matchesSearch
    }.sortedBy { it.id }

    val totalRetail = products.fold(0.0) { acc, p -> acc + (p.price * p.remainder) }
    val totalCost = products.fold(0.0) { acc, p -> acc + (p.costPrice * p.remainder) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isMobile = maxWidth < 600.dp
            Surface(
                modifier = if (isMobile) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.92f)
                },
                shape = if (isMobile) androidx.compose.foundation.shape.RoundedCornerShape(0.dp) else androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = if (isMobile) 10.dp else 16.dp, vertical = if (isMobile) 8.dp else 14.dp)
                ) {
                    PosCatalogManagementTopBar(
                        skuCount = products.size,
                        totalRetail = totalRetail,
                        totalCost = totalCost,
                        onDismiss = onDismiss,
                        onAddProduct = { onEvent(PosEvent.PromptProductEdit(null, null)) },
                        onReprice = { onEvent(PosEvent.ToggleBatchRepricingDialog(true)) },
                        onExportCsv = {
                            csvExportContent = ProductCsvParser.exportToCsv(products)
                            isCsvExportOpen = true
                        },
                        onImportCsv = { isCsvImportOpen = true }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Пошук товару, коду, категорії...",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(38.dp)) {
                                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onEvent(PosEvent.ToggleScannerDialog(true)) }, modifier = Modifier.size(38.dp)) {
                                    Text("📷", fontSize = 15.sp)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val baseCats = listOf(
                            "ALL" to "Всі (${products.size})",
                            "PIECE" to "Штучні",
                            "WEIGHTED" to "Вагові",
                            "EXCISE" to "Підакцизні",
                            "LOW_STOCK" to "Закінчуються (${products.count { it.isLowStock }})",
                            "OUT_OF_STOCK" to "Відсутні (${products.count { it.isOutOfStock }})"
                        )
                        items(baseCats) { (catKey, catLabel) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                                label = { Text(catLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { product ->
                            PosCatalogManagementItemCard(
                                product = product,
                                onAdjustStock = { stockAdjustProductId = product.id },
                                onAddToCart = { onEvent(PosEvent.AddProduct(product)) },
                                onToggleFavorite = { onEvent(PosEvent.ToggleProductFavorite(product.id)) },
                                onPrintPriceTag = { onEvent(PosEvent.PromptPriceTag(product)) },
                                onDuplicate = { onEvent(PosEvent.DuplicateProduct(product)) },
                                onEdit = { onEvent(PosEvent.PromptProductEdit(product)) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Діалог управління залишками (Прихід / Списання / Інвентаризація)
    stockAdjustProductId?.let { prodId ->
        val prod = products.find { it.id == prodId }
        if (prod != null) {
            PosCatalogStockAdjustDialog(
                product = prod,
                onApply = { mode, qty ->
                    when (mode) {
                        "ADD" -> onEvent(PosEvent.AdjustProductStock(prodId, qty))
                        "SUB" -> onEvent(PosEvent.AdjustProductStock(prodId, -qty))
                        "SET" -> onEvent(PosEvent.SetProductStockExact(prodId, qty))
                    }
                    stockAdjustProductId = null
                },
                onDismiss = { stockAdjustProductId = null }
            )
        }
    }

    // Діалог імпорту CSV
    if (isCsvImportOpen) {
        PosCatalogCsvImportDialog(
            onImport = { csvText -> onEvent(PosEvent.ImportProductsFromCsv(csvText)) },
            onDismiss = { isCsvImportOpen = false }
        )
    }

    // Діалог експорту CSV
    if (isCsvExportOpen) {
        PosCatalogCsvExportDialog(
            csvContent = csvExportContent,
            onDismiss = { isCsvExportOpen = false }
        )
    }
}
