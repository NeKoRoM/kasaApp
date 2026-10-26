package com.kasa.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.FiscalUrlGenerator
import com.kasa.model.Receipt
import com.kasa.model.ReceiptTextRenderer

/**
 * Модальне вікно попереднього перегляду та шерингу електронного фіскального чека (Android / iOS):
 * - Відтворює графічний вигляд термодруку касової стрічки;
 * - Підтримка швидкого копіювання тексту чека або посилання ДПС в 1 дотик для Viber/Telegram/SMS;
 * - Тактильний вібровідгук (Haptic Feedback) та звукове підтвердження дій.
 */
@Composable
fun PosReceiptPreviewDialog(
    receipt: Receipt,
    headerLines: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copyNotice by remember { mutableStateOf<String?>(null) }

    val renderedText = remember(receipt, headerLines) {
        ReceiptTextRenderer.render(
            receipt = receipt,
            headerLines = headerLines.ifEmpty {
                listOf("ТОРГОВИЙ ЦЕНТР", "м. Київ, вул. Хрещатик", "ФН 12345678")
            },
            tapeWidth = 32
        )
    }

    val taxUrl = remember(receipt) {
        FiscalUrlGenerator.generateTaxVerificationUrl(receipt)
    }

    BoxWithConstraints(modifier = modifier) {
        val isCompactHeight = maxHeight < 500.dp
        val isNarrow = maxWidth < 440.dp
        val maxReceiptHeight = if (isCompactHeight) 150.dp else 320.dp

        AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "🧾 Електронний чек",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "(${receipt.items.size})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (copyNotice != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = copyNotice!!,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxReceiptHeight),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = renderedText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Мобільна панель шерингу чека
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onShareReceipt != null) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                PosAudioFeedback.playScanBeep()
                                onShareReceipt(renderedText)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(if (isNarrow) "📤 Чек" else "📤 Поділитись", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            PosAudioFeedback.playScanBeep()
                            clipboardManager.setText(AnnotatedString(renderedText))
                            copyNotice = "✓ Текст чека скопійовано"
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(if (isNarrow) "📋 Текст" else "📋 Копіювати", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    if (taxUrl != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                PosAudioFeedback.playScanBeep()
                                if (onOpenTaxUrl != null) {
                                    onOpenTaxUrl(taxUrl)
                                } else {
                                    clipboardManager.setText(AnnotatedString(taxUrl))
                                    copyNotice = "✓ Посилання ДПС скопійовано"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("🌐 ДПС", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (taxUrl != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = {
                            if (onOpenTaxUrl != null) onOpenTaxUrl(taxUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Онлайн-перевірка в ДПС:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Відкрити ↗", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = taxUrl,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Закрити", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = null,
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
