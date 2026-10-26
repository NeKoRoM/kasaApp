package com.nekorom.kasaapp.screens.tablet

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.navigation.BottomNavRoute
import com.nekorom.kasaapp.navigation.NavRoute

/**
 * Швидке меню налаштувань терміналу (Terminal Settings FAB & Modal Dialog).
 *
 * Основна функція:
 * - Керування перемикачем "вимкнути клавіатуру" ([R.string.keyboard_state] у [SharedPreferences]).
 * - При відключенні програмної клавіатури система блокує виклик екранного вводу (IME),
 *   дозволяючи апаратним сканерам штрихкодів вводити дані напряму без перекриття інтерфейсу каси.
 * - При закритті діалогу зберігає налаштування та оновлює цільовий екран [navDestination].
 *
 * @param navController Контролер навігації Jetpack Compose.
 * @param navDestination Цільовий маршрут навігації для оновлення стану після зміни налаштувань.
 */
@Composable
fun SettingsMenu(
    navController: NavHostController,
    navDestination: String
) {

    var menuState by remember {
        mutableStateOf(false)
    }

    val modifierForMenu = Modifier.background(Color(R.color.teal_200), CircleShape)
    val context = LocalContext.current

    val sharedPref =
        (LocalContext.current as Activity)?.getPreferences(Context.MODE_PRIVATE) ?: return
    val settingSwitchState =
        sharedPref.getBoolean(stringResource(id = R.string.keyboard_state), true)


    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        //horizontalAlignment = BiasAlignment.Horizontal(0.5f)
    ) {
        IconButton(modifier = Modifier
            .background(Color(R.color.teal_700), CircleShape)
            .border(1.dp, Color.Green, CircleShape)
            .size(50.dp),
            onClick = { menuState = !menuState })
        {
            if (menuState) {
                Icon(Icons.Default.Settings, contentDescription = null)
            } else
                Icon(Icons.TwoTone.Settings, contentDescription = "menu")


        }
        Spacer(modifier = Modifier.size(25.dp))

        if (menuState) {

            Dialog(onDismissRequest = { menuState = false }) {
                var switchState by remember {
                    mutableStateOf(settingSwitchState)
                }
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Text(text = "вимкнути клавіатуру")
                    Switch(checked = switchState, onCheckedChange = {
                        switchState = it

                    })
                    DisposableEffect(switchState) {
                        onDispose {
                            navController.navigate(navDestination){
                                popUpTo(NavRoute.Scanner.route)
                            }
                            with(sharedPref.edit()) {
                                putBoolean(getString(context, R.string.keyboard_state), switchState)
                                apply()
                            }
                        }


                    }
            }


            }


        }


    }
}