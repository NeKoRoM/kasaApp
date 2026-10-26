package com.nekorom.kasaapp.screens

import android.view.View
import android.widget.ImageButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.nekorom.bt.ItemAdapter
import com.nekorom.bt.ListItem
import com.nekorom.kasaapp.btactivity.DeviceListFragment
import com.nekorom.bt.R
import com.nekorom.bt.databinding.BtListFragmentLayoutBinding
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.navigation.NavRouteLoginDrawer


/**
 * Екран вибору Bluetooth-пристрою при початковому запуску додатку (стартова реєстрація).
 *
 * Використовує інтеграцію Jetpack Compose з Android ViewBinding через [AndroidViewBinding]:
 * - Впроваджує XML-розмітку [BtListFragmentLayoutBinding] та фрагмент [DeviceListFragment].
 * - Підключає слухача [ItemAdapter.Listener] до адаптера списку спарених пристроїв.
 * - При кліку на MAC-адресу РРО [ItemAdapter.Listener.onClickMac] знищує фрагмент і повертається
 *   до вікна авторизації [NavRouteLoginDrawer.Login] із параметром обраного MAC `?mac=$mac`.
 *
 * @param navController Контролер навігації Jetpack Compose.
 * @param viewModel Головна [MainViewModel] додатку.
 */
@Composable
fun BtListScreenForStart(navController: NavController, viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()

    AndroidViewBinding(BtListFragmentLayoutBinding::inflate) {
        val fragment = fragmentContainerView.getFragment<DeviceListFragment>()
        val menuButton = this.root.findViewById<ImageButton>(R.id.menuButton)
        val rcPair = this.root.findViewById<RecyclerView>(R.id.rcViewPaired)

        val adapter = rcPair.adapter as ItemAdapter
        menuButton.visibility = View.GONE

        // Слухач вибору Bluetooth-пристрою з адаптера
        class BtListListener : ItemAdapter.Listener {
            override fun onClick(device: ListItem) {
                navController.navigate(NavRouteLoginDrawer.Login.route)
            }

            override fun onClickMac(mac: String) {
                fragment.onDestroy()
                navController.navigate(NavRouteLoginDrawer.Login.route + "?mac=$mac")
            }
        }

        adapter.addListener(BtListListener())
    }
}