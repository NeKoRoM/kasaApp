package com.kasa.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kasa.android.bluetooth.AndroidBluetoothDialog
import com.kasa.android.bluetooth.AndroidBluetoothScanner
import com.kasa.android.bluetooth.BluetoothConstants
import com.kasa.android.bluetooth.BluetoothDiagnosticReceiver
import com.kasa.feature.pos.PosEvent
import com.kasa.feature.pos.PosScreen
import com.kasa.feature.pos.PosViewModel
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.SimulatedKasaTransport
import com.kasa.transport.SwitchableKasaTransport
import kotlinx.coroutines.launch

/**
 * Головна точка входу Android POS-терміналу касира.
 * Підтримує нативну роботу на смартфонах, планшетах та спеціалізованих терміналах (Sunmi, iMin, Urovo):
 * - Захист від блокування та згасання екрана касира (FLAG_KEEP_SCREEN_ON);
 * - Підтримка апаратних бічних клавіш сканера (Sunmi yellow button, KEYCODE_BUTTON_R1, KEYCODE_SCANCODE);
 * - Коректна обробка безпечних зон екрана (Safe Drawing / Insets) для мобільних пристроїв;
 * - Апаратне Bluetooth SPP підключення до РРО Exellio/Datecs з автоматичним відновленням сесії.
 */
class MainActivity : ComponentActivity() {

    private var activeViewModel: PosViewModel? = null
    private var activeTransport: KasaTransport? = null
    private var diagnosticReceiver: BluetoothDiagnosticReceiver? = null
    private val barcodeBuffer = StringBuilder()
    private var lastKeyTimestampMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Запобігаємо згасанню екрана під час робочої зміни касира
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        diagnosticReceiver = BluetoothDiagnosticReceiver(
            context = applicationContext,
            transportProvider = { activeTransport },
            viewModelProvider = { activeViewModel }
        ).also { it.register() }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val simulatedTransport = remember { SimulatedKasaTransport() }
            val switchableTransport = remember {
                SwitchableKasaTransport(simulatedTransport, coroutineScope).also { activeTransport = it }
            }
            val receiptEngine = remember { FiscalReceiptEngine(switchableTransport) }
            val serviceEngine = remember { FiscalServiceEngine(switchableTransport) }

            val scanner = remember { AndroidBluetoothScanner(applicationContext) }
            DisposableEffect(scanner) {
                onDispose { scanner.release() }
            }

            val prefs = remember {
                getSharedPreferences(BluetoothConstants.PREFS_NAME, Context.MODE_PRIVATE)
            }

            val storageHolder = remember { AndroidPosStorageHolder(filesDir) }
            val initialProducts = remember { storageHolder.loadInitialProducts() }
            val initialCustomers = remember { storageHolder.loadInitialCustomers() }

            val viewModel = remember {
                PosViewModel(
                    receiptEngine = receiptEngine,
                    serviceEngine = serviceEngine,
                    transport = switchableTransport,
                    initialCatalog = initialProducts,
                    initialHeldReceipts = storageHolder.loadInitialHeldReceipts(),
                    initialDrawerBalanceKopecks = storageHolder.loadInitialDrawerBalance(),
                    initialCustomers = initialCustomers,
                    initialShift = storageHolder.loadInitialShift(),
                    onReceiptFiscalized = { receipt ->
                        storageHolder.journalStorage.recordReceipt(receipt)
                    },
                    onCatalogUpdated = { storageHolder.productStorage.saveCatalog(it) },
                    onHeldReceiptsUpdated = { storageHolder.heldStorage.saveHeldReceipts(it) },
                    onDrawerBalanceUpdated = { newBalance ->
                        storageHolder.saveDrawerBalance(newBalance)
                    },
                    onShiftUpdated = { storageHolder.shiftStorage.saveCurrentShift(it) },
                    onShiftClosed = { storageHolder.shiftStorage.recordClosedShift(it) }
                ).also { activeViewModel = it }
            }
            val state by viewModel.state.collectAsState()
            val transportState by switchableTransport.state.collectAsState()

            var isBluetoothDialogOpen by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val savedJournal = storageHolder.loadJournal()
                viewModel.onEvent(PosEvent.UpdateJournal(savedJournal))

                val useSimulator = prefs.getBoolean(BluetoothConstants.KEY_USE_SIMULATOR, true)
                val lastMac = prefs.getString(BluetoothConstants.KEY_LAST_MAC, null)

                if (!useSimulator && !lastMac.isNullOrBlank()) {
                    val btTransport = BluetoothKasaTransport(adapter = scanner.bluetoothAdapter)
                    switchableTransport.switchTo(btTransport)
                    switchableTransport.connect(lastMac)
                } else {
                    switchableTransport.connect("SIMULATED-RRO")
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    PosScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onOpenConnectionDialog = { isBluetoothDialogOpen = true },
                        onOpenTaxUrl = { url ->
                            runCatching {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        onShareReceipt = { receiptText ->
                            runCatching {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, receiptText)
                                    type = "text/plain"
                                }
                                startActivity(Intent.createChooser(sendIntent, "Поділитися чеком"))
                            }
                        }
                    )

                    if (isBluetoothDialogOpen) {
                        AndroidBluetoothDialog(
                            currentState = transportState,
                            scanner = scanner,
                            onConnectSimulated = {
                                coroutineScope.launch {
                                    prefs.edit().putBoolean(BluetoothConstants.KEY_USE_SIMULATOR, true).apply()
                                    switchableTransport.switchTo(simulatedTransport)
                                    switchableTransport.connect("SIMULATED-RRO")
                                    isBluetoothDialogOpen = false
                                }
                            },
                            onConnectBluetooth = { mac, name ->
                                coroutineScope.launch {
                                    prefs.edit()
                                        .putBoolean(BluetoothConstants.KEY_USE_SIMULATOR, false)
                                        .putString(BluetoothConstants.KEY_LAST_MAC, mac)
                                        .putString(BluetoothConstants.KEY_LAST_NAME, name)
                                        .apply()

                                    val btTransport = BluetoothKasaTransport(adapter = scanner.bluetoothAdapter)
                                    switchableTransport.switchTo(btTransport)
                                    val ok = switchableTransport.connect(mac)
                                    if (ok) {
                                        isBluetoothDialogOpen = false
                                    }
                                }
                            },
                            onDisconnect = {
                                coroutineScope.launch { switchableTransport.disconnect() }
                            },
                            onDismiss = { isBluetoothDialogOpen = false }
                        )
                    }
                }
            }
        }
    }

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val vm = activeViewModel ?: return super.dispatchKeyEvent(event)

        // Обробка апаратних кнопок запуску сканування на POS-терміналах (Sunmi V2 / V2 Pro / PDA)
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_R1 || event.keyCode == 293 || event.keyCode == 301) {
            vm.onEvent(PosEvent.ToggleScannerDialog(true))
            return true
        }

        // Перехоплення швидких HID-послідовностей від USB OTG / Bluetooth сканера
        val now = System.currentTimeMillis()
        if (now - lastKeyTimestampMs > 65L && barcodeBuffer.isNotEmpty()) {
            barcodeBuffer.clear()
        }

        if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (barcodeBuffer.length >= 3) {
                val scanned = barcodeBuffer.toString()
                barcodeBuffer.clear()
                vm.onEvent(PosEvent.ScanBarcode(scanned))
                return true
            }
            barcodeBuffer.clear()
        }

        val unicodeChar = event.keyCharacterMap.get(event.keyCode, event.metaState)
        if (unicodeChar >= 32 && !Character.isISOControl(unicodeChar)) {
            barcodeBuffer.append(unicodeChar.toChar())
            lastKeyTimestampMs = now
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        diagnosticReceiver?.unregister()
        diagnosticReceiver = null
        activeTransport = null
        activeViewModel = null
        super.onDestroy()
    }
}
