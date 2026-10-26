// =================================================================================================
// КОРЕНЕВИЙ ГРАФ НАВІГАЦІЇ ДОДАТКУ (ROOT LOGIN & DRAWER NAV HOST)
//
// Архітектурний шар: Presentation / Navigation Layer (Jetpack Compose).
// Призначення:
// Керує переходом між етапом автентифікації/первинного налаштування РРО (Login, Bluetooth pairing)
// та головним робочим простором касира (Drawer), що містить робочі екрани продажу та обліку.
// =================================================================================================

package com.nekorom.kasaapp.navigation

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.screens.BtListScreenForStart
import com.nekorom.kasaapp.screens.DeviceListScreen
import com.nekorom.kasaapp.screens.Login
import com.nekorom.kasaapp.utils.USER

/**
 * Маршрути кореневого рівня навігації.
 */
sealed class NavRouteLoginDrawer(val route: String) {
    // Екран входу касира / вибору профілю
    object Login: NavRoute("login_screen")
    // Головний робочий контейнер з бічним меню
    object Drawer: NavRoute("drawer")
    // Екран вибору знайдених пристроїв Bluetooth
    object DevListScreen: NavRoute("device_list_screen")
    // Екран первинного вибору касового апарата при першому запуску
    object BtListScreenForStart: NavRoute("bt_screen_for_start")
}

/**
 * Кореневий Composable [NavHost], що перемикає екран авторизації та основну частину програми.
 *
 * @param viewModel Головна [MainViewModel].
 * @param actListLauncher Лаунчер зовнішніх системних Activity.
 * @param onClearMac Колбек скидання збереженої MAC-адреси РРО.
 */
@Composable
fun LoginDrawerNavHost(
    viewModel: MainViewModel,
    actListLauncher: ActivityResultLauncher<Intent>,
    onClearMac: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoute.Login.route) {

        // Екран авторизації з необов'язковим аргументом mac-адреси обраного РРО
        composable(
            route = NavRouteLoginDrawer.Login.route + "?mac={mac}",
            arguments = listOf(navArgument("mac") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            Login(
                navController = navController,
                viewModel = viewModel,
                mac = backStackEntry.arguments?.getString("mac") ?: "",
                actListLauncher = actListLauncher
            )
        }

        // Головний робочий екран із Drawer
        composable(NavRouteLoginDrawer.Drawer.route) {
            Drawer(navController, viewModel, actListLauncher)
        }

        // Початковий екран вибору Bluetooth РРО
        composable(NavRouteLoginDrawer.BtListScreenForStart.route) {
            BtListScreenForStart(navController = navController, viewModel = viewModel)
        }

        // Список знайдених Bluetooth-пристроїв
        composable(NavRouteLoginDrawer.DevListScreen.route) {
            DeviceListScreen(navController = navController)
        }
    }
}
