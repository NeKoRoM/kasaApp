// =================================================================================================
// ВНУТРІШНІЙ ГРАФ НАВІГАЦІЇ POS-ТЕРМІНАЛУ (APP WORKSPACE NAVIGATION GRAPH)
//
// Архітектурний шар: Presentation / Navigation Layer (Jetpack Compose).
// Призначення:
// 1. Описує всі маршрути бізнес-екранів касового терміналу (сканування, чек, товари, налаштування).
// 2. Реалізує конфігурацію апаратної клавіатури: вимкнення екранної клавіатури (IME)
//    через `LocalTextInputService provides null` для зручної роботи з фізичним USB/BT-сканером.
// 3. Інжектує необхідні Hilt ViewModels (EditCodeViewModel, SettingsViewModel, TabletViewModel).
// =================================================================================================

package com.nekorom.kasaapp.navigation

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.screens.BtListScreenInApp
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.tablet.ReceiptProductList
import com.nekorom.kasaapp.screens.edit.EditProductScreen
import com.nekorom.kasaapp.screens.receipt.ReceiptScreen
import com.nekorom.kasaapp.screens.settings.SettingsScreen
import com.nekorom.kasaapp.screens.scan.CameraView
import com.nekorom.kasaapp.screens.scan.Scanner
import com.nekorom.kasaapp.screens.settings.AdminSettings
import com.nekorom.kasaapp.screens.settings.CameraSettings
import com.nekorom.kasaapp.screens.settings.GroupSettings
import com.nekorom.kasaapp.screens.settings.KasaSettings
import com.nekorom.kasaapp.screens.settings.SettingsViewModel
import com.nekorom.kasaapp.screens.settings.SettingsVisibleScreen
import com.nekorom.kasaapp.screens.settings.UKTZSettings
import com.nekorom.kasaapp.screens.tablet.TabletViewModel

/**
 * Ієрархія маршрутів для робочого простору касира.
 */
sealed class NavRoute(val route: String) {
    object ScanListTopBar: NavRoute("scanner_list_top_bar")
    object Login: NavRoute("login_screen")
    object Scanner: NavRoute("scanner_screen")

    // Розділи налаштувань
    object Settings: NavRoute("settings_screen")
    object SettingsVisible: NavRoute("settings_visible_screen")
    object SettingsKasa: NavRoute("settings_kasa_screen")
    object SettingsGroup: NavRoute("settings_group_screen")
    object SettingsUktz: NavRoute("settings_Uktz_screen")
    object SettingsCam: NavRoute("settings_Cam_screen")
    object SettingAdmin: NavRoute("settings_Admin")

    // Каталог, редагування, чеки та Bluetooth
    object ReceiptProductListScreen: NavRoute("receipt_product_list_screen")
    object EditProductScreen: NavRoute("edit_product_screen")
    object BtListScreen: NavRoute("bt_screen")
    object BarCodeScanScreen: NavRoute("bar_code_scan")
    object ReceiptScreen: NavRoute("receipt_screen")
}

/**
 * Головний навігаційний хост робочих екранів касового терміналу.
 *
 * @param drawerState Стан бічного навігаційного меню для відкриття/закриття жестів.
 * @param viewModel Головна [MainViewModel].
 * @return Сконфігурований [NavHostController].
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalGetImage::class) 
@Composable
fun MyNavHost(drawerState: DrawerState, viewModel: MainViewModel): NavHostController {
    val navController = rememberNavController()
    val editViewModel: EditCodeViewModel = hiltViewModel()
    val settingsViewModel : SettingsViewModel = hiltViewModel()
    var paddingValues by remember { mutableStateOf(PaddingValues()) }
    val tabletViewModel: TabletViewModel = hiltViewModel()

    val sharedPref = (LocalContext.current as Activity)?.getPreferences(Context.MODE_PRIVATE)





    NavHost(navController = navController, startDestination = NavRoute.Scanner.route) {

        // Екран сканування штрих-коду камерою пристрою (з опціональним номером штрих-коду barnum)
        composable(
            route = NavRoute.BarCodeScanScreen.route + "?barnum={barnum}",
            arguments = listOf(navArgument("barnum") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            CameraView(
                navController = navController,
                editViewModel = editViewModel,
                barNum = backStackEntry.arguments?.getString("barnum"),
                values = paddingValues,
                navHostControllerMain = navController
            )
        }

        // Головний екран швидкого сканування та введення артикулів
        composable(NavRoute.Scanner.route) {
            val settingSwitchState = sharedPref?.getBoolean(stringResource(id = R.string.keyboard_state), true) ?: true

            // Якщо екранна клавіатура увімкнена в налаштуваннях:
            if (settingSwitchState)
                Scanner(
                    navHostController = navController,
                    drawerState = drawerState,
                    viewModel = viewModel,
                    editViewModel = editViewModel
                ) {
                    paddingValues = it
                }
            else {
                // Придушення екранної клавіатури IME для роботи із зовнішнім USB/BT-сканером
                CompositionLocalProvider(LocalTextInputService provides null) {
                    Scanner(
                        navHostController = navController,
                        drawerState = drawerState,
                        viewModel = viewModel,
                        editViewModel = editViewModel
                    ) {
                        paddingValues = it
                    }
                }
            }
        }

        // Екран керування Bluetooth-з'єднаннями всередині робочого простору
        composable(NavRoute.BtListScreen.route) {
            BtListScreenInApp(navController = navController, drawerState = drawerState, viewModel = viewModel)
        }

        // Головний планшетний екран: каталог товарів та формування фіскального чека
        composable(NavRoute.ReceiptProductListScreen.route) {
            val settingSwitchState = sharedPref?.getBoolean(stringResource(id = R.string.keyboard_state), true) ?: true

            if (settingSwitchState)
                ReceiptProductList(viewModel, navController, drawerState, editViewModel, settingsViewModel, tabletViewModel)
            else {
                CompositionLocalProvider(LocalTextInputService provides null) {
                    ReceiptProductList(viewModel, navController, drawerState, editViewModel, settingsViewModel, tabletViewModel)
                }
            }
        }

        // Екран створення / редагування картки товару (mode: "new" або "edit")
        composable(NavRoute.EditProductScreen.route + "/{mode}") { backStackEntry ->
            val settingSwitchState = sharedPref?.getBoolean(stringResource(id = R.string.keyboard_state), true) ?: true

            if (settingSwitchState)
                EditProductScreen(
                    navController = navController,
                    drawerState = drawerState,
                    viewModel = viewModel,
                    editViewModel = editViewModel,
                    settingsViewModel = settingsViewModel,
                    mode = backStackEntry.arguments?.getString("mode") ?: ""
                )
            else {
                CompositionLocalProvider(LocalTextInputService provides null) {
                    EditProductScreen(
                        navController = navController,
                        drawerState = drawerState,
                        viewModel = viewModel,
                        editViewModel = editViewModel,
                        settingsViewModel = settingsViewModel,
                        mode = backStackEntry.arguments?.getString("mode") ?: ""
                    )
                }
            }
        }

        // Головне меню системних налаштувань каси
        composable(NavRoute.Settings.route) {
            SettingsScreen(navController = navController, drawerState = drawerState)
        }

        // Налаштування видимості стовпчиків товарної таблиці на планшеті
        composable(NavRoute.SettingsVisible.route) {
            SettingsVisibleScreen(settingsViewModel)
        }

        // Апаратні налаштування РРО Datecs (час, шапка чека, X/Z звіти)
        composable(NavRoute.SettingsKasa.route) {
            KasaSettings(viewModel)
        }

        // Налаштування фіскальних податкових груп (А..К)
        composable(NavRoute.SettingsGroup.route) {
            GroupSettings(settingsViewModel)
        }

        // Довідник 10-значних кодів УКТЗЕД для підакцизних товарів
        composable(NavRoute.SettingsUktz.route) {
            UKTZSettings(settingsViewModel)
        }

        // Налаштування відеокамери для програмного сканера штрих-кодів
        composable(NavRoute.SettingsCam.route) {
            CameraSettings(navController)
        }

        // Панель адміністратора торговельної мережі
        composable(NavRoute.SettingAdmin.route) {
            AdminSettings(viewModel)
        }

        // Журнал історії збережених чеків та повторний друк копій
        composable(NavRoute.ReceiptScreen.route) {
            ReceiptScreen()
        }
    }

    return navController
}

