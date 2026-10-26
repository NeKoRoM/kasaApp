package com.kasa.android.bluetooth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.protocol.transport.TransportState

/**
 * Список спарених та виявлених в ефірі Bluetooth-пристроїв (РРО) для мобільного діалогу.
 * Оптимізовано під сенсорне керування та стабільні розриви рядків без артефактів.
 */
@Composable
fun AndroidBluetoothDeviceList(
    bondedDevices: List<DiscoveredBluetoothDevice>,
    scannedDevices: List<DiscoveredBluetoothDevice>,
    currentState: TransportState,
    isScanning: Boolean,
    onRefreshBonded: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectBluetooth: (mac: String, deviceName: String) -> Unit,
    maxListHeight: androidx.compose.ui.unit.Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxListHeight),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Спарені пристрої (${bondedDevices.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                TextButton(
                    onClick = onRefreshBonded,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                ) {
                    Text("⟳ Оновити", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        if (bondedDevices.isEmpty()) {
            item {
                Text("Немає спарених Bluetooth-пристроїв. Запустіть пошук нижче.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(bondedDevices) { dev ->
                AndroidBluetoothDeviceRow(
                    device = dev,
                    isCurrentlyConnected = currentState is TransportState.Connected && currentState.target.contains(dev.address),
                    onConnect = { onConnectBluetooth(dev.address, dev.name) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Виявлені в ефірі (${scannedDevices.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                if (isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        TextButton(
                            onClick = onStopDiscovery,
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        ) {
                            Text("Зупинити", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                        }
                    }
                } else {
                    TextButton(
                        onClick = onStartDiscovery,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                    ) {
                        Text("🔍 Шукати", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        if (scannedDevices.isEmpty()) {
            item {
                Text(
                    if (isScanning) "Сканування ефіру на наявність РРО..." else "Натисніть 'Шукати' для виявлення кас поблизу.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(scannedDevices) { dev ->
                AndroidBluetoothDeviceRow(
                    device = dev,
                    isCurrentlyConnected = currentState is TransportState.Connected && currentState.target.contains(dev.address),
                    onConnect = {
                        onStopDiscovery()
                        onConnectBluetooth(dev.address, dev.name)
                    }
                )
            }
        }
    }
}
