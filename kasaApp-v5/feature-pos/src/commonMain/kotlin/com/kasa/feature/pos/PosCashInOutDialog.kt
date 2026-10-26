package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
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
 * Режим службової операції з готівкою в касовій скриньці.
 */
enum class CashInOutMode {
    CASH_IN,
    CASH_OUT
}

/**
 * Ергономічний діалог службового внесення або видачі готівки для смартфонів:
 * - Швидкі номінали НБУ та кнопка "Вся скринька" для швидкої інкасації;
 * - Валідація ліміту залишку скриньки при видачі;
 * - Пряме виконання фіскальної операції внесення/видачі.
 */
@Composable
fun PosCashInOutDialog(
    mode: CashInOutMode,
    currentBalanceKopecks: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var amountText by remember { mutableStateOf("") }
    var isFirstInput by remember { mutableStateOf(false) }

    val parsedUah = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val amountKopecks = Math.round(parsedUah * 100.0)
    val isAmountPositive = amountKopecks > 0L
    val isOverdrawn = mode == CashInOutMode.CASH_OUT && amountKopecks > currentBalanceKopecks
    val isValid = isAmountPositive && !isOverdrawn

    fun submit() {
        if (!isValid) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val finalKopecks = if (mode == CashInOutMode.CASH_IN) amountKopecks else -amountKopecks
        onConfirm(finalKopecks)
        onDismiss()
    }

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
                        text = if (mode == CashInOutMode.CASH_IN) "💵 Службове внесення" else "📤 Службова видача",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "У скриньці: ${PosCurrencyFormatter.formatKopecks(currentBalanceKopecks)} грн",
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
                // Швидкі кнопки номіналів
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == CashInOutMode.CASH_IN) {
                        listOf(100L, 200L, 500L, 1000L).forEach { denom ->
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountText = "$denom.00"
                                    isFirstInput = true
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+$denom₴", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    } else {
                        listOf(100L, 200L, 500L).forEach { denom ->
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountText = "$denom.00"
                                    isFirstInput = true
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$denom₴", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }
                        if (currentBalanceKopecks > 0L) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountText = String.format(Locale.US, "%.2f", currentBalanceKopecks / 100.0)
                                    isFirstInput = true
                                },
                                modifier = Modifier.weight(1.2f).defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Вся каса", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (isFirstInput && sanitized.isNotEmpty()) {
                            val lastChar = sanitized.last()
                            amountText = if (lastChar.isDigit() || lastChar == '.') lastChar.toString() else sanitized
                            isFirstInput = false
                        } else if (sanitized.count { it == '.' } <= 1 && sanitized.all { it.isDigit() || it == '.' }) {
                            amountText = sanitized
                        }
                    },
                    label = { Text("Сума операції (грн)") },
                    placeholder = { Text("Наприклад: 500.00") },
                    singleLine = true,
                    isError = isOverdrawn,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        if (amountText.isNotEmpty()) {
                            IconButton(onClick = {
                                isFirstInput = false
                                amountText = ""
                            }, modifier = Modifier.size(40.dp)) {
                                Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOverdrawn) {
                    Text(
                        text = "⚠️ Сума перевищує залишок у скриньці!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == CashInOutMode.CASH_IN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (mode == CashInOutMode.CASH_IN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                val actionTitle = if (mode == CashInOutMode.CASH_IN) "Внести" else "Видати"
                val sumStr = if (amountKopecks > 0L) " ${PosCurrencyFormatter.formatKopecks(amountKopecks)} грн" else ""
                Text("$actionTitle$sumStr", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
    )
}
