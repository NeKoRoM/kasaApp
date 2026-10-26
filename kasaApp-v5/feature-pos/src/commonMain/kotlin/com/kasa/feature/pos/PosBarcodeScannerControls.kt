package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kasa.model.Product

/**
 * Панель швидких перемикачів сканера: Пакетний режим, Ліхтарик, Звуковий сигнал.
 */
@Composable
fun PosBarcodeScannerControls(
    isBatchMode: Boolean,
    onToggleBatchMode: () -> Unit,
    isFlashlightOn: Boolean,
    onToggleFlashlight: () -> Unit,
    isSoundOn: Boolean,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = isBatchMode,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleBatchMode()
            },
            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
            label = {
                Text(
                    text = if (isBatchMode) "🔁 Пакетне (ON)" else "🔁 Пакетне",
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        )
        FilterChip(
            selected = isFlashlightOn,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleFlashlight()
            },
            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
            label = {
                Text(
                    text = if (isFlashlightOn) "🔦 Ліхтар ON" else "🔦 Ліхтар",
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        )
        FilterChip(
            selected = isSoundOn,
            onClick = onToggleSound,
            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
            label = {
                Text(
                    text = if (isSoundOn) "🔔 Звук" else "🔕 Без звуку",
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        )
    }
}

/**
 * Банер підтвердження останнього відсканованого товару в пакетному режимі.
 */
@Composable
fun PosBarcodeLastScannedBanner(
    batchScannedCount: Int,
    matchedProduct: Product?,
    lastScannedCode: String?,
    modifier: Modifier = Modifier
) {
    if (batchScannedCount <= 0) return

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                val itemTitle = matchedProduct?.printableName ?: "Код: ${lastScannedCode?.take(16)}"
                Text(
                    text = "✓ Додано: $itemTitle",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                val subText = if (matchedProduct != null) {
                    val priceKopecks = Math.round(matchedProduct.price * 100)
                    "${PosCurrencyFormatter.formatKopecks(priceKopecks)} грн • ${matchedProduct.unitOfMeasurement}"
                } else {
                    "Позиція успішно додана в чек"
                }
                Text(
                    text = subText,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Text("+$batchScannedCount", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
