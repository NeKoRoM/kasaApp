package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ергономічний сегментований перемикач режиму вітрини товарів: [⊞ Сітка] та [≡ Список].
 */
@Composable
fun PosCatalogViewModeSegmentedToggle(
    isCatalogTableView: Boolean,
    onToggleViewMode: (Boolean) -> Unit,
    height: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(height)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = {
                    if (isCatalogTableView) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleViewMode(false)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = if (!isCatalogTableView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.fillMaxHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 42.dp)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "⊞",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isCatalogTableView) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Surface(
                onClick = {
                    if (!isCatalogTableView) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleViewMode(true)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isCatalogTableView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.fillMaxHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 42.dp)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "≡",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCatalogTableView) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
