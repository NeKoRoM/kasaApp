package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
 * Модальний діалог ручного введення готівки для смартфонів:
 * - Цифрова десяткова клавіатура з дією Enter (↵);
 * - Швидкі кнопки додавання номіналів (+50, +100, +200, +500) та точної суми ("Рівно");
 * - Онлайн-розрахунок решти в реальному часі;
 * - Захист від клавіатури через imePadding.
 */
@Composable
fun PosCustomCashInputDialog(
    totalPayableKopecks: Long,
    initialCashKopecks: Long = 0L,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val initialText = if (initialCashKopecks > 0L) {
        if (initialCashKopecks % 100 == 0L) (initialCashKopecks / 100).toString()
        else String.format(Locale.US, "%.2f", initialCashKopecks / 100.0)
    } else ""
    var inputText by remember { mutableStateOf(initialText) }
    var isFirstInput by remember { mutableStateOf(initialText.isNotEmpty()) }
    val parsedUah = inputText.replace(',', '.').toDoubleOrNull()
    val isValid = parsedUah != null && parsedUah > 0.0
    val enteredKopecks = if (isValid) Math.round(parsedUah!! * 100.0) else 0L

    fun submit() {
        if (isValid) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onApply(enteredKopecks)
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
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text("🔢 Введення готівки", fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, softWrap = false)
                    Text(
                        "До сплати: ${PosCurrencyFormatter.formatKopecks(totalPayableKopecks)} грн",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (isFirstInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            inputText = if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                            isFirstInput = false
                        } else if (sanitized.count { it == '.' } <= 1 && sanitized.all { it.isDigit() || it == '.' }) {
                            inputText = sanitized
                        }
                    },
                    label = { Text("Отримана сума (грн)") },
                    placeholder = { Text("Наприклад: 200") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFirstInput = false
                                        inputText = ""
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
                    modifier = Modifier.fillMaxWidth()
                )

                // Живий розрахунок решти прямо у формі введення
                if (isValid) {
                    val diff = enteredKopecks - totalPayableKopecks
                    when {
                        diff > 0L -> Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "Решта покупцю: ${PosCurrencyFormatter.formatKopecks(diff)} грн",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        diff == 0L -> Text("✓ Сума без решти", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        else -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "Не вистачає: ${PosCurrencyFormatter.formatKopecks(-diff)} грн",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val exactUah = totalPayableKopecks / 100.0
                            inputText = if (totalPayableKopecks % 100 == 0L) {
                                (totalPayableKopecks / 100).toString()
                            } else {
                                String.format(Locale.US, "%.2f", exactUah)
                            }
                            isFirstInput = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("Рівно", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    listOf(50, 100, 200, 500, 1000).forEach { addVal ->
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isFirstInput = false
                                val current = inputText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                val next = current + addVal
                                inputText = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Text("+$addVal", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
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
        modifier = Modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 440.dp)
        )
    }
}
