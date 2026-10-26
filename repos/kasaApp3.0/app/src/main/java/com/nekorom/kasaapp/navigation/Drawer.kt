// =================================================================================================
// БІЧНЕ НАВІГАЦІЙНЕ МЕНЮ (MODAL NAVIGATION DRAWER ДЛЯ POS-ТЕРМІНАЛУ)
//
// Архітектурний шар: Presentation / Navigation Layer (Jetpack Compose).
// Призначення:
// 1. Надає швидкий доступ до ключових розділів касового терміналу:
//    - Сканер штрих-кодів (Scanner)
//    - Товарний каталог та кошик чека (List / ReceiptProductList)
//    - Системні налаштування РРО, податків, груп (Settings)
//    - Архів та історія чеків (ReceiptScreen)
//    - Меню Bluetooth-з'єднання з РРО та вагами (Bluetooth)
//    - Вихід з облікового запису касира (Log out)
// 2. Реалізує перемикання орієнтації екрана (Портретна / Альбомна для планшетів).
// 3. Відображає системні діалоги блокування касира (BadUser) та вибору Bluetooth-каси (BluetoothDialog).
// =================================================================================================

package com.nekorom.kasaapp.navigation

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Create
import androidx.compose.material.icons.twotone.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.btactivity.BtActivity
import com.nekorom.kasaapp.keys.USER_ERR
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.USER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Головний контейнер навігаційного меню [ModalNavigationDrawer].
 *
 * @param navHostController Контролер глобальної навігації (рівень Login / App).
 * @param viewModel Головна [MainViewModel] для контролю сесії користувача та стану BT.
 * @param actListLauncher Лаунчер запуску системних Activity (список Bluetooth пристроїв).
 */
@Composable
fun Drawer(
    navHostController: NavHostController,
    viewModel: MainViewModel,
    actListLauncher: ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current
    var dialogState by remember { mutableStateOf(false) }

    // Спостереження за помилками авторизації / ліцензії касира
    var dialogUserState = viewModel.dialogState.observeAsState().value

    // Якщо касира деактивовано або виявлено невідповідність — показуємо блокуючий діалог
    if (dialogUserState == USER_ERR)
        BadUser(onDismiss = {
            viewModel.logOut()
            navHostController.popBackStack(NavRouteLoginDrawer.Login.route, false)
            viewModel.dialogState.value = "no" }) {
            viewModel.logOut()
            navHostController.popBackStack(NavRouteLoginDrawer.Login.route, false)
        }

    // Внутрішній контролер навігації між робочими екранами каси
    lateinit var navHostControllerInApp : NavHostController

    // Список пунктів меню касового терміналу
    val items = listOf(
        DrawerItem(painterResource(R.drawable.hend_scanner), "Scanner", NavRoute.Scanner.route),
        DrawerItem(
            painterResource(R.drawable.numbered_list),
            "List",
            NavRoute.ReceiptProductListScreen.route
        ),
        DrawerItem(
            rememberVectorPainter(image = Icons.Default.Settings),
            "Settings",
            NavRoute.Settings.route
        ),
        DrawerItem(
            rememberVectorPainter(image = Icons.TwoTone.Receipt),
            "чеки",
            NavRoute.ReceiptScreen.route
        ),
        DrawerItem(
            rememberVectorPainter(image = Icons.Default.BluetoothConnected),
            "Bluetooth",
            NavRoute.BtListScreen.route
        ),
        DrawerItem(
            rememberVectorPainter(image = Icons.Default.ArrowBack),
            "Log out",
            NavRoute.Login.route
        ),
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollableState = rememberScrollState()
    val selectItem = remember {
        mutableStateOf(items[0])
    }
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollableState)) {
            // Верхній логотип/заголовок бічного меню
            Image(painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.height(15.dp))

            // Рендеринг елементів навігаційного меню
            items.forEach{item ->
                NavigationDrawerItem(label = { Text(item.title)},
                    selected = selectItem.value == item,
                    icon = {
                        Icon(painter = item.image, contentDescription = item.title, modifier = Modifier.size(40.dp))
                    },
                    onClick = {
                        coroutineScope.launch {
                            selectItem.value = item
                            withContext(Dispatchers.Main){
                                // Перехід на цільовий маршрут, якщо це не вихід і не BT діалог
                                if (navHostControllerInApp.currentDestination?.route != selectItem.value.route &&
                                    selectItem.value.route != NavRoute.Login.route &&
                                    selectItem.value.route != NavRoute.BtListScreen.route){
                                    navHostControllerInApp.navigate(selectItem.value.route) {}
                                }
                            }
                            drawerState.close()

                            // Обробка виходу з акаунта
                            if (selectItem.value.route == NavRoute.Login.route){
                                viewModel.logOut()
                                navHostController.popBackStack(NavRouteLoginDrawer.Login.route, false)
                            }

                            // Відкриття діалогу підключення Bluetooth
                            if (selectItem.value.route == NavRoute.BtListScreen.route){
                                dialogState = true
                            }
                        }
                    })
            }
            
            // Перемикач орієнтації екрана (Портретна / Альбомна для планшетів)
            var orientationSwitch by rememberSaveable {
                mutableStateOf(false)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = orientationSwitch, onCheckedChange = {
                    if (!it)
                        (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    orientationSwitch = it
                })
                Text(text = "орієнтація")
            }
        }
    }, content = {
        // Відображення діалогу підключення до РРО
        when {
            dialogState -> {
                BluetoothDialog(
                    onDismissRequest = {
                        dialogState = false
                    },
                    onConnectRequest = {
                        USER?.mac?.let { it1 -> 
                            viewModel.btConnect(it1)
                            dialogState = false
                        }
                    },
                    onRegisterRequest = {
                        dialogState = false
                        viewModel.logOut()
                        navHostController.popBackStack(NavRouteLoginDrawer.Login.route, false)
                    }
                )
            }
        }

        // Внутрішній хост навігації між екранами авторизованого касира
        navHostControllerInApp = MyNavHost(drawerState, viewModel)
    })
}

/**
 * Діалог вибору та підключення до фіскального касового апарата Datecs через Bluetooth.
 *
 * @param onDismissRequest Закриття діалогового вікна.
 * @param onRegisterRequest Перехід до перереєстрації/зміни каси.
 * @param onConnectRequest Ініціалізація зв'язку за збереженою MAC-адресою [USER.mac].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDialog(
    onDismissRequest: () -> Unit, 
    onRegisterRequest: () -> Unit, 
    onConnectRequest: () -> Unit
) {
    AlertDialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ){
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Icon(painter = painterResource(com.nekorom.bt.R.drawable.ic_bluetooth), contentDescription = "")
                }
                Text(text = Constants.Keys.CONNECT_TO_KASA)
                Text(text = USER?.locationAddress + "", color = Color.Red)
                Row {
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(onClick = { onConnectRequest() }) {
                        Text(text = Constants.Keys.CONNECT_BUTTON)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(onClick = { onDismissRequest() }) {
                        Text(text = Constants.Keys.EXIT_DIALOG)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
                Button(onClick = { onRegisterRequest() }) {
                    Text(text = Constants.Keys.CONNECT_TO_OTHER)
                }
            }
        }
    }
}

/**
 * Блокуючий діалог при виявленні неактивного користувача або ліцензійної помилки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadUser(onDismiss: () -> Unit, onTry: () -> Unit) {
    AlertDialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = MaterialTheme.shapes.large
        ){
            Column {
                Text(text = Constants.Keys.USER_ERR)
                Button(onClick = {
                    onTry()
                    onDismiss()
                }) {
                    Text(text = Constants.Keys.LOG_OUT)
                }
                Button(onClick = { onDismiss() }) {
                    Text(text = "ok")
                }
            }
        }
    }
}

/**
 * Елемент навігаційного меню.
 *
 * @param image Графічна іконка пункту меню.
 * @param title Відображувана назва.
 * @param route Унікальний маршрут навігації Compose.
 */
data class DrawerItem(
    val image: Painter,
    val title: String,
    val route: String
)