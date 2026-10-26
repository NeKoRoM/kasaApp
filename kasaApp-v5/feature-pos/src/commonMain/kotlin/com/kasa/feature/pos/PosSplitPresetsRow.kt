package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Рядок швидких пресетів розподілу суми чека між готівкою та карткою:
 * - 50/50 (порівну);
 * - Номінали готівки (50₴, 100₴, 200₴, 500₴, 1000₴);
 * - 100% Картка;
 * - 100% Готівка.
 */
@Composable
fun PosSplitPresetsRow(
    subtotalKopecks: Long,
    onSelectPreset: (cashKopecks: Long, cardKopecks: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Швидкі суми готівки:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(4.dp))
        val standardPresets = remember { listOf(50L, 100L, 200L, 500L, 1000L) }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                PosSplitPresetButton(text = "50/50") {
                    val halfCash = subtotalKopecks / 2L
                    val halfCard = subtotalKopecks - halfCash
                    onSelectPreset(halfCash, halfCard)
                }
            }

            items(standardPresets) { uah ->
                val kop = uah * 100L
                if (subtotalKopecks > kop) {
                    PosSplitPresetButton(text = "${uah}₴") {
                        val remKop = (subtotalKopecks - kop).coerceAtLeast(0L)
                        onSelectPreset(kop, remKop)
                    }
                }
            }

            item {
                PosSplitPresetButton(text = "100% Картка") {
                    onSelectPreset(0L, subtotalKopecks)
                }
            }

            item {
                PosSplitPresetButton(text = "100% Готівка") {
                    onSelectPreset(subtotalKopecks, 0L)
                }
            }
        }
    }
}

@Composable
fun PosSplitPresetButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
