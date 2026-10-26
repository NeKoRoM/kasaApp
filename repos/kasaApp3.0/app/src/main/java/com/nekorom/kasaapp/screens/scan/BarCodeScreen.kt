package com.nekorom.kasaapp.screens.scan

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel

/**
 * Екран-обгортка для модуля сканування штрихкодів у нижній навігації (Barcode Screen Destination).
 *
 * Передає контекст [MainViewModel], параметри відступів [values] та контролери навігації
 * до базового компонента [CameraView] у режимі поточного продажу ([barNum] = "101" або інший).
 *
 * @param values Системні відступи [PaddingValues] від Scaffold.
 * @param viewModel Головна [MainViewModel] касового вузла.
 * @param navHostControllerMain Головний навігаційний контролер додатку.
 * @param inNavController Внутрішній контролер нижньої навігації вкладок.
 * @param barNum Код цільового призначення сканованого ШК.
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalGetImage::class)
@Composable
fun BarCodeScreen(
    values: PaddingValues,
    viewModel: MainViewModel,
    navHostControllerMain: NavHostController,
    inNavController: NavHostController,
    barNum: String?
) {
    CameraView(
        navController = null,
        editCodeViewModel = null,
        barNum = barNum,
        viewModel = viewModel,
        values = values,
        navHostControllerMain = navHostControllerMain,
        inNavController = inNavController
    )
}
