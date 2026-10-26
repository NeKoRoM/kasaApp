package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Сенсорна цифрова клавіатура касира (Touchscreen Numpad) для швидкого введення
 * кодів PLU, штрих-кодів, кількості та цін без фізичної клавіатури.
 */
@Composable
fun PosTouchNumpadDialog(
    onScanBarcode: (String) -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    onSearchPlu: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var buffer by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔢 Касовий нумпад",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 360.dp
                val isCompactHeight = maxHeight < 480.dp
                val displayHeight = if (isCompactHeight) 38.dp else 48.dp
                val keyHeight = if (isCompactHeight) 38.dp else 44.dp
                val actionHeight = if (isCompactHeight) 38.dp else 44.dp
                val rowSpacing = if (isCompactHeight) 2.dp else 4.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 4.dp else 8.dp)
                ) {
                    // Дисплей набраного значення з адаптивним шрифтом та вставкою
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(displayHeight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (buffer.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            buffer = ""
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val digitsAndDot = clip.filter { it.isDigit() || it == '.' }.take(18)
                                            if (digitsAndDot.isNotBlank()) buffer = digitsAndDot
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("📋", fontSize = 13.sp)
                                }
                            }

                            val displayFontSize = when {
                                buffer.length > 13 -> 14.sp
                                buffer.length > 8 -> 17.sp
                                isCompactHeight -> 18.sp
                                else -> 22.sp
                            }
                            Text(
                                text = if (buffer.isEmpty()) "0" else buffer,
                                fontSize = displayFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (buffer.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Швидкі дії касира
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                if (buffer.isNotBlank()) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onScanBarcode(buffer)
                                    onDismiss()
                                }
                            },
                            enabled = buffer.isNotBlank(),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = actionHeight),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(if (isNarrow) "📷 Код" else "📷 Штрих-код", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                if (buffer.isNotBlank()) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSearchPlu(buffer)
                                    onDismiss()
                                }
                            },
                            enabled = buffer.isNotBlank(),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = actionHeight),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(if (isNarrow) "🔍 PLU" else "🔍 PLU / Код", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                val qty = buffer.toDoubleOrNull()
                                if (qty != null && qty > 0.0) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onUpdateQuantity(qty)
                                    onDismiss()
                                }
                            },
                            enabled = (buffer.toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = actionHeight),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(if (isNarrow) "× К-сть" else "× Кількість", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Цифрова сітка (7 8 9 / 4 5 6 / 1 2 3 / 0 . ⌫ C)
                    val rows = listOf(
                        listOf("7", "8", "9", "⌫"),
                        listOf("4", "5", "6", "C"),
                        listOf("1", "2", "3", "00"),
                        listOf("0", ".", "000", "↵")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
                        for (rowKeys in rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (key in rowKeys) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            when (key) {
                                                "C" -> buffer = ""
                                                "⌫" -> if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
                                                "↵" -> {
                                                    if (buffer.isNotBlank()) {
                                                        onScanBarcode(buffer)
                                                        onDismiss()
                                                    }
                                                }
                                                "." -> {
                                                    if (buffer.isEmpty()) {
                                                        buffer = "0."
                                                    } else if (!buffer.contains('.')) {
                                                        buffer += "."
                                                    }
                                                }
                                                else -> {
                                                    if (buffer.length + key.length <= 18) buffer += key
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(keyHeight),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = if (key == "↵") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = if (isCompactHeight) 14.sp else 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (key) {
                                                "C" -> MaterialTheme.colorScheme.error
                                                "↵" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Закрити", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = null,
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 420.dp)
    )
}
