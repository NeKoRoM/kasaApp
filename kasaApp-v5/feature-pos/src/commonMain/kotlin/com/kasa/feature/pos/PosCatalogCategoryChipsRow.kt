package com.kasa.feature.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Горизонтальний рядок категорій товарів для сенсорного вибору та скролу пальцем на телефонах.
 * Забезпечує сенсорну область >= 36 dp та унеможливлює розрив рядків тексту.
 */
@Composable
fun PosCatalogCategoryChipsRow(
    selectedCategory: String,
    availableCategories: List<String>,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val chipHeight = if (isCompactHeight) 34.dp else 40.dp
    val chipFontSize = if (isCompactHeight) 10.sp else 11.sp

    val primaryCats = listOf(
        "ALL" to "Всі",
        "FAVORITES" to "⭐ Швидкі"
    )

    val secondaryCats = listOf(
        "WEIGHTED" to "⚖ Вагові",
        "EXCISE" to "🍾 Підакцизні",
        "IN_STOCK" to "✅ В наявності",
        "LOW_STOCK" to "⚠️ Закінчуються",
        "OUT_OF_STOCK" to "❌ Немає",
        "PIECE" to "Штучні"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(primaryCats) { (catKey, catLabel) ->
            FilterChip(
                selected = selectedCategory == catKey,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCategory(catKey)
                },
                modifier = Modifier.defaultMinSize(minHeight = chipHeight),
                label = { Text(catLabel, fontSize = chipFontSize, maxLines = 1, softWrap = false) }
            )
        }
        items(availableCategories) { customCat ->
            FilterChip(
                selected = selectedCategory == customCat,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCategory(customCat)
                },
                modifier = Modifier.defaultMinSize(minHeight = chipHeight),
                label = {
                    Text(
                        text = customCat,
                        fontSize = chipFontSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
        items(secondaryCats) { (catKey, catLabel) ->
            FilterChip(
                selected = selectedCategory == catKey,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCategory(catKey)
                },
                modifier = Modifier.defaultMinSize(minHeight = chipHeight),
                label = { Text(catLabel, fontSize = chipFontSize, maxLines = 1, softWrap = false) }
            )
        }
    }
}
