package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Діалогове вікно синхронізації та керування енергонезалежною таблицею артикулів (PLU) каси:
 * - Повне вивантаження товарів із фізичної пам'яті РРО у вітрину додатка (Dump)
 * - Пакетне програмування поточного каталогу в пам'ять каси (Upload)
 * - Зручні повнорозмірні кнопки для смартфонів та індикація прогресу
 */
@Composable
fun PosPluDialog(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier) {
        val isNarrow = maxWidth < 440.dp

        AlertDialog(
            onDismissRequest = {
                if (!state.isPluSyncing) onEvent(PosEvent.TogglePluDialog)
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Пам'ять РРО (PLU)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                IconButton(
                    onClick = { if (!state.isPluSyncing) onEvent(PosEvent.TogglePluDialog) },
                    enabled = !state.isPluSyncing
                ) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Інформаційна картка
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Товарів у вітрині POS: ${state.catalogProducts.size} шт.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Дозволяє напряму синхронізувати внутрішню таблицю артикулів апарата Datecs / Exellio.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!state.isKasaOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Text(
                                text = "РРО офлайн: підключіть апарат для обміну таблицею артикулів",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Дії синхронізації (повнорозмірні кнопки для сенсорного натискання)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(PosEvent.StartPluDumpFromKasa)
                        },
                        enabled = !state.isPluSyncing && state.isKasaOnline,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(if (isNarrow) "⬇ Зчитати (Dump)" else "⬇ Зчитати з РРО (Dump)", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(PosEvent.StartPluUploadToKasa)
                        },
                        enabled = !state.isPluSyncing && state.isKasaOnline && state.catalogProducts.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(if (isNarrow) "⬆ Записати (Upload)" else "⬆ Записати в РРО (Upload)", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Прогрес синхронізації
                if (state.isPluSyncing || state.pluSyncProgress != null) {
                    val progress = state.pluSyncProgress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.isPluSyncing) "Синхронізація..." else "Завершено",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${progress?.current ?: 0} / ${progress?.total ?: 0}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress?.percent ?: 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!progress?.currentItemName.isNullOrBlank()) {
                                Text(
                                    text = "Товар: ${progress?.currentItemName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Повідомлення про результат операції
                if (!state.pluSyncStatusMessage.isNullOrBlank()) {
                    Text(
                        text = state.pluSyncStatusMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onEvent(PosEvent.TogglePluDialog) },
                enabled = !state.isPluSyncing,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
            ) {
                Text("Закрити", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
        )
    }
}
