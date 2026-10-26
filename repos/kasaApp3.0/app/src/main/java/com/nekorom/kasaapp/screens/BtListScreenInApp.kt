package com.nekorom.kasaapp.screens

import android.widget.ImageButton
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.nekorom.bt.ItemAdapter
import com.nekorom.bt.ListItem
import com.nekorom.bt.databinding.BtListFragmentLayoutBinding
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.navigation.NavRoute
import kotlinx.coroutines.launch

/**
 * Екран керування Bluetooth-підключенням усередині робочого простору додатку (In-App Bluetooth Manager).
 *
 * Дозволяє перепідключитися до іншого РРО або ваг без виходу з облікового запису касира:
 * - Включає кнопку відкриття бічного меню ([DrawerState.open]).
 * - Вбудовує XML-розмітку [BtListFragmentLayoutBinding] зі списком спарених та доступних пристроїв.
 * - При виборі MAC-адреси [ItemAdapter.Listener.onClickMac] викликає [MainViewModel.btConnect]
 *   та автоматично перемикає екран на сканер чеків [NavRoute.Scanner].
 *
 * @param navController Контролер навігації Jetpack Compose.
 * @param drawerState Стан бічної панелі навігації (Drawer).
 * @param viewModel Головна [MainViewModel] додатку.
 */
@Composable
fun BtListScreenInApp(navController: NavController, drawerState: DrawerState, viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    AndroidViewBinding(BtListFragmentLayoutBinding::inflate) {
        val menuButton = this.root.findViewById<ImageButton>(com.nekorom.bt.R.id.menuButton)
        val rcPair = this.root.findViewById<RecyclerView>(com.nekorom.bt.R.id.rcViewPaired)

        val adapter = rcPair.adapter as ItemAdapter
        menuButton.setOnClickListener {
            coroutineScope.launch {
                drawerState.open()
            }
        }

        // Слухач вибору пристрою для підключення "на льоту"
        class BtListListener : ItemAdapter.Listener {
            override fun onClick(device: ListItem) {
                navController.navigate(NavRoute.Scanner.route)
            }

            override fun onClickMac(mac: String) {
                viewModel.btConnect(mac = mac)
                navController.navigate(NavRoute.Scanner.route)
            }
        }
        adapter.addListener(BtListListener())
    }
}