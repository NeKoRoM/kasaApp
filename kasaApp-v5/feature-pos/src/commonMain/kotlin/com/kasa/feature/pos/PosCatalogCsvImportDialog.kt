package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Сенсорний діалог імпорту номенклатури товарів у форматі CSV:
 * - Підтримка прямої вставки з буфера обміну;
 * - Кнопка підстановки зразка заголовка колонок;
 * - Підрахунок виявлених рядків товарів.
 */
@Composable
fun PosCatalogCsvImportDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var csvImportText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val lineCount = remember(csvImportText) {
        csvImportText.lineSequence().filter { it.isNotBlank() }.count()
    }

    val sampleHeader = "PLU,Назва,Ціна,Собівартість,Штрихкод,Категорія,Одиниця\n"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📥 Імпорт з CSV",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val editorHeight = if (maxHeight < 500.dp) 110.dp else 160.dp

                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Вставте текст CSV:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    csvImportText = if (csvImportText.isBlank()) sampleHeader else "$sampleHeader$csvImportText"
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+ Зразок", fontSize = 11.sp, maxLines = 1)
                            }
                            TextButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        csvImportText = clipText
                                    }
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("📋 Вставити", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    if (lineCount > 1) {
                        Text(
                            text = "✓ Виявлено рядків: ${lineCount - 1} (без заголовка)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedTextField(
                        value = csvImportText,
                        onValueChange = { csvImportText = it },
                        modifier = Modifier.fillMaxWidth().height(editorHeight),
                        placeholder = { Text("PLU,Назва,Ціна,Собівартість,Штрихкод...") },
                        trailingIcon = {
                            if (csvImportText.isNotBlank()) {
                                IconButton(
                                    onClick = { csvImportText = "" },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (csvImportText.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onImport(csvImportText)
                    }
                    onDismiss()
                },
                enabled = csvImportText.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Завантажити", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 520.dp)
    )
}
