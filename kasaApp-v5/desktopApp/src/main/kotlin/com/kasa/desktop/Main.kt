package com.kasa.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kasa.feature.pos.*
import com.kasa.feature.sync.*
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.launch
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 780.dp)

    val coroutineScope = rememberCoroutineScope()
    val simulatedTransport = remember { SimulatedKasaTransport() }
    val switchableTransport = remember { SwitchableKasaTransport(simulatedTransport, coroutineScope) }
    val receiptEngine = remember { FiscalReceiptEngine(switchableTransport) }
    val serviceEngine = remember { FiscalServiceEngine(switchableTransport) }

    val homeDir = remember { File(System.getProperty("user.home"), ".kasaapp") }
    val productStorage = remember { FileJsonProductStorage(File(homeDir, "catalog.json")) }
    val journalStorage = remember { FileJsonReceiptJournalStorage(File(homeDir, "receipts_journal.json")) }
    val heldStorage = remember { FileJsonHeldReceiptStorage(File(homeDir, "held_receipts.json")) }
    val drawerStorage = remember { FileJsonCashDrawerStorage(File(homeDir, "cash_drawer.json")) }
    val customerStorage = remember { FileJsonCustomerStorage(File(homeDir, "customers.json")) }
    val shiftStorage = remember { FileJsonShiftStorage(File(homeDir, "shifts_journal.json")) }
    val syncStorage = remember { FileJsonSyncStorage(File(homeDir, "offline_orders.json")) }
    val syncClient = remember { SyncClient("http://localhost:8000") }
    val syncQueue = remember { SyncQueueLogic(syncClient, persistence = syncStorage) }

    val initialProducts = remember {
        val loaded = productStorage.loadCatalog()
        if (loaded.isNotEmpty()) {
            loaded
        } else {
            val samples = DesktopSampleCatalog.createSampleCatalog()
            productStorage.saveCatalog(samples)
            samples
        }
    }

    val viewModel = remember {
        PosViewModel(
            receiptEngine = receiptEngine,
            serviceEngine = serviceEngine,
            transport = switchableTransport,
            initialCatalog = initialProducts,
            initialHeldReceipts = heldStorage.loadHeldReceipts(),
            initialDrawerBalanceKopecks = drawerStorage.loadDrawerState().balanceKopecks,
            initialCustomers = customerStorage.loadCustomers(),
            initialShift = shiftStorage.loadCurrentShift(),
            onReceiptFiscalized = { receipt ->
                journalStorage.recordReceipt(receipt)
                syncQueue.enqueue(OrderCreateDto.fromReceipt(receipt, cashRegisterId = 1))
            },
            onCatalogUpdated = { productStorage.saveCatalog(it) },
            onHeldReceiptsUpdated = { heldStorage.saveHeldReceipts(it) },
            onDrawerBalanceUpdated = { newBalance ->
                drawerStorage.saveDrawerState(CashDrawerState(newBalance, System.currentTimeMillis()))
            },
            onShiftUpdated = { shiftStorage.saveCurrentShift(it) },
            onShiftClosed = { shiftStorage.recordClosedShift(it) }
        )
    }

    val syncWorker = remember {
        PeriodicSyncWorker(
            syncQueue = syncQueue,
            syncClient = syncClient,
            scope = coroutineScope,
            onCatalogDownloaded = { catalog ->
                productStorage.saveCatalog(catalog)
                viewModel.onEvent(PosEvent.UpdateCatalog(catalog))
            },
            syncIntervalMs = 30_000L
        )
    }

    var isCustomerDisplayOpen by remember { mutableStateOf(false) }

    val barcodeWedge = remember {
        DesktopBarcodeScannerWedge(
            onHotKey = { key ->
                when (key) {
                    androidx.compose.ui.input.key.Key.F2 -> { viewModel.onEvent(PosEvent.ToggleScannerDialog(true)); true }
                    androidx.compose.ui.input.key.Key.F3 -> { viewModel.onEvent(PosEvent.PromptProductEdit(null, null)); true }
                    androidx.compose.ui.input.key.Key.F4 -> { viewModel.onEvent(PosEvent.ToggleNumpadDialog(true)); true }
                    androidx.compose.ui.input.key.Key.F7 -> { viewModel.onEvent(PosEvent.OpenCashDrawer); true }
                    androidx.compose.ui.input.key.Key.F8 -> { isCustomerDisplayOpen = !isCustomerDisplayOpen; true }
                    androidx.compose.ui.input.key.Key.F9 -> { viewModel.onEvent(PosEvent.FiscalizeReceipt); true }
                    androidx.compose.ui.input.key.Key.Escape -> { viewModel.onEvent(PosEvent.DismissStatusMessage); true }
                    else -> false
                }
            },
            onBarcodeScanned = { scannedBarcode -> viewModel.onEvent(PosEvent.ScanBarcode(scannedBarcode)) }
        )
    }

    LaunchedEffect(Unit) {
        switchableTransport.connect("SIMULATED-RRO")
        val savedJournal = journalStorage.loadJournal()
        viewModel.onEvent(PosEvent.UpdateJournal(savedJournal))
        syncWorker.start("demo-token")
    }

    DisposableEffect(Unit) {
        onDispose {
            syncWorker.stop()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "KasaApp POS Terminal v5 (AI-Native Cashier)",
        onKeyEvent = barcodeWedge::onKeyEvent
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF1976D2),
                secondary = Color(0xFF00796B),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E)
            )
        ) {
            val state by viewModel.state.collectAsState()
            val pendingOrders by syncQueue.pendingOrders.collectAsState()
            val syncWorkerState by syncWorker.state.collectAsState()
            val transportState by switchableTransport.state.collectAsState()
            var syncStatus by remember { mutableStateOf<String?>(null) }
            var isConnectionDialogOpen by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize()) {
                // ВЕРХНІЙ СИСТЕМНИЙ БАР
                DesktopTopBar(
                    isKasaOnline = state.isKasaOnline,
                    pendingOrdersCount = pendingOrders.size,
                    syncWorkerState = syncWorkerState,
                    isFiscalProcessing = state.isFiscalProcessing,
                    canPay = state.canPay,
                    heldReceiptsCount = state.heldReceipts.size,
                    kasaStatus = state.kasaStatus,
                    currentShift = state.currentShift,
                    onToggleShiftReportDialog = { viewModel.onEvent(PosEvent.ToggleShiftReportDialog) },
                    onVoidReceipt = { viewModel.onEvent(PosEvent.VoidReceipt) },
                    onOpenConnectionDialog = { isConnectionDialogOpen = true },
                    onManualSync = {
                        coroutineScope.launch {
                            val summary = syncWorker.performSyncCycle("demo-token")
                            syncStatus = "Синхронізація: відправлено ${summary.uploadedCount}, в черзі ${summary.remainingCount}"
                        }
                    },
                    onPrintXReport = { viewModel.onEvent(PosEvent.PrintXReport) },
                    onPrintZReport = { viewModel.onEvent(PosEvent.PrintZReport) },
                    onToggleServiceDialog = { viewModel.onEvent(PosEvent.ToggleServiceDialog) },
                    onToggleJournalDialog = { viewModel.onEvent(PosEvent.ToggleJournalDialog) },
                    onToggleHeldReceiptsDialog = { viewModel.onEvent(PosEvent.ToggleHeldReceiptsDialog) },
                    onToggleCatalogManagement = { viewModel.onEvent(PosEvent.ToggleCatalogManagementDialog(true)) },
                    onRefreshCatalog = {

                        coroutineScope.launch {
                            when (val res = syncClient.fetchProducts("demo-token")) {
                                is SyncResult.Success -> {
                                    productStorage.saveCatalog(res.data)
                                    viewModel.onEvent(PosEvent.UpdateCatalog(res.data))
                                    syncStatus = "Каталог оновлено з сервера (${res.data.size} товарів)"
                                }
                                is SyncResult.Failure -> syncStatus = "Помилка оновлення каталогу: ${res.message}"
                            }
                        }
                    },
                    isCustomerDisplayOpen = isCustomerDisplayOpen,
                    onToggleCustomerDisplay = { isCustomerDisplayOpen = !isCustomerDisplayOpen },
                    onFiscalizeReceipt = { viewModel.onEvent(PosEvent.FiscalizeReceipt) }
                )

                syncStatus?.let { msg ->
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF004D40)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(msg, color = Color.White)
                            TextButton(onClick = { syncStatus = null }) { Text("ОК", color = Color.White) }
                        }
                    }
                }

                state.statusMessage?.let { msg ->
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF004D40)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(msg, color = Color.White)
                            TextButton(onClick = { viewModel.onEvent(PosEvent.DismissStatusMessage) }) { Text("ОК", color = Color.White) }
                        }
                    }
                }

                // ОСНОВНИЙ ДВОПАНЕЛЬНИЙ POS ЕКРАН
                PosScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onOpenTaxUrl = { url -> runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) } },
                    modifier = Modifier.weight(1f)
                )

                if (isConnectionDialogOpen) {
                    PosConnectionDialog(
                        currentState = transportState,
                        onConnectSimulated = {
                            coroutineScope.launch {
                                switchableTransport.switchTo(simulatedTransport); switchableTransport.connect("SIMULATED-RRO"); isConnectionDialogOpen = false
                            }
                        },
                        onConnectSerial = { port, baud ->
                            coroutineScope.launch {
                                val serialTransport = com.kasa.transport.SerialKasaTransport(com.kasa.transport.SerialPortConfig(baudRate = baud))
                                switchableTransport.switchTo(serialTransport)
                                if (switchableTransport.connect(port)) isConnectionDialogOpen = false
                            }
                        },
                        onDisconnect = { coroutineScope.launch { switchableTransport.disconnect() } },
                        onDismiss = { isConnectionDialogOpen = false }
                    )
                }
            }
        }
    }

    if (isCustomerDisplayOpen) {
        val custState by viewModel.state.collectAsState()
        Window(
            onCloseRequest = { isCustomerDisplayOpen = false },
            title = "Екран покупця — KasaApp POS",
            state = rememberWindowState(width = 800.dp, height = 580.dp)
        ) {
            PosCustomerDisplayScreen(state = custState)
        }
    }
}
