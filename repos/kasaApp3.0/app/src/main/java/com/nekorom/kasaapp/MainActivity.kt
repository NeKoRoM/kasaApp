// =================================================================================================
// ГОЛОВНА АКТИВНІСТЬ POS-ТЕРМІНАЛУ (MAIN ANDROID ENTRY ACTIVITY)
//
// Архітектурний шар: Presentation Layer (Jetpack Compose + Dagger Hilt @AndroidEntryPoint).
// Призначення:
// 1. Коренева точка входу касового терміналу kasaApp3.0.
// 2. Встановлює системний прапорець FLAG_KEEP_SCREEN_ON (запобігає засинанню екрана планшета на касі).
// 3. Відстежує глобальні стани:
//    - Bluetooth-зв'язок з РРО (CONNECTED, NOT_CONNECTED, CONNECTING) та автоматичний реконект.
//    - Таймаути відповідей апарата (NOT_ANSWER) з інформуванням касира.
//    - Живе налагодження обміну протокольними пакетами (nowSendCommand, lastGetAns).
//    - Блокування терміналу при відкликанні ліцензії через Firebase (USER_ERR).
// =================================================================================================

package com.nekorom.kasaapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoBackpack
import androidx.compose.material.icons.filled.NoTransfer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.nekorom.bt.BluetoothConstants
import com.nekorom.bt.bluetooth.BluetoothController
import com.nekorom.kasaapp.keys.USER_ERR
import com.nekorom.kasaapp.navigation.LoginDrawerNavHost
import com.nekorom.kasaapp.screens.MyDialog
import com.nekorom.kasaapp.ui.theme.KasaAppTheme
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.Constants.ResultCode.MAC_RESULT
import com.nekorom.kasaapp.utils.USER
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Головна активність касового терміналу.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var preferences: SharedPreferences? = null
    private lateinit var actListLauncher: ActivityResultLauncher<Intent>
    private var connState = keys.NOT_CONNECTED

    // Головна ViewModel програми
    val viewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Фіксація екрана постійно увімкненим для безперервної роботи касового вузла
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ініціалізація реєстратора результатів вибору Bluetooth пристрою
        onBtListResult()
        preferences = getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)

        setContent {
            KasaAppTheme {
                var connectState by remember { mutableStateOf(keys.NOT_CONNECTED) }
                var dialogBtState by remember { mutableStateOf(false) }
                var dialogAnswerState by remember { mutableStateOf(false) }
                var dialogUserState by remember { mutableStateOf(false) }
                var requestToBtState by remember { mutableStateOf(false) }
                var requestText by remember { mutableStateOf("") }
                var ansText by remember { mutableStateOf("") }
                var job: Job? = null

                // Спостереження за станом Bluetooth з'єднання
                viewModel.btState.observe(this) {
                    connectState = it
                    connState = it
                    if (it == keys.NOT_CONNECTED)
                        dialogBtState = true
                }

                // Спостереження за системними діалогами та ліцензійними помилками
                viewModel.dialogState.observe(this) {
                    when (it) {
                        USER_ERR -> {
                            dialogUserState = true
                        }
                    }
                }

                // Спостереження за таймаутом відповіді РРО
                viewModel.btAnswer.observe(this) {
                    if (it == BluetoothController.NOT_ANSWER &&
                        !dialogBtState &&
                        connectState != keys.NOT_CONNECTED) {
                        dialogAnswerState = true
                    }
                }

                // Відображення поточної відправленої команди на HUD-панелі
                viewModel.btKasaExellioRepository.nowSendCommand.observe(this) {
                    job?.cancel()
                    job = CoroutineScope(Dispatchers.Default).launch {
                        requestText = it
                        requestToBtState = true
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.btRequestState.value = true
                        }
                        delay(1000)
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.btRequestState.value = false
                        }
                        requestToBtState = false
                    }
                }

                // Відображення останньої отриманої відповіді від РРО
                viewModel.btKasaExellioRepository.lastGetAns.observe(this) {
                    job?.cancel()
                    job = CoroutineScope(Dispatchers.Default).launch {
                        ansText = it
                        requestToBtState = true
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.btRequestState.value = true
                        }
                        delay(1000)
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.btRequestState.value = false
                        }
                        requestToBtState = false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Діалог помилки підключення до РРО з можливістю повторити спробу
                    if (dialogBtState)
                        MyDialog(
                            onDismissRequest = { dialogBtState = false },
                            onConfirmation = {
                                val mac = preferences?.getString(BluetoothConstants.MAC, "") ?: ""

                                if (mac != "" && viewModel.readAllUsers().value?.indexOfFirst { it.mac == mac } == -1)
                                    viewModel.btNewConnect(mac)
                                else if (USER?.mac != null)
                                    viewModel.btConnect(USER!!.mac)
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.btRequestState.value = true
                                    delay(4000)
                                    viewModel.btRequestState.value = false
                                }
                                dialogBtState = false
                            },
                            dialogTitle = Constants.Keys.CONNECT_ERROR,
                            dialogText = Constants.Keys.TRY_CONNECT_AGAIN,
                            confirmButtonText = stringResource(id = R.string.tak),
                            icon = painterResource(id = R.drawable.disconnect)
                        )

                    // Діалог відсутності відповіді від апарата (проблема шлейфу, живлення або режиму)
                    if (dialogAnswerState) {
                        MyDialog(
                            onDismissRequest = { dialogAnswerState = false },
                            onConfirmation = { dialogAnswerState = false },
                            dialogTitle = stringResource(R.string.kasa_not_answer),
                            dialogText = stringResource(R.string.shore_kasa_in_6),
                            icon = rememberVectorPainter(image = Icons.Default.NoTransfer)
                        )
                    }

                    Box {
                        // Кореневий графік навігації Compose
                        LoginDrawerNavHost(viewModel, actListLauncher) {}

                        // Спливаючий індикатор передачі команд через Bluetooth (HUD Overlay)
                        if (requestToBtState) {
                            Row(Modifier.align(Alignment.Center)) {
                                Surface(color = MaterialTheme.colorScheme.secondary) {
                                    Column {
                                        Text(text = requestText)
                                        Text(text = ansText)
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        // Іконка поточного статусу зв'язку з касою у правому верхньому кутку
                        Box(Modifier.align(Alignment.TopEnd)) {
                            Row {
                                when (connectState) {
                                    keys.CONNECTED -> {
                                        Icon(
                                            painter = painterResource(id = R.drawable.connect_bt),
                                            contentDescription = "Підключено",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color.Green
                                        )
                                        dialogBtState = false
                                    }
                                    keys.NOT_CONNECTED -> {
                                        Icon(
                                            painter = painterResource(id = R.drawable.disconnect),
                                            contentDescription = "Відключено",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color.Red
                                        )
                                    }
                                    keys.CONNECTING -> {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Реєстрація слухача для отримання MAC-адреси з активності пошуку Bluetooth пристроїв.
     */
    private fun onBtListResult() {
        actListLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val mac = result.data?.getSerializableExtra(MAC_RESULT) as String
                    if (connState == keys.NOT_CONNECTED)
                        viewModel.btNewConnect(mac)
                    else
                        viewModel.btConnected(mac)
                }
            }
    }
}




