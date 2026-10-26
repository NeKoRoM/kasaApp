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

/**
 * Діалогове вікно встановлення відсоткової знижки на товар або на весь чек.
 * Оптимізоване під сенсорні смартфони (Android / iOS):
 * - Мобільна десяткова клавіатура (KeyboardType.Decimal);
 * - Візуальний онлайн-розрахунок суми знижки та фінальної ціни в грн;
 * - Вібро-відгук при виборі відсоткових пресетів (0%, 3%, 5%, 7%, 10%, 15%, 20%, 25%, 50%);
 * - Захист від перекриття віртуальною клавіатурою (imePadding).
 */
@Composable
fun PosDiscountDialog(
    title: String,
    targetName: String,
    currentDiscountPercent: Double,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    rawTotalKopecks: Long? = null
) {
    val initialDiscountStr = if (currentDiscountPercent > 0.0) {
        if (currentDiscountPercent % 1.0 == 0.0) "${currentDiscountPercent.toInt()}" else "$currentDiscountPercent"
    } else {
        ""
    }
    var discountText by remember { mutableStateOf(initialDiscountStr) }
    var isFirstInput by remember { mutableStateOf(initialDiscountStr.isNotEmpty()) }
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val parsedPercent = discountText.toDoubleOrNull() ?: 0.0
    val clampedPercent = parsedPercent.coerceIn(0.0, 99.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = targetName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Швидкі кнопки популярних знижок (горизонтальний скрол)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(0.0, 3.0, 5.0, 7.0, 10.0, 15.0, 20.0, 25.0, 50.0)
                    for (preset in presets) {
                        val isInt = preset % 1.0 == 0.0
                        val presetStr = if (isInt) "${preset.toInt()}" else "$preset"
                        val isSelected = discountText == presetStr || (preset == 0.0 && (discountText == "0" || (discountText.isEmpty() && currentDiscountPercent == 0.0)))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isFirstInput = false
                                discountText = if (preset == 0.0) "0" else presetStr
                            },
                            label = { Text("$presetStr%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        )
                    }
                }

                // Поле ручного введення відсотка з цифровою клавіатурою для телефонів
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() || it == '.' }
                        if (isFirstInput && clean.isNotEmpty()) {
                            val lastChar = clean.last()
                            discountText = if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else clean
                            isFirstInput = false
                        } else {
                            discountText = clean
                        }
                    },
                    label = { Text("Відсоток знижки (0..99%)") },
                    placeholder = { Text("наприклад: 7.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onApply(clampedPercent) }),
                    trailingIcon = {
                        if (discountText.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFirstInput = false
                                        discountText = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onApply(clampedPercent)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Інтерактивна картка попереднього перегляду фінальної суми
                if (rawTotalKopecks != null && rawTotalKopecks > 0L) {
                    val discountKopecks = Math.round((rawTotalKopecks * clampedPercent) / 100.0)
                    val finalKopecks = (rawTotalKopecks - discountKopecks).coerceAtLeast(0L)
                    val pctStr = if (clampedPercent % 1.0 == 0.0) "${clampedPercent.toInt()}%" else "$clampedPercent%"
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (clampedPercent > 0.0) {
                                        "Знижка -$pctStr: -${PosCurrencyFormatter.formatKopecks(discountKopecks)} грн"
                                    } else {
                                        "Без знижки"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (clampedPercent > 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "До сплати:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${PosCurrencyFormatter.formatKopecks(finalKopecks)} грн",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (currentDiscountPercent > 0.0) {
                    val curLabel = if (currentDiscountPercent % 1.0 == 0.0) "${currentDiscountPercent.toInt()}" else "$currentDiscountPercent"
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onApply(0.0)
                        },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Скинути діючу знижку ($curLabel%)", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply(clampedPercent)
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Застосувати", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
    )
}
