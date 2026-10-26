// =================================================================================================
// ВКЛАДЕНИЙ ГРАФ НАВІГАЦІЇ НИЖНЬОЇ ПАНЕЛІ СКАНЕРА (SCANNER BOTTOM BAR NAV HOST)
//
// Архітектурний шар: Presentation / Navigation Layer (Jetpack Compose).
// Призначення:
// Керує вкладеним перемиканням контенту нижньої панелі екрана швидкого сканування:
// 1. ListScreenIn: Список відсканованих товарів, підготовлених до передачі в чек або РРО (ListForSendScreen).
// 2. CameraScreenIn: Режим неперервного потокового розпізнавання штрих-кодів через камеру (BarCodeScreen).
// 3. CameraScreenInOne: Одноразове сканування штрих-коду для прив'язки до конкретної позиції.
// =================================================================================================

package com.nekorom.kasaapp.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.scan.BarCodeScreen
import com.nekorom.kasaapp.screens.scan.ListForSendScreen

/**
 * Маршрути внутрішньої навігації екрана сканера.
 */
sealed class BottomNavRoute(val route: String) {
    // Список товарів черги на відправку
    object ListScreenIn : NavRoute("list_screen_in")
    // Потокове розпізнавання камери
    object CameraScreenIn : NavRoute("camera_screen_in")
    // Одиночне розпізнавання для заповнення штрих-коду
    object CameraScreenInOne : NavRoute("camera_screen_in_one")
}

/**
 * Вкладений хост навігації [NavHostBottomBar] для панелі операцій сканування.
 *
 * @param values Системні відступи Scaffold.
 * @param viewModel Головна [MainViewModel].
 * @param editViewModel [EditCodeViewModel] для роботи з карткою товару.
 * @param navHostControllerMain Головний зовнішній [NavHostController].
 * @return Локальний [NavHostController] нижньої панелі.
 */
@Composable
fun NavHostBottomBar(
    values: PaddingValues,
    viewModel: MainViewModel,
    editViewModel: EditCodeViewModel,
    navHostControllerMain: NavHostController
): NavHostController {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = BottomNavRoute.ListScreenIn.route) {
        // Екран списку товарів у черзі на відправку
        composable(BottomNavRoute.ListScreenIn.route) {
            ListForSendScreen(
                values = values,
                viewModel = viewModel,
                editViewModel = editViewModel,
                navHostController = navHostControllerMain,
                inNavController = navController
            )
        }

        // Потоковий сканер камери
        composable(BottomNavRoute.CameraScreenIn.route) {
            BarCodeScreen(
                values = values,
                viewModel = viewModel,
                navHostControllerMain = navHostControllerMain,
                inNavController = navController,
                code = null
            )
        }

        // Одиночний сканер для прив'язки коду товару
        composable(BottomNavRoute.CameraScreenInOne.route) {
            BarCodeScreen(
                values = values,
                viewModel = viewModel,
                navHostControllerMain = navHostControllerMain,
                inNavController = navController,
                code = "101"
            )
        }
    }

    return navController
}