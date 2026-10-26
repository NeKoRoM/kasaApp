package com.kasa.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.protocol.transport.TransportState

/**
 * Діалогове вікно вибору каналу зв'язку та керування Bluetooth-підключенням до РРО.
 * Забезпечує прямий доступ до спарених апаратів, сканування ефіру та перемикання на симулятор.
 */
@SuppressLint("MissingPermission")
@Composable
fun AndroidBluetoothDialog(
    currentState: TransportState,
    scanner: AndroidBluetoothScanner,
    onConnectSimulated: () -> Unit,
    onConnectBluetooth: (mac: String, deviceName: String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bondedDevices by scanner.bondedDevices.collectAsState()
    val scannedDevices by scanner.scannedDevices.collectAsState()
    val scanState by scanner.scanState.collectAsState()
    val isScanning = scanState is BluetoothScanState.Scanning
    val isBtEnabled by scanner.isBluetoothEnabled.collectAsState()
    var isLocEnabled by remember { mutableStateOf(scanner.isLocationEnabled()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (scanner.hasConnectPermission()) {
            scanner.refreshBondedDevices()
        }
        if (scanner.hasScanPermissions()) {
            scanner.startDiscovery()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (scanner.hasConnectPermission()) {
            scanner.refreshBondedDevices()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isLocEnabled = scanner.isLocationEnabled()
    }

    LaunchedEffect(Unit) {
        if (!scanner.hasConnectPermission() || !scanner.hasScanPermissions()) {
            permissionLauncher.launch(scanner.getRequiredPermissions())
        } else {
            scanner.refreshBondedDevices()
            scanner.startDiscovery()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isCompactHeight = maxHeight < 500.dp
        val maxListHeight = if (isCompactHeight) 140.dp else 260.dp

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Підключення РРО (BT)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Поточний стан зв'язку:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                            val statusText = when (currentState) {
                                is TransportState.Connected -> "Підключено: ${currentState.target}"
                                is TransportState.Connecting -> "З'єднання з: ${currentState.target}..."
                                is TransportState.Disconnected -> "Не підключено (Офлайн)"
                                is TransportState.Error -> "Помилка: ${currentState.message}"
                            }
                            val statusColor = when (currentState) {
                                is TransportState.Connected -> MaterialTheme.colorScheme.tertiary
                                is TransportState.Connecting -> MaterialTheme.colorScheme.secondary
                                is TransportState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                                is TransportState.Error -> MaterialTheme.colorScheme.error
                            }
                            Text(statusText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = statusColor)
                        }
                    }

                    if (!isBtEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bluetooth вимкнено", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                Button(
                                    onClick = { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Text("Увімкнути", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("GPS вимкнено (потрібен для пошуку)", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                Button(
                                    onClick = { locationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Text("GPS", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    if (scanState is BluetoothScanState.Error) {
                        Text(
                            text = (scanState as BluetoothScanState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }

                    AndroidBluetoothDeviceList(
                        bondedDevices = bondedDevices,
                        scannedDevices = scannedDevices,
                        currentState = currentState,
                        isScanning = isScanning,
                        onRefreshBonded = {
                            if (scanner.hasConnectPermission()) {
                                scanner.refreshBondedDevices()
                            } else {
                                permissionLauncher.launch(scanner.getRequiredPermissions())
                            }
                        },
                        onStartDiscovery = {
                            if (scanner.hasScanPermissions()) {
                                scanner.startDiscovery()
                            } else {
                                permissionLauncher.launch(scanner.getRequiredPermissions())
                            }
                        },
                        onStopDiscovery = { scanner.stopDiscovery() },
                        onConnectBluetooth = onConnectBluetooth,
                        maxListHeight = maxListHeight
                    )
                }
            },
            confirmButton = {
                if (isCompactHeight) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentState is TransportState.Connected) {
                            OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Відключити", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        } else {
                            OutlinedButton(onClick = onConnectSimulated, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp)) {
                                Text("🧪 Симулятор", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        Button(onClick = onDismiss, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("Закрити", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onConnectSimulated, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("🧪 Увімкнути симулятор РРО", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        if (currentState is TransportState.Connected) {
                            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Відключити РРО", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("Закрити", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            },
            dismissButton = null,
            modifier = Modifier.fillMaxWidth(0.96f).widthIn(max = 500.dp)
        )
    }
}
