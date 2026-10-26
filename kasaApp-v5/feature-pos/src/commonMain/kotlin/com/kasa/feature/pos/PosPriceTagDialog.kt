package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.kasa.model.PriceTagRenderer
import com.kasa.model.Product

/**
 * Модальний діалог перегляду та друку поличкового цінника товару.
 * Адаптований для смартфонів та десктопів з можливістю вибору кількості копій.
 */
@Composable
fun PosPriceTagDialog(
    product: Product,
    onPrintToKasa: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var copyCount by remember { mutableStateOf(1) }
    val haptic = LocalHapticFeedback.current

    val tagText = PriceTagRenderer.renderPriceTag(
        product = product,
        storeName = "KASA-POS СУПЕРМАРКЕТ",
        printDate = java.time.LocalDate.now().toString()
    )

    BoxWithConstraints(modifier = modifier) {
        val isCompactHeight = maxHeight < 500.dp
        val isNarrow = maxWidth < 440.dp
        val maxPreviewHeight = if (isCompactHeight) 140.dp else 260.dp

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🖨 Цінник: ${product.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Інформаційний бейдж товару
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PLU #${product.id}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${PosCurrencyFormatter.formatKopecks(Math.round(product.price * 100.0))} грн/${product.unitOfMeasurement}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Вибір кількості цінників
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNarrow) "Копій:" else "Кількість копій:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2, 3, 5).forEach { count ->
                                FilterChip(
                                    selected = copyCount == count,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        copyCount = count
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                    label = { Text("$count шт", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isNarrow) "Попередній перегляд (57мм):" else "Попередній перегляд для термодруку 57мм:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxPreviewHeight)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val fullPrintText = (1..copyCount).joinToString("\n\n") { tagText }
                        onPrintToKasa(fullPrintText)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(
                        text = if (copyCount > 1) "🖨 Друк ($copyCount)" else "🖨 Друк",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Закрити", maxLines = 1, softWrap = false)
                }
            },
            modifier = Modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
        )
    }
}
