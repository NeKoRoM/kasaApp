package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.ReceiptType
import java.util.Locale

/**
 * Чіпи статусів каси для мобільного топ-бару:
 * РРО, Повернення, Відкладені чеки, Ваги, Клієнт, Касова зміна.
 */
@Composable
fun PosMobileKasaStatusChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)?,
    minHeight: Dp,
    verticalPad: Dp
) {
    val haptic = LocalHapticFeedback.current
    val kasaContainer = if (state.isKasaOnline) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
    val kasaContent = if (state.isKasaOnline) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
    val kasaDot = if (state.isKasaOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = kasaContainer,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (onOpenConnectionDialog != null) onOpenConnectionDialog() else onEvent(PosEvent.ToggleServiceDialog)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).background(color = kasaDot, shape = CircleShape))
            Text(
                text = if (state.isKasaOnline) "РРО Онлайн" else "РРО Офлайн",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = kasaContent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PosMobileReturnModeChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    minHeight: Dp,
    verticalPad: Dp
) {
    if (state.receiptType != ReceiptType.RETURN) return
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onEvent(PosEvent.SetReceiptType(ReceiptType.SALE))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("↩️ Повернення ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PosMobileHeldReceiptsChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    minHeight: Dp,
    verticalPad: Dp
) {
    if (state.heldReceipts.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(PosEvent.ToggleHeldReceiptsDialog)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏸ ${state.heldReceipts.size} відкл.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PosMobileScaleWeightChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    minHeight: Dp,
    verticalPad: Dp
) {
    val weight = state.scaleWeight ?: return
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(PosEvent.TareScale)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚖ ${String.format(Locale.US, "%.3f", weight.weightKilograms)} кг", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (weight.isStable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PosMobileCustomerChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    minHeight: Dp,
    verticalPad: Dp
) {
    val haptic = LocalHapticFeedback.current
    val customer = state.selectedCustomer
    val hasCustomer = customer != null
    val label = if (customer != null) {
        val pctStr = if (customer.discountPercent % 1.0 == 0.0) "${customer.discountPercent.toInt()}%" else "${customer.discountPercent}%"
        val discount = if (customer.discountPercent > 0.0) " -$pctStr" else ""
        val parts = customer.name.trim().split(Regex("\\s+"))
        val shortName = if (parts.size >= 2) {
            "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}."
        } else {
            customer.name.take(12)
        }
        "👤 $shortName$discount"
    } else {
        "👤 +Клієнт"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasCustomer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(PosEvent.PromptLoyaltyDialog(true))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hasCustomer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PosMobileShiftStatusChip(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    minHeight: Dp,
    verticalPad: Dp
) {
    val haptic = LocalHapticFeedback.current
    val isExpired = state.is24HoursShiftExpired
    val isShiftOpen = state.currentShift != null && state.currentShift.isOpen
    val shiftNum = state.currentShift?.shiftNumber ?: 1
    val shiftTitle = when {
        isExpired -> "⚠️ >24 год!"
        isShiftOpen && state.cashDrawerBalanceKopecks > 0L -> "Зм.#$shiftNum • ${PosCurrencyFormatter.formatKopecks(state.cashDrawerBalanceKopecks)}₴"
        isShiftOpen -> "Зм.#$shiftNum"
        else -> "Зміна закрита"
    }
    val (shiftBg, shiftTextColor) = when {
        isExpired -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        isShiftOpen -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = shiftBg,
        modifier = Modifier.defaultMinSize(minHeight = minHeight).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(PosEvent.PromptShiftReportDialog(true))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shiftTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = shiftTextColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
