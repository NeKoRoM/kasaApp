package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.BarcodeSheetRenderer
import com.kasa.model.Product

/**
 * Діалог попереднього перегляду та друку аркуша/стрічки штрих-кодів товарів.
 * Дозволяє вибрати групу товарів (фаворити, категорія, всі) та роздрукувати на термопринтері РРО.
 */
@Composable
fun PosBarcodeSheetDialog(
    products: List<Product>,
    availableCategories: List<String>,
    onPrintToKasa: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("FAVORITES") }
    val haptic = LocalHapticFeedback.current

    val filteredProducts = when (selectedCategory) {
        "FAVORITES" -> products.filter { it.isFavorite }
        "ALL" -> products
        else -> products.filter { it.categoryName.equals(selectedCategory, ignoreCase = true) }
    }

    val sheetTitle = when (selectedCategory) {
        "FAVORITES" -> "ШВИДКІ ТОВАРИ (ФАВОРИТИ)"
        "ALL" -> "ПОВНИЙ КАТАЛОГ ШТРИХ-КОДІВ"
        else -> "КАТАЛОГ: ${selectedCategory.uppercase()}"
    }

    val renderedSheet = BarcodeSheetRenderer.renderBarcodeSheet(
        title = sheetTitle,
        products = filteredProducts,
        categoryName = if (selectedCategory in listOf("ALL", "FAVORITES")) null else selectedCategory
    )

    BoxWithConstraints(modifier = modifier) {
        val isNarrow = maxWidth < 440.dp

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "🖨 Друк штрих-кодів (${filteredProducts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Термострічка 57мм для сканування",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxPreviewHeight = if (maxHeight < 500.dp) 130.dp else 240.dp

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Вибір товарної групи для друку
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "FAVORITES",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = "FAVORITES"
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                label = { Text("⭐ Фаворити (${products.count { it.isFavorite }})", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = "ALL"
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                label = { Text("Всі товари (${products.size})", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                        items(availableCategories) { cat ->
                            val catCount = products.count { it.categoryName.equals(cat, ignoreCase = true) }
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = cat
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                label = { Text("$cat ($catCount)", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text("Попередній перегляд (57мм):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = maxPreviewHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Немає товарів у вибраній категорії для друку.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = renderedSheet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    PosAudioFeedback.playScanBeep()
                    onPrintToKasa(renderedSheet)
                    onDismiss()
                },
                enabled = filteredProducts.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isNarrow) "🖨 Друк" else "🖨 Друк на РРО", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Закрити", fontSize = 13.sp, maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 560.dp)
        )
    }
}
