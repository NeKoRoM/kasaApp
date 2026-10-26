package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Рядок перемикання вигляду вітрини (Таблиця / Сітка), сортування та індикації кількості товарів.
 */
@Composable
fun PosCatalogSortBar(
    isCatalogTableView: Boolean,
    onToggleViewMode: (Boolean) -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit,
    filteredCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompact) {
            val (sortLabel, nextSort) = when (sortMode) {
                "NAME" -> "⇅ А-Я" to "PRICE"
                "PRICE" -> "⇅ Ціна ₴" to "PLU"
                else -> "⇅ № PLU" to "NAME"
            }
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosCatalogViewModeSegmentedToggle(
                    isCatalogTableView = isCatalogTableView,
                    onToggleViewMode = onToggleViewMode,
                    height = 36.dp
                )
                FilterChip(
                    selected = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSortModeChange(nextSort)
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                    label = {
                        Text(
                            text = sortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosCatalogViewModeSegmentedToggle(
                    isCatalogTableView = isCatalogTableView,
                    onToggleViewMode = onToggleViewMode,
                    height = 40.dp
                )
                FilterChip(
                    selected = sortMode == "PLU",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSortModeChange("PLU")
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    label = { Text("№ PLU", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
                FilterChip(
                    selected = sortMode == "NAME",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSortModeChange("NAME")
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    label = { Text("А-Я", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
                FilterChip(
                    selected = sortMode == "PRICE",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSortModeChange("PRICE")
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    label = { Text("Ціна ₴", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        Text(
            text = "$filteredCount/$totalCount",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
