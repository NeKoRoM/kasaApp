package com.nekorom.kasaapp.screens.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.keys
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.utils.Constants
import kotlinx.coroutines.launch

/**
 * Головний екран налаштувань додатку (Main Settings Hub).
 *
 * Надає навігаційні переходи до всіх розділів конфігурації:
 * 1. [NavRoute.SettingsVisible] — Видимість полів форми товару ([Constants.Keys.ACTIVE_FIELD]).
 * 2. [NavRoute.SettingsKasa] — Параметри підключення та команд РРО ([Constants.Keys.KASA_SETTINGS]).
 * 3. [NavRoute.SettingsGroup] — Налаштування товарних груп та підакцизності ([Constants.Keys.GROUP_SETTINGS]).
 * 4. [NavRoute.SettingsUktz] — Довідник кодів УКТ ЗЕД ([Constants.Keys.UKTZ_SETTINGS]).
 * 5. [NavRoute.SettingsCam] — Вибір камери для сканування штрихкодів (передня / задня).
 * 6. [NavRoute.SettingAdmin] — Адміністративна панель керування акаунтами та ліцензіями.
 *
 * @param navController Контролер навігації Jetpack Compose.
 * @param drawerState Стан бічного навігаційного меню (Drawer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()
    
Scaffold(topBar = {
    TopAppBar(title = { Text(text = "Settings") }, navigationIcon = {
        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = null)
        }
    })
}) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .padding(it)
            .fillMaxWidth()
            .scrollable(scroll,Orientation.Vertical),

        horizontalAlignment = BiasAlignment.Horizontal(0f)) {
        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = {
            navController.navigate(NavRoute.SettingsVisible.route)

        }) {
            Text(text = Constants.Keys.ACTIVE_FIELD)
        }

        Button(onClick = {
            navController.navigate(NavRoute.SettingsKasa.route)

        }) {
            Text(text = Constants.Keys.KASA_SETTINGS)
        }
        Button(onClick = {
            navController.navigate(NavRoute.SettingsGroup.route)

        }) {
            Text(text = Constants.Keys.GROUP_SETTINGS)
        }
        Button(onClick = {
            navController.navigate(NavRoute.SettingsUktz.route)

        }) {
            Text(text = Constants.Keys.UKTZ_SETTINGS)
        }
        Button(onClick = {
            navController.navigate(NavRoute.SettingsCam.route)

        }) {
            Text(text = stringResource(R.string.camera_Settings))
        }
        Button(onClick = {
            navController.navigate(NavRoute.SettingAdmin.route)

        }) {
            Text(text = "ADMIN")
        }
            
    }

}
}