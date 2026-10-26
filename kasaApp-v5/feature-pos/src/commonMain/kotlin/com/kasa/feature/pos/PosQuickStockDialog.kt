package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
 * Швидке модальне вікно корекції та оприбуткування товару в 1 клік прямо з вітрини:
 * - Відображає поточний залишок та одиницю виміру (Locale.US форматування)
 * - Адаптивні пресети для штучних (+1..+50) та вагових (+0.5..+10 кг) товарів
 * - Швидке підтвердження (↵) у полі введення та клавіатурі для роботи одним пальцем
 */
@Composable
fun PosQuickStockDialog(
    product: Product,
    onAdjustStock: (Long, Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialDelta = if (product.isWeighted) "1.000" else "1.0"
    var deltaInput by remember { mutableStateOf(initialDelta) }
    var isFirstInput by remember { mutableStateOf(true) }
    var isNegative by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val parsedDelta = deltaInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val effectiveDelta = if (isNegative) -Math.abs(parsedDelta) else Math.abs(parsedDelta)
    val predictedRemainder = (product.remainder + effectiveDelta).coerceAtLeast(0.0)
    val isValid = parsedDelta > 0.0

    fun submit() {
        if (isValid && effectiveDelta != 0.0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAdjustStock(product.id, effectiveDelta)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📦 Швидкий рух товару",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "#${product.id} ${product.printableName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosQuickStockPreviewCard(
                        product = product,
                        predictedRemainder = predictedRemainder,
                        isNegative = isNegative,
                        isNarrow = isNarrow
                    )

                    // Пресети швидкого додавання/списання
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (product.isWeighted) listOf(0.5, 1.0, 2.0, 5.0, 10.0) else listOf(1.0, 5.0, 10.0, 20.0, 50.0)
                        presets.forEach { p ->
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFirstInput = false
                                    deltaInput = if (p % 1.0 == 0.0 && !product.isWeighted) p.toInt().toString() else String.format(Locale.US, "%.3f", p)
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                val label = if (p % 1.0 == 0.0) "+${p.toInt()}" else "+$p"
                                Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isNegative,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isNegative = false
                            },
                            label = { Text("+ Прихід", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                        )
                        FilterChip(
                            selected = isNegative,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isNegative = true
                            },
                            label = { Text("- Списання", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                        )
                    }

                    OutlinedTextField(
                        value = deltaInput,
                        onValueChange = { input ->
                            val sanitized = input.replace(',', '.')
                            if (isFirstInput && sanitized.isNotEmpty()) {
                                val lastChar = sanitized.last()
                                deltaInput = if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                                isFirstInput = false
                            } else if (sanitized.count { it == '.' } <= 1 && sanitized.all { it.isDigit() || it == '.' }) {
                                deltaInput = sanitized
                            }
                        },
                        label = { Text("Кількість (${product.unitOfMeasurement})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                if (deltaInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFirstInput = false
                                            deltaInput = ""
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (isValid) {
                                    IconButton(onClick = { submit() }, modifier = Modifier.size(40.dp)) {
                                        Text("↵", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submit() },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        contentColor = if (isNegative) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
                    ),
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    val actionName = if (isNegative) "Списати" else if (isNarrow) "+ Прихід" else "Оприбуткувати"
                    Text("$actionName ↵", fontWeight = FontWeight.Bold, fontSize = if (isNarrow) 12.sp else 14.sp, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) { Text("Скасувати", maxLines = 1, softWrap = false) }
            },
            modifier = Modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
