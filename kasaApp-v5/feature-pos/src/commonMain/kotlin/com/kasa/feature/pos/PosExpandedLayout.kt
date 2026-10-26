package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Широкоекранний двопанельний макет POS-терміналу для планшетів (10"+) та комп'ютерів (>= 720 dp).
 * Одночасно розміщує чекову стрічку ліворуч та вітрину товарів праворуч із нижнім рядком гарячих клавіш.
 */
@Composable
fun PosExpandedLayout(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    tapeWeight: Float = 0.42f,
    catalogWeight: Float = 0.58f,
    showShortcutsBar: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ЛІВА ПАНЕЛЬ: Чекова стрічка, підсумки, знижки та блок оплати
            PosTapePanel(
                state = state,
                onEvent = onEvent,
                onOpenConnectionDialog = onOpenConnectionDialog,
                onOpenTaxUrl = onOpenTaxUrl,
                onShareReceipt = onShareReceipt,
                modifier = Modifier
                    .weight(tapeWeight)
                    .fillMaxHeight()
                    .padding(8.dp)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            // ПРАВА ПАНЕЛЬ: Пошук, ваги та вітрина товарів
            PosCatalogPanel(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .weight(catalogWeight)
                    .fillMaxHeight()
                    .padding(8.dp),
                isMobile = false
            )
        }

        if (showShortcutsBar) {
            PosExpandedShortcutsBar(
                activeCartIndex = state.activeCartIndex,
                itemsCount = state.items.size
            )
        }
    }
}

/**
 * Інформаційна смуга гарячих клавіш для швидкої роботи касира на десктопі та терміналах з фізичною клавіатурою.
 */
@Composable
private fun PosExpandedShortcutsBar(
    activeCartIndex: Int,
    itemsCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosShortcutBadge(key = "F2", action = "Сканер")
                PosShortcutBadge(key = "F3", action = "+Товар")
                PosShortcutBadge(key = "F4", action = "Нумпад")
                PosShortcutBadge(key = "F7", action = "Скринька")
                PosShortcutBadge(key = "F8", action = "Покупець")
                PosShortcutBadge(key = "F9", action = "Оплата")
                PosShortcutBadge(key = "Esc", action = "Закрити")
            }

            Text(
                text = "Чек #${activeCartIndex + 1} • Позицій: $itemsCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PosShortcutBadge(
    key: String,
    action: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = key,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        Text(
            text = action,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
