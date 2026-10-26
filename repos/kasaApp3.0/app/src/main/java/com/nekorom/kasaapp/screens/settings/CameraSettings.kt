package com.nekorom.kasaapp.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.screens.settings.CamKey.BACK_CAM
import com.nekorom.kasaapp.screens.settings.CamKey.FRONT_CAM

/**
 * Екран налаштування активної камери для оптичного сканування штрихкодів (Camera Selector Screen).
 *
 * Дозволяє перемикатися між:
 * - Передньою камерою (зручно для стаціонарного планшета-підставки, коли клієнт сам підносить товар або картку).
 * - Задньою камерою (зручно для мобільних телефонів та ручних терміналів).
 *
 * Зберігає вибір у [SharedPreferences] за ключем [R.string.SHASED_PRREF_camera_mode].
 *
 * @param navController Навігаційний контролер додатку.
 */
@Composable
fun CameraSettings(navController: NavHostController) {
    val sharedPref = LocalContext.current.getSharedPreferences(stringResource(R.string.SHASED_PRREF_camera_mode),Context.MODE_PRIVATE)
    val nowChose = sharedPref.getString(stringResource(id = R.string.SHASED_PRREF_camera_mode),"")
    var frontCheck by remember{
        mutableStateOf(nowChose== FRONT_CAM)
    }
    var backCheck by remember{
        mutableStateOf(nowChose!= FRONT_CAM)
    }


    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = frontCheck, onCheckedChange ={
                    frontCheck = it
                    backCheck =!it
                    navController.navigateUp()

                } )
                Text(text = "Передня камера")

            }
            Spacer(modifier = Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = backCheck, onCheckedChange ={
                    frontCheck = !it
                    backCheck =it
                    navController.navigateUp()

                } ,)
                Text(text = "Задня камера")

            }
        }


    }
    if (frontCheck)
        sharedPref.edit().putString(stringResource(id = R.string.SHASED_PRREF_camera_mode),FRONT_CAM).apply()
if (backCheck)
    sharedPref.edit().putString(stringResource(id = R.string.SHASED_PRREF_camera_mode), BACK_CAM).apply()




}
/**
 * Константи режимів камери для сканування.
 */
object CamKey {
    /** Передня камера пристрою */
    const val FRONT_CAM = "FRONT_CAM"
    /** Задня (основна) камера пристрою */
    const val BACK_CAM = "BACK_CAM"
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun camSPrew() {
   // CameraSettings(navController)
}