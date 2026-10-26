package com.kasa.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.protocol.transport.TransportState
import com.kasa.transport.SerialKasaTransport
import com.kasa.transport.SerialPortInfo

/**
 * Діалог налаштування та вибору підключення до РРО:
 * - Перемикання між програмним емулятором та фізичним COM/Bluetooth-портом.
 * - Сканування доступних послідовних портів в операційній системі (macOS / Windows / Linux).
 */
@Composable
fun PosConnectionDialog(
    currentState: TransportState,
    onConnectSimulated: () -> Unit,
    onConnectSerial: (portName: String, baudRate: Int) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSimulatedMode by remember { mutableStateOf(currentState is TransportState.Connected && (currentState as? TransportState.Connected)?.target == "SIMULATED-RRO") }
    var availablePorts by remember { mutableStateOf(emptyList<SerialPortInfo>()) }
    var selectedPort by remember { mutableStateOf<String?>(null) }
    val baudRate by remember { mutableStateOf(115200) }

    LaunchedEffect(Unit) {
        availablePorts = SerialKasaTransport.listAvailablePorts()
        if (selectedPort == null && availablePorts.isNotEmpty()) {
            selectedPort = availablePorts.first().systemPortName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Підключення до РРО (Exellio / Datecs)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Статус поточного з'єднання
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Поточний стан:", fontSize = 12.sp, color = Color.Gray)
                        val statusText = when (currentState) {
                            is TransportState.Connected -> "Підключено: ${currentState.target}"
                            is TransportState.Connecting -> "З'єднання з: ${currentState.target}..."
                            is TransportState.Disconnected -> "Не підключено (Офлайн)"
                            is TransportState.Error -> "Помилка: ${currentState.message}"
                        }
                        val statusColor = when (currentState) {
                            is TransportState.Connected -> Color(0xFF2E7D32)
                            is TransportState.Connecting -> Color(0xFFE65100)
                            is TransportState.Disconnected -> Color.Gray
                            is TransportState.Error -> Color.Red
                        }
                        Text(statusText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = statusColor)
                    }
                }

                // Вибір режиму (Емулятор або Фізичний порт)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isSimulatedMode,
                        onClick = { isSimulatedMode = true },
                        label = { Text("Симулятор РРО", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = !isSimulatedMode,
                        onClick = {
                            isSimulatedMode = false
                            availablePorts = SerialKasaTransport.listAvailablePorts()
                            if (selectedPort == null && availablePorts.isNotEmpty()) {
                                selectedPort = availablePorts.first().systemPortName
                            }
                        },
                        label = { Text("Фізичний COM-порт", fontSize = 12.sp) }
                    )
                }

                if (isSimulatedMode) {
                    Text(
                        text = "Вбудований емулятор протоколу Exellio/Datecs. Дозволяє проводити чеки, друкувати X/Z звіти без реального апарата.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Виявлені порти (${availablePorts.size}):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { availablePorts = SerialKasaTransport.listAvailablePorts() }) {
                            Text("⟳ Оновити")
                        }
                    }

                    if (availablePorts.isEmpty()) {
                        Text("Портів не виявлено. Перевірте підключення кабелю USB або Bluetooth-пару.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availablePorts) { port ->
                                val isSelected = selectedPort == port.systemPortName
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPort = port.systemPortName },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPort = port.systemPortName }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(port.systemPortName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(port.portDescription, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentState is TransportState.Connected) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text("Відключити")
                    }
                }
                Button(
                    onClick = {
                        if (isSimulatedMode) {
                            onConnectSimulated()
                        } else {
                            selectedPort?.let { onConnectSerial(it, baudRate) }
                        }
                    },
                    enabled = isSimulatedMode || selectedPort != null
                ) {
                    Text("Підключити")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}
