package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import java.util.Locale

/**
 * Сенсорне модальне вікно оприбуткування, списання та інвентаризації залишку товару:
 * - Швидкий вибір типу операції (+ Прихід, - Списання, Інвентаризація);
 * - Сенсорні чіпи швидких кількостей (+1, +5, +10, +20, +50);
 * - Повнорозмірні сенсорні кнопки (>= 44 dp) для роботи на екранах смартфонів;
 * - Валідація дробових та цілих чисел залишку.
 */
@Composable
fun PosCatalogStockAdjustDialog(
    product: Product,
    onApply: (mode: String, quantity: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stockAdjustMode by remember { mutableStateOf("ADD") } // "ADD", "SUB", "SET"
    val initialQty = if (product.isWeighted) "1.00" else "10"
    var stockQuantityStr by remember { mutableStateOf(initialQty) }
    var isFirstInput by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val remFormatted = if (product.remainder % 1.0 == 0.0) "${product.remainder.toInt()}" else String.format(Locale.US, "%.2f", product.remainder)
    val parsedQty = stockQuantityStr.replace(',', '.').toDoubleOrNull() ?: 0.0
    val projectedRemainder = when (stockAdjustMode) {
        "ADD" -> product.remainder + parsedQty
        "SUB" -> product.remainder - parsedQty
        "SET" -> parsedQty
        else -> product.remainder
    }
    val projFormatted = if (projectedRemainder % 1.0 == 0.0) "${projectedRemainder.toInt()}" else String.format(Locale.US, "%.2f", projectedRemainder)

    fun submit() {
        if (parsedQty > 0.0 || stockAdjustMode == "SET") {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onApply(stockAdjustMode, parsedQty)
        }
    }

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
                    Text(
                        text = "📦 Рух товару: ${product.printableName}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = when {
                        projectedRemainder < 0.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        stockAdjustMode == "ADD" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Поточний: $remFormatted ${product.unitOfMeasurement}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "➔ $projFormatted ${product.unitOfMeasurement}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (projectedRemainder < 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf("ADD" to "+ Прихід", "SUB" to "- Списати", "SET" to "= Факт")
                    modes.forEach { (mode, label) ->
                        FilterChip(
                            selected = stockAdjustMode == mode,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                stockAdjustMode = mode
                                isFirstInput = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp),
                            label = {
                                Text(
                                    label,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = stockQuantityStr,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (isFirstInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            stockQuantityStr = if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                            isFirstInput = false
                        } else {
                            stockQuantityStr = sanitized
                        }
                    },
                    label = {
                        val labelText = if (stockAdjustMode == "SET") "Точний залишок (${product.unitOfMeasurement})" else "Кількість (${product.unitOfMeasurement})"
                        Text(labelText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingIcon = {
                        if (stockQuantityStr.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        stockQuantityStr = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { submit() }, modifier = Modifier.size(32.dp)) {
                                    Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = if (product.isWeighted) listOf(0.5, 1.0, 2.0, 5.0, 10.0) else listOf(1.0, 5.0, 10.0, 20.0, 50.0)
                    for (amount in presets) {
                        val isInt = amount % 1.0 == 0.0
                        val amtStr = if (isInt) "${amount.toInt()}" else "$amount"
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                stockQuantityStr = amtStr
                                isFirstInput = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 36.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(amtStr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isNarrow) "Готово ↵" else "Застосувати ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
