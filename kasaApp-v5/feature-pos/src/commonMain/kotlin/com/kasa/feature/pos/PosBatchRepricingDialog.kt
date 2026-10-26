package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import java.util.Locale

/**
 * Модальний діалог масової переоцінки товарів (зміна цін за категорією у % або на фіксовану суму).
 * Оптимізований для мобільних пристроїв:
 * - Швидкі пресети відсотків (+5%, +10%, +15%, +20%, -5%, -10%) та гривень;
 * - Сенсорний Enter (↵) у полі введення та клавіатурі;
 * - Візуальний індикатор напрямку переоцінки (збільшення/зниження) та тактильний вібровідгук.
 */
@Composable
fun PosBatchRepricingDialog(
    availableCategories: List<String>,
    onApply: (category: String, percentDelta: Double, fixedDelta: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var mode by remember { mutableStateOf("PERCENT") } // "PERCENT" або "FIXED"
    var deltaValueStr by remember { mutableStateOf("10.0") }
    var isFirstInput by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val allCategories = listOf("ALL" to "Всі категорії") + availableCategories.map { it to it }
    val parsedDelta = deltaValueStr.replace(',', '.').toDoubleOrNull() ?: 0.0
    val isValid = parsedDelta != 0.0

    fun submit() {
        if (isValid) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (mode == "PERCENT") {
                onApply(selectedCategory, parsedDelta, 0.0)
            } else {
                onApply(selectedCategory, 0.0, parsedDelta)
            }
            onDismiss()
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
                        text = "🏷 Масова переоцінка",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Оберіть категорію та розмір зміни цін:", fontSize = 12.sp)

                // Вибір категорії
                Text("Категорія для переоцінки:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allCategories.size) { idx ->
                        val (catKey, catLabel) = allCategories[idx]
                        FilterChip(
                            selected = selectedCategory == catKey,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = catKey
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                            label = { Text(catLabel, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // Вибір режиму: % або грн
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "PERCENT",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mode = "PERCENT"
                            deltaValueStr = "10.0"
                            isFirstInput = true
                        },
                        label = { Text("У відсотках (%)", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                    )
                    FilterChip(
                        selected = mode == "FIXED",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mode = "FIXED"
                            deltaValueStr = "5.00"
                            isFirstInput = true
                        },
                        label = { Text("Фіксована (грн)", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp)
                    )
                }

                // Пресети швидкого вибору розміру переоцінки
                Text("Швидкі пресети:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = if (mode == "PERCENT") {
                        listOf(5.0, 10.0, 15.0, 20.0, -5.0, -10.0)
                    } else {
                        listOf(1.0, 2.0, 5.0, 10.0, -1.0, -5.0)
                    }
                    presets.forEach { p ->
                        val sign = if (p > 0) "+$p" else "$p"
                        val suffix = if (mode == "PERCENT") "%" else "₴"
                        val label = if (p % 1.0 == 0.0) {
                            val intVal = p.toInt()
                            (if (intVal > 0) "+$intVal" else "$intVal") + suffix
                        } else {
                            "$sign$suffix"
                        }
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                deltaValueStr = if (p % 1.0 == 0.0) p.toInt().toString() else String.format(Locale.US, "%.2f", p)
                                isFirstInput = false
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                OutlinedTextField(
                    value = deltaValueStr,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        val effective = if (isFirstInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            isFirstInput = false
                            if (lastChar.isDigit() || lastChar == '.' || lastChar == '-') lastChar.toString() else sanitized
                        } else {
                            sanitized
                        }
                        if (effective.count { it == '.' } <= 1 && effective.replace("-", "").all { it.isDigit() || it == '.' }) {
                            deltaValueStr = effective
                        }
                    },
                    label = { Text(if (mode == "PERCENT") "Зміна у % (напр. 5.0 або -3.0)" else "Зміна у грн (напр. 2.50 або -1.00)") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (deltaValueStr.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        deltaValueStr = ""
                                        isFirstInput = false
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )

                val directionText = when {
                    parsedDelta > 0.0 -> "🟢 Збільшення цін на: +$deltaValueStr ${if (mode == "PERCENT") "%" else "грн"}"
                    parsedDelta < 0.0 -> "🔴 Зниження цін на: $deltaValueStr ${if (mode == "PERCENT") "%" else "грн"}"
                    else -> "Введіть розмір переоцінки"
                }
                Text(directionText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isValid,
                contentPadding = PaddingValues(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text(if (isNarrow) "Готово ↵" else "Застосувати ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
        )
    }
}
