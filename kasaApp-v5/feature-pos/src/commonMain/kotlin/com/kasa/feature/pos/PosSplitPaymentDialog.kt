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
import java.util.Locale

/**
 * Модальне вікно налаштування комбінованої/роздільної оплати чека (Готівка + Картка).
 * Підтримує інтерактивний перерахунок залишку, швидкі пресети та швидке підтвердження ↵.
 */
@Composable
fun PosSplitPaymentDialog(
    subtotalKopecks: Long,
    initialCashKopecks: Long? = null,
    initialCardKopecks: Long? = null,
    onApply: (cashKopecks: Long, cardKopecks: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initCash = initialCashKopecks ?: (subtotalKopecks / 2L)
    val initCard = initialCardKopecks ?: (subtotalKopecks - initCash)
    var cashInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initCash / 100.0)) }
    var cardInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initCard / 100.0)) }
    var isFirstCashInput by remember { mutableStateOf(true) }
    var isFirstCardInput by remember { mutableStateOf(true) }

    val cashParsed = cashInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val cardParsed = cardInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val cashKopecks = Math.round(cashParsed * 100.0).coerceAtLeast(0L)
    val cardKopecks = Math.round(cardParsed * 100.0).coerceAtLeast(0L)
    val sumKopecks = cashKopecks + cardKopecks
    val isSumValid = sumKopecks == subtotalKopecks && (cashKopecks > 0L || cardKopecks > 0L)
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    fun submit() {
        if (isSumValid) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onApply(cashKopecks, cardKopecks)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚖️ Змішана оплата",
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
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "До оплати за чеком:",
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${PosCurrencyFormatter.formatKopecks(subtotalKopecks)} грн",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { newVal ->
                        val sanitized = newVal.replace(',', '.')
                        val effective = if (isFirstCashInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            isFirstCashInput = false
                            if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                        } else {
                            sanitized
                        }
                        if (effective.count { it == '.' } <= 1 && effective.all { it.isDigit() || it == '.' }) {
                            cashInput = effective
                            val parsed = effective.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                val parsedKop = Math.round(parsed * 100.0)
                                val remKop = (subtotalKopecks - parsedKop).coerceAtLeast(0L)
                                cardInput = String.format(Locale.US, "%.2f", remKop / 100.0)
                            }
                        }
                    },
                    label = { Text("Готівка (грн)") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (cashInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        cashInput = ""
                                        isFirstCashInput = false
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isSumValid) {
                                IconButton(onClick = { submit() }, modifier = Modifier.size(40.dp)) {
                                    Text("↵", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardInput,
                    onValueChange = { newVal ->
                        val sanitized = newVal.replace(',', '.')
                        val effective = if (isFirstCardInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            isFirstCardInput = false
                            if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                        } else {
                            sanitized
                        }
                        if (effective.count { it == '.' } <= 1 && effective.all { it.isDigit() || it == '.' }) {
                            cardInput = effective
                            val parsed = effective.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                val parsedKop = Math.round(parsed * 100.0)
                                val remKop = (subtotalKopecks - parsedKop).coerceAtLeast(0L)
                                cashInput = String.format(Locale.US, "%.2f", remKop / 100.0)
                            }
                        }
                    },
                    label = { Text("Картка (грн)") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (cardInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        cardInput = ""
                                        isFirstCardInput = false
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isSumValid) {
                                IconButton(onClick = { submit() }, modifier = Modifier.size(40.dp)) {
                                    Text("↵", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                PosSplitPresetsRow(
                    subtotalKopecks = subtotalKopecks,
                    onSelectPreset = { cashKop, cardKop ->
                        isFirstCashInput = false; isFirstCardInput = false
                        cashInput = String.format(Locale.US, "%.2f", cashKop / 100.0)
                        cardInput = String.format(Locale.US, "%.2f", cardKop / 100.0)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                val diff = sumKopecks - subtotalKopecks
                val statusText = when {
                    diff == 0L -> "✓ Сума точно розподілена"
                    diff > 0L -> "⚠️ Надлишок: +${PosCurrencyFormatter.formatKopecks(diff)} грн"
                    else -> "⚠️ Не вистачає: ${PosCurrencyFormatter.formatKopecks(Math.abs(diff))} грн"
                }
                val statusColor = if (diff == 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isSumValid,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) { Text("Застосувати ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) { Text("Скасувати", maxLines = 1, softWrap = false) }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
    )
}
