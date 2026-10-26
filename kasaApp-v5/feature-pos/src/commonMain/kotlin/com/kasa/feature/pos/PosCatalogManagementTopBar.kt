package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Верхня панель управління складом та номенклатурою (Top App Bar & Action Buttons).
 * Надає миттєвий доступ до додавання товару, масової переоцінки та CSV імпорту/експорту.
 */
@Composable
fun PosCatalogManagementTopBar(
    skuCount: Int,
    totalRetail: Double,
    totalCost: Double,
    onDismiss: () -> Unit,
    onAddProduct: () -> Unit,
    onReprice: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                    Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("📦 Складська база", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, softWrap = false)
                    Text(
                        text = "$skuCount SKU • %.2f₴ • Прибуток: %.2f₴".format(totalRetail, (totalRetail - totalCost).coerceAtLeast(0.0)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddProduct()
                },
                modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("+ Товар", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onReprice()
                },
                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("🏷 Переоцінка", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExportCsv()
                },
                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("📤 CSV Експорт", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onImportCsv()
                },
                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("📥 CSV Імпорт", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}
