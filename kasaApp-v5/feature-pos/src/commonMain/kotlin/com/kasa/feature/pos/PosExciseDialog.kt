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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Інтерактивний діалог введення та перевірки акцизної марки для смартфонів (Android та iOS):
 * - Підтримка сканування електронної акцизної марки «Дія» (QR-посилання https://t.gov.ua/...);
 * - Підтримка традиційних паперових серій та номерів (наприклад, АА12345678);
 * - Сенсорні швидкі шаблони для миттєвого тестування та зручні touch-target >= 44 dp;
 * - Тактильний вібровідгук (Haptic Feedback) та адаптація під віртуальну клавіатуру.
 */
@Composable
fun PosExciseDialog(
    productName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenScanner: (() -> Unit)? = null
) {
    var stampText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val clean = stampText.trim()
    val isDiiaExcise = clean.contains("t.gov.ua/")
    val isPaperExcise = clean.length in 8..16 && clean.all { it.isLetterOrDigit() }

    val presets = listOf(
        Triple("https://t.gov.ua/AA12345678", "🍾 Е-марка Дія", "QR-код Дія"),
        Triple("АА12345678", "🍷 Паперова (АА)", "АА12345678"),
        Triple("ТТ98765432", "🚬 Тютюнова (ТТ)", "ТТ98765432")
    )
    val pasteClipboard = {
        val clip = clipboardManager.getText()?.text?.trim()
        if (!clip.isNullOrBlank()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            stampText = if (!clip.startsWith("http")) clip.uppercase() else clip
        }
    }

    fun submit() {
        val finalStamp = if (clean.contains("t.gov.ua/")) clean.substringAfter("t.gov.ua/").take(16).trim() else clean
        if (finalStamp.isNotBlank()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            PosAudioFeedback.playScanBeep()
            onConfirm(finalStamp)
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
                    text = "🍾 Акцизна марка",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
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
                Text(
                    text = "Товар: $productName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Скануйте штрих-код або введіть марку:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = stampText,
                    onValueChange = { input ->
                        stampText = if (!input.startsWith("http")) input.uppercase() else input
                    },
                    singleLine = true,
                    label = { Text("Акцизна марка або QR Дія") },
                    placeholder = { Text("https://t.gov.ua/.. або АА12345678") },
                    supportingText = {
                        when {
                            clean.isBlank() -> Text("Введіть вручну або виберіть зразок нижче")
                            isDiiaExcise -> Text("✓ Електронна акцизна марка Дія", color = MaterialTheme.colorScheme.secondary)
                            isPaperExcise -> Text("✓ Паперова марка (${clean.length} зн.)", color = MaterialTheme.colorScheme.tertiary)
                            else -> Text("Формат марки: QR Дія або 8-16 символів")
                        }
                    },
                    trailingIcon = {
                        if (stampText.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(onClick = { stampText = "" }, modifier = Modifier.size(32.dp)) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { submit() }, modifier = Modifier.size(32.dp)) {
                                    Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 2.dp)) {
                                if (onOpenScanner != null) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onOpenScanner()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) { Text("📷", fontSize = 15.sp) }
                                }
                                IconButton(onClick = pasteClipboard, modifier = Modifier.size(34.dp)) {
                                    Text("📋", fontSize = 13.sp)
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Швидкі зразки марок:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { (sampleCode, sampleLabel, sampleDisplay) ->
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                stampText = sampleCode
                            },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sampleLabel,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sampleDisplay,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = clean.isNotBlank(),
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
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
    )
}
