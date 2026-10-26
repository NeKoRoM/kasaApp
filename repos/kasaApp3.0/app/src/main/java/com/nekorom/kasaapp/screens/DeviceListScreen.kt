package com.nekorom.kasaapp.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.view.View
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.nekorom.bt.BluetoothConstants
import com.nekorom.bt.ListItem
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.navigation.NavRouteLoginDrawer

/**
 * Екран вибору спареного Bluetooth-пристрою (Compose-альтернатива спискам пристроїв).
 *
 * Отримує набір спарених пристроїв Android ([BluetoothAdapter.getBondedDevices])
 * та відображає їх у вигляді списку кнопок.
 * При виборі пристрою повертає його MAC-адресу в маршрут авторизації [NavRouteLoginDrawer.Login].
 *
 * @param navController Контролер навігації додатку.
 */
@SuppressLint("MissingPermission")
@Composable
fun DeviceListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val list = ArrayList<ListItem>()
    val bManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val btAdapter = bManager.adapter
    val deviceList = btAdapter.bondedDevices as Set<BluetoothDevice>

    LazyColumn {
        items(deviceList.toList()) {
            Button(onClick = {
                navController.navigate(NavRouteLoginDrawer.Login.route + "?mac=${it.address}")
            }) {
                Text(text = it.name)
            }
        }
    }
}

/**
 * Допоміжна функція отримання списку спарених пристроїв.
 */
private fun getPairedDevices(bAdapter: BluetoothAdapter) {
}