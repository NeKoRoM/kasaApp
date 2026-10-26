package com.kasa.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kasa.feature.sync.SyncWorkerState

/**
 * Верхня системна панель терміналу Desktop POS.
 * Відображає статуси РРО, офлайн-черги, фонового воркера синхронізації та системні дії.
 */
@Composable
fun DesktopTopBar(
    isKasaOnline: Boolean,
    pendingOrdersCount: Int,
    syncWorkerState: SyncWorkerState,
    isFiscalProcessing: Boolean,
    canPay: Boolean,
    heldReceiptsCount: Int = 0,
    kasaStatus: com.kasa.protocol.status.KasaStatus? = null,
    currentShift: com.kasa.model.CashierShift? = null,
    onToggleShiftReportDialog: () -> Unit = {},
    onVoidReceipt: () -> Unit = {},
    onOpenConnectionDialog: () -> Unit,
    onManualSync: () -> Unit,
    onPrintXReport: () -> Unit,
    onPrintZReport: () -> Unit,
    onToggleServiceDialog: () -> Unit,
    onToggleJournalDialog: () -> Unit,
    onToggleHeldReceiptsDialog: () -> Unit = {},
    onRefreshCatalog: () -> Unit,
    onToggleCatalogManagement: () -> Unit = {},
    isCustomerDisplayOpen: Boolean = false,
    onToggleCustomerDisplay: () -> Unit = {},
    onFiscalizeReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ліва частина: Логотип і статусні бейджі
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "KasaApp POS v5",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Статус фіскального зв'язку з РРО
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isKasaOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.clickable { onOpenConnectionDialog() }
                ) {
                    Text(
                        text = if (isKasaOnline) "РРО ONLINE ▾" else "РРО OFFLINE ▾",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Статус черги невідправлених офлайн-чеків
                if (pendingOrdersCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFFE65100)
                    ) {
                        Text(
                            text = "Офлайн-черга: $pendingOrdersCount",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Статус відкладених чеків
                if (heldReceiptsCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.clickable { onToggleHeldReceiptsDialog() }
                    ) {
                        Text(
                            text = "Відкладені: $heldReceiptsCount ▾",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Статус касової зміни
                currentShift?.let { shift ->
                    Spacer(modifier = Modifier.width(8.dp))
                    val shiftColor = if (shift.isExpired24h) Color(0xFFC62828) else Color(0xFF1565C0)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = shiftColor,
                        modifier = Modifier.clickable { onToggleShiftReportDialog() }
                    ) {
                        Text(
                            text = if (shift.isExpired24h) "⚠️ ЗМІНА > 24 ГОД! ▾" else "Зміна #${shift.shiftNumber} (${shift.formattedDuration}) ▾",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Статус фонової синхронізації
                if (syncWorkerState.isFlushing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF00796B)
                    ) {
                        Text(
                            text = "🔄 Синхронізація...",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Апаратні попередження РРО
                if (kasaStatus?.isReceiptPaperOut == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFFC62828)) {
                        Text("⚠️ НЕМАЄ ПАПЕРУ", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (kasaStatus?.isCoverOpen == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFFE65100)) {
                        Text("⚠️ КРИШКА ВІДКРИТА", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (kasaStatus?.is24HoursShiftExpired == true && currentShift?.isExpired24h != true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFFC62828)) {
                        Text("⚠️ ЗМІНА > 24 ГОД", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (kasaStatus?.isFiscalReceiptOpen == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF880E4F),
                        modifier = Modifier.clickable { onVoidReceipt() }
                    ) {
                        Text("⚠️ ЧЕК В РРО (АНУЛЮВАТИ ✕)", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Права частина: Кнопки управління
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingOrdersCount > 0) {
                    OutlinedButton(
                        onClick = onManualSync,
                        enabled = !syncWorkerState.isFlushing
                    ) {
                        Text("Синхронізація ($pendingOrdersCount)")
                    }
                }

                OutlinedButton(
                    onClick = onPrintXReport,
                    enabled = !isFiscalProcessing
                ) {
                    Text("X-Звіт")
                }

                OutlinedButton(
                    onClick = onPrintZReport,
                    enabled = !isFiscalProcessing
                ) {
                    Text("Z-Звіт")
                }

                OutlinedButton(
                    onClick = onToggleServiceDialog
                ) {
                    Text("⚙ Сервіс")
                }

                OutlinedButton(
                    onClick = onToggleJournalDialog
                ) {
                    Text("📋 Журнал")
                }

                OutlinedButton(onClick = onToggleCatalogManagement) { Text("📦 Товари") }
                OutlinedButton(onClick = onRefreshCatalog, enabled = !syncWorkerState.isFlushing) { Text("Каталог ⟳") }
                FilledTonalButton(onClick = onToggleCustomerDisplay) {
                    Text(if (isCustomerDisplayOpen) "🖥 Покупець ●" else "🖥 Покупець")
                }

                Button(
                    onClick = onFiscalizeReceipt,
                    enabled = canPay && !isFiscalProcessing
                ) {
                    Text(if (isFiscalProcessing) "Друк..." else "Оплатити та закрити чек")
                }
            }
        }
    }
}
