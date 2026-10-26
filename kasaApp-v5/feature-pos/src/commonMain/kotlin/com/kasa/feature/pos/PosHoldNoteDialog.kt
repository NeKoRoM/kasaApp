package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Модальне вікно відкладання чека з приміткою, оптимізоване для мобільних пристроїв:
 * - Швидкі сенсорні шаблони призначень (столи, доставка, самовивіз, телефон);
 * - Зручні зони дотику (minHeight 44 dp) для роботи великим пальцем на смартфонах;
 * - Тактильний вібровідгук (Haptic Feedback) та адаптація під віртуальну клавіатуру.
 */
@Composable
fun PosHoldNoteDialog(
    onConfirm: (note: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val quickNotePresets = listOf(
        "Стіл 1",
        "Стіл 2",
        "Стіл 3",
        "Самовивіз",
        "Доставка",
        "Телефон",
        "Бронь"
    )

    fun submit() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onConfirm(noteText.trim())
    }

    BoxWithConstraints(modifier = modifier) {
        val isNarrow = maxWidth < 440.dp
        val noteLabel = "Примітка"
        val notePlaceholder = "Стіл №2, Олена, Доставка..."

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏸ Відкласти чек",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = "Поточний кошик буде надійно збережено у список відкладених чеків, а касова стрічка очищена для наступного покупця.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(noteLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text(notePlaceholder) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (noteText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            noteText = ""
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(
                                    onClick = { submit() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("↵", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Швидкі примітки в 1 дотик:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickNotePresets) { preset ->
                            FilterChip(
                                selected = noteText == preset,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    noteText = preset
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                label = { Text(preset, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submit() },
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Відкласти ↵", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = if (isNarrow) 8.dp else 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Скасувати", maxLines = 1, softWrap = false)
                }
            },
            modifier = Modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
