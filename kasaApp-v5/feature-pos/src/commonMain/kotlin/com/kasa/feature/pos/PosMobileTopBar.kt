package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Компактний мобільний статус-бар для смартфонів (Android та iOS):
 * - Індикація підключення фізичного РРО Datecs (Онлайн / Офлайн) в 1 дотик;
 * - Статус касової зміни та попередження про ліміт 24 годин;
 * - Швидкий індикатор прив'язаного клієнта програми лояльності;
 * - Мобільне меню швидких касових звітів і службових операцій.
 */
@Composable
fun PosMobileTopBar(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    isLandscapeCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isMenuOpen by remember { mutableStateOf(false) }
    val chipMinHeight = if (isLandscapeCompact) 34.dp else 40.dp
    val chipVerticalPad = if (isLandscapeCompact) 3.dp else 6.dp
    val menuBtnSize = if (isLandscapeCompact) 36.dp else 44.dp
    val rowVerticalPad = if (isLandscapeCompact) 1.dp else 2.dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = rowVerticalPad, bottom = rowVerticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Горизонтальний скрол статусів на вузьких екранах: РРО, повернення, відкладені, ваги, клієнт, зміна
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PosMobileKasaStatusChip(state, onEvent, onOpenConnectionDialog, chipMinHeight, chipVerticalPad)
                PosMobileReturnModeChip(state, onEvent, chipMinHeight, chipVerticalPad)
                PosMobileHeldReceiptsChip(state, onEvent, chipMinHeight, chipVerticalPad)
                PosMobileScaleWeightChip(state, onEvent, chipMinHeight, chipVerticalPad)
                PosMobileCustomerChip(state, onEvent, chipMinHeight, chipVerticalPad)
                PosMobileShiftStatusChip(state, onEvent, chipMinHeight, chipVerticalPad)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Фіксована кнопка меню дій касира
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isMenuOpen = true
                },
                modifier = Modifier.size(menuBtnSize)
            ) {
                Text("⋮", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isMenuOpen) {
        PosMobileActionsMenuDialog(
            onDismiss = { isMenuOpen = false },
            onEvent = onEvent,
            onOpenConnectionDialog = onOpenConnectionDialog
        )
    }
}
