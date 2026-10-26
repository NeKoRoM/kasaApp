package com.kasa.feature.pos

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
import com.kasa.model.ReceiptItem
import java.util.Locale

/**
 * Сенсорне модальне вікно для швидкого та точного введення кількості або маси товару в чеку.
 * Оптимізоване під сенсорні телефони (Android / iOS):
 * - Мобільна десяткова клавіатура та сенсорний нумпад із клавішею Backspace;
 * - Тактильний вібровідгук на кожну цифру та крок степера;
 * - Адаптивна висота та ширина під екран смартфона.
 */
@Composable
fun PosQuantityDialog(
    item: ReceiptItem,
    onConfirm: (quantity: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialText = if (item.quantity % 1.0 == 0.0) {
        item.quantity.toInt().toString()
    } else {
        String.format(Locale.US, "%.3f", item.quantity)
    }

    var quantityText by remember { mutableStateOf(initialText) }
    var isFirstInput by remember { mutableStateOf(true) }
    val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
    val isValid = parsedQty > 0.0
    val liveSubtotalKopecks = Math.round(parsedQty * item.priceKopecks)
    val unitLabel = if (item.unitOfMeasurement.isNotBlank()) item.unitOfMeasurement else if (item.isWeighted) "кг" else "од."
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    fun submit() {
        if (isValid) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfirm(parsedQty)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isNarrow = maxWidth < 440.dp
        val isCompactHeight = maxHeight < 480.dp
        val keyHeight = if (isCompactHeight) 38.dp else 44.dp
        val presetHeight = if (isCompactHeight) 36.dp else 40.dp
        val spacing = if (isCompactHeight) 6.dp else 10.dp

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
                            text = if (item.isWeighted) "⚖️ Введення ваги" else "🔢 Кількість товару",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.productName} • ${PosCurrencyFormatter.formatKopecks(item.priceKopecks)} грн/$unitLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            val sanitized = input.replace(',', '.')
                            val effective = if (isFirstInput && sanitized.isNotEmpty()) {
                                val lastChar = sanitized.last()
                                isFirstInput = false
                                if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                            } else {
                                sanitized
                            }
                            if (effective.count { it == '.' } <= 1 && effective.all { it.isDigit() || it == '.' }) {
                                quantityText = effective
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        textStyle = LocalTextStyle.current.copy(fontSize = if (isCompactHeight) 18.sp else 20.sp, fontWeight = FontWeight.Bold),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                if (quantityText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFirstInput = false
                                            quantityText = quantityText.dropLast(1)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("⌫", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(text = unitLabel, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                if (isValid) {
                                    IconButton(onClick = { submit() }, modifier = Modifier.size(40.dp)) {
                                        Text("↵", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сума:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${PosCurrencyFormatter.formatKopecks(liveSubtotalKopecks)} грн",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (item.isWeighted) listOf(-0.1, 0.1, 0.25, 0.5, 1.0) else listOf(-1.0, 1.0, 2.0, 5.0, 10.0)
                        for (addQty in presets) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFirstInput = false
                                    val current = quantityText.toDoubleOrNull() ?: 0.0
                                    val minQty = if (item.isWeighted) 0.001 else 1.0
                                    val updated = (current + addQty).coerceAtLeast(minQty)
                                    quantityText = if (updated % 1.0 == 0.0 && !item.isWeighted) updated.toInt().toString() else String.format(Locale.US, "%.3f", updated)
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = presetHeight),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                val label = when {
                                    addQty < 0 && addQty % 1.0 == 0.0 -> "${addQty.toInt()}"
                                    addQty < 0 -> "$addQty"
                                    addQty % 1.0 == 0.0 -> "+${addQty.toInt()}"
                                    else -> "+$addQty"
                                }
                                Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    PosQuantityNumpad(
                        onDigit = { digit ->
                            if (isFirstInput) {
                                quantityText = digit
                                isFirstInput = false
                            } else {
                                quantityText = if (quantityText == "0") digit else quantityText + digit
                            }
                        },
                        onClear = {
                            quantityText = ""
                            isFirstInput = false
                        },
                        onDot = {
                            if (isFirstInput) {
                                quantityText = "0."
                                isFirstInput = false
                            } else if (!quantityText.contains('.')) {
                                quantityText = if (quantityText.isEmpty()) "0." else "$quantityText."
                            }
                        },
                        onBackspace = {
                            if (quantityText.isNotEmpty()) {
                                isFirstInput = false
                                quantityText = quantityText.dropLast(1)
                            }
                        },
                        keyHeight = keyHeight,
                        isCompactHeight = isCompactHeight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submit() },
                    enabled = isValid,
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) { Text(if (isNarrow) "Готово ↵" else "Застосувати ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) { Text("Скасувати", maxLines = 1, softWrap = false) }
            },
            modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 440.dp)
        )
    }
}
