package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Діалогове вікно сервісних функцій фіскального реєстратора (РРО):
 * - Моніторинг ідентифікаторів (ЗН, ФН)
 * - Зчитування та синхронізація годинника реального часу (RTC)
 * - Службові операції з готівкою (внесення/видача) та відкриття скриньки
 * - Керування дисплеєм покупця (VFD 2x20)
 * - Друк денних службових звітів (X-звіт, Z-звіт)
 */
@Composable
fun PosServiceDialog(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = { onEvent(PosEvent.ToggleServiceDialog) },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Сервісне меню РРО",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEvent(PosEvent.ToggleServiceDialog) }) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onOpenConnectionDialog != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Зв'язок з РРО", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val stColor = if (state.isKasaOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                val stText = if (state.isKasaOnline) "Підключено (Онлайн)" else "Офлайн (Не підключено)"
                                Text(stText, fontSize = 11.sp, color = stColor, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onEvent(PosEvent.ToggleServiceDialog)
                                    onOpenConnectionDialog()
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("📡 Зв'язок", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                ServiceHardwareCard(
                    hardwareInfo = state.kasaHardwareInfo,
                    onQueryInfo = { onEvent(PosEvent.QueryKasaHardwareInfo) }
                )

                ServiceRtcCard(
                    rtcTime = state.kasaRtcTime,
                    onReadTime = { onEvent(PosEvent.ReadKasaTime) },
                    onSyncTime = { onEvent(PosEvent.SyncKasaTime) }
                )

                ServiceCashCard(
                    cashDrawerBalanceKopecks = state.cashDrawerBalanceKopecks,
                    onPerformCashInOut = { onEvent(PosEvent.PerformCashInOut(it)) },
                    onOpenDrawer = { onEvent(PosEvent.OpenCashDrawer) }
                )

                ServiceDisplayCard(
                    line1 = state.customerDisplayLine1,
                    line2 = state.customerDisplayLine2,
                    onSendGreeting = {
                        onEvent(PosEvent.DisplayCustomerText("Ласкаво просимо!", "Каса вільна"))
                    },
                    onClearDisplay = {
                        onEvent(PosEvent.ClearCustomerDisplay)
                    },
                    onSendCustomText = { l1, l2 ->
                        onEvent(PosEvent.DisplayCustomerText(l1, l2))
                    }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Пам'ять артикулів РРО (PLU)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Синхронізація, дамп та запис товарів у пам'ять каси", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onEvent(PosEvent.ToggleServiceDialog)
                                onEvent(PosEvent.TogglePluDialog)
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("📦 PLU РРО", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                ServiceReportsCard(
                    onPrintX = { onEvent(PosEvent.PrintXReport) },
                    onPrintZ = { onEvent(PosEvent.PrintZReport) },
                    isReceiptOpen = state.kasaStatus?.isFiscalReceiptOpen == true,
                    onVoidReceipt = { onEvent(PosEvent.VoidReceipt) }
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onEvent(PosEvent.ToggleServiceDialog) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
            ) {
                Text("Закрити", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 560.dp)
    )
}
