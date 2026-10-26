package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

/**
 * Діалог встановлення або зміни ціни товару (вільна ціна / ручна корекція).
 * Оптимізований під смартфони (Android / iOS):
 * - Мобільна клавіатура десяткових чисел (KeyboardType.Decimal);
 * - Скрол контенту при піднятті наекранної клавіатури;
 * - Тактильний вібровідгук на пресети швидкої ціни.
 */
@Composable
fun PosPricePromptDialog(
    productName: String,
    currentPriceKopecks: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPriceUah = if (currentPriceKopecks > 0L) {
        String.format(java.util.Locale.US, "%.2f", currentPriceKopecks / 100.0)
    } else {
        ""
    }
    var priceText by remember { mutableStateOf(initialPriceUah) }
    var isFirstInput by remember { mutableStateOf(initialPriceUah.isNotEmpty()) }
    var isError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    fun submit() {
        val parsed = priceText.toDoubleOrNull()
        if (parsed != null && parsed > 0.0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val kopecks = Math.round(parsed * 100.0)
            onConfirm(kopecks)
        } else {
            isError = true
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
                    text = "🏷 Введіть ціну товару",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = productName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Товар із вільною ціною або потребує підтвердження суми продажу:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        val filtered = it.replace(',', '.')
                        if (isFirstInput && filtered.isNotEmpty()) {
                            val lastChar = filtered.last()
                            priceText = if (lastChar.isDigit()) lastChar.toString() else if (lastChar == '.') "0." else filtered
                            isFirstInput = false
                        } else if (filtered.count { it == '.' } <= 1 && filtered.all { it.isDigit() || it == '.' }) {
                            priceText = filtered
                        }
                        if (isError && (priceText.toDoubleOrNull() ?: 0.0) > 0.0) isError = false
                    },
                    label = { Text("Ціна за одиницю (грн) *") },
                    placeholder = { Text("0.00") },
                    isError = isError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        if (priceText.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFirstInput = false
                                        priceText = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { submit() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    supportingText = {
                        if (isError) {
                            Text("Введіть коректну додатну ціну (наприклад: 25.50)", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Ціна у гривнях з точністю до копійок")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    if (currentPriceKopecks > 0L) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFirstInput = false
                                    priceText = String.format(java.util.Locale.US, "%.2f", currentPriceKopecks / 100.0)
                                    isError = false
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "↺ Базова (${PosCurrencyFormatter.formatKopecks(currentPriceKopecks)}₴)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    val presets = listOf(10, 20, 50, 100, 200, 500)
                    items(presets) { preset ->
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isFirstInput = false
                                priceText = "$preset.00"
                                isError = false
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("${preset}₴", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text(if (isNarrow) "Готово ↵" else "Встановити ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
