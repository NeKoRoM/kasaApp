package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.protocol.scale.ScaleWeight

/**
 * Десктопна панель керування каталогом: індикатор ваг, повний набір касових інструментів та рядок пошуку.
 */
@Composable
fun PosDesktopCatalogHeader(
    scaleWeight: ScaleWeight?,
    onEvent: (PosEvent) -> Unit,
    searchQuery: String,
    focusManager: FocusManager,
    btnMinHeight: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            scaleWeight?.let { weight ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⚖ ${String.format(java.util.Locale.US, "%.3f", weight.weightKilograms)} кг",
                        fontWeight = FontWeight.Bold,
                        color = if (weight.isStable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedButton(
                        onClick = { onEvent(PosEvent.TareScale) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("Тара", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { onEvent(PosEvent.PromptProductEdit(null, null)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)
                ) { Text("+ Товар", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleScannerDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("📷 Сканер", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleNumpadDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("🔢 Нумпад", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleCatalogManagementDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("☰ Склад", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleBarcodeSheetDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("🖨 Штрих-коди", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.PromptLoyaltyDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("👤 Клієнт", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.PrintXReport) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("X-звіт", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.PromptShiftReportDialog(true)) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("Z-звіт", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleServiceDialog) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("⚙ Сервіс", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.ToggleJournalDialog) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("📋 Журнал", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { onEvent(PosEvent.TogglePluDialog) }, modifier = Modifier.defaultMinSize(minHeight = btnMinHeight)) { Text("📦 PLU", fontSize = 12.sp, maxLines = 1, softWrap = false) }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onEvent(PosEvent.UpdateSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Пошук за назвою, штрих-кодом, PLU або '3*101'...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    if (searchQuery.isNotBlank()) {
                        onEvent(PosEvent.ScanBarcode(searchQuery))
                        onEvent(PosEvent.UpdateSearchQuery(""))
                    }
                }
            ),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEvent(PosEvent.UpdateSearchQuery("")) }, modifier = Modifier.size(40.dp)) {
                            Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            onEvent(PosEvent.ScanBarcode(searchQuery))
                            onEvent(PosEvent.UpdateSearchQuery(""))
                        }, modifier = Modifier.size(40.dp)) { Text("↵", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                    }
                } else {
                    IconButton(onClick = { onEvent(PosEvent.ToggleScannerDialog(true)) }, modifier = Modifier.size(40.dp)) {
                        Text("📷", fontSize = 16.sp)
                    }
                }
            }
        )
    }
}
