package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Сервісна картка операцій з готівкою в скриньці РРО:
 * - Залишок готівки в скриньці;
 * - Швидкі службові операції: Розмін (+500), Інкасація;
 * - Швидкі номінали (+100, +200, +500, +1000 ₴);
 * - Службове внесення (+) та службова видача (-);
 * - Відкриття грошової скриньки.
 */
@Composable
fun ServiceCashCard(
    cashDrawerBalanceKopecks: Long,
    onPerformCashInOut: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cashAmountText by remember { mutableStateOf("") }
    var isFirstInput by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    ServiceSectionCard(title = "Службові операції з готівкою", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Грошей у скриньці:", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${PosCurrencyFormatter.formatKopecks(cashDrawerBalanceKopecks)} грн",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPerformCashInOut(50000L)
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Розмін (+500)", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = {
                    if (cashDrawerBalanceKopecks > 0L) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPerformCashInOut(-cashDrawerBalanceKopecks)
                    }
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                enabled = cashDrawerBalanceKopecks > 0L
            ) {
                Text("Інкасація", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Швидкі номінали для зручного та швидкого службового внесення
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(100L, 200L, 500L, 1000L).forEach { denom ->
                val isSelected = cashAmountText == "$denom.00" || cashAmountText == "$denom"
                if (isSelected) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cashAmountText = "$denom.00"
                            isFirstInput = true
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("+$denom ₴", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cashAmountText = "$denom.00"
                            isFirstInput = true
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("+$denom ₴", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        val parsedAmount = cashAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val isCashValid = parsedAmount > 0.0
        val amountKopecks = Math.round(parsedAmount * 100.0)

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = cashAmountText,
            onValueChange = { input ->
                val s = input.replace(',', '.')
                if (isFirstInput && s.isNotEmpty()) {
                    val lastChar = s.last()
                    cashAmountText = if (lastChar.isDigit()) lastChar.toString() else if (lastChar == '.') "0." else s
                    isFirstInput = false
                } else if (s.count { it == '.' } <= 1 && s.all { it.isDigit() || it == '.' }) {
                    cashAmountText = s
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Сума в грн (наприклад: 500.00)") },
            trailingIcon = {
                if (cashAmountText.isNotEmpty()) {
                    IconButton(onClick = {
                        isFirstInput = false
                        cashAmountText = ""
                    }, modifier = Modifier.size(40.dp)) {
                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (amountKopecks > 0L) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPerformCashInOut(amountKopecks)
                        cashAmountText = ""
                    }
                },
                enabled = isCashValid,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Внесення (+)", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = {
                    if (amountKopecks > 0L) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPerformCashInOut(-amountKopecks)
                        cashAmountText = ""
                    }
                },
                enabled = isCashValid && amountKopecks <= cashDrawerBalanceKopecks,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Видача (-)", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 440.dp
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenDrawer()
                },
                contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
            ) {
                Text(if (isNarrow) "Відкрити скриньку" else "Відкрити грошову скриньку", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
