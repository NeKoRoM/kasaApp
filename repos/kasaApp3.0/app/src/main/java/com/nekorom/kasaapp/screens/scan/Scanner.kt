package com.nekorom.kasaapp.screens.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.navigation.BottomNavRoute
import com.nekorom.kasaapp.navigation.NavHostBottomBar
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import kotlinx.coroutines.launch

/**
 * Екран-контейнер касового сканування (Scanner POS Screen).
 *
 * Організує інтерфейс із нижньою панеллю вкладок ([TabRow]):
 * 1. Вкладка 0 — "сканер" ([BottomNavRoute.CameraScreenIn]): сканування штрихкодів камерою або введення з клавіатури.
 * 2. Вкладка 1 — "список" ([BottomNavRoute.ListScreenIn]): список товарів поточного чека для відправки в РРО.
 *
 * Також містить кнопку-гамбургер для виклику бокового навігаційного меню ([drawerState.open])
 * та керує вкладеним підграфом навігації [NavHostBottomBar].
 *
 * @param drawerState Стан бічного навігаційного меню.
 * @param viewModel Головна [MainViewModel] касового модуля.
 * @param editViewModel [EditCodeViewModel] для редагування параметрів номенклатури.
 * @param navHostController Кореневий навігаційний контролер додатку.
 * @param onPaddingChange Колбек для передачі системних відступів (Padding) зовнішньому лейауту.
 */
@Composable
fun Scanner(
    drawerState: DrawerState,
    viewModel: MainViewModel,
    editViewModel: EditCodeViewModel,
    navHostController: NavHostController,
    onPaddingChange:(PaddingValues)->Unit
) {
    val coroutineScope = rememberCoroutineScope()


    //Drawer()
    var state by remember { mutableStateOf(1) }
    Scaffold(modifier = Modifier
        .fillMaxSize(),
        bottomBar = {

            val titles = listOf("сканер", "список")

            Row() {
                IconButton(modifier = Modifier.align(Alignment.Bottom), onClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }


                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Go back"
                    )
                }


                Column(Modifier.padding()) {


                    TabRow(selectedTabIndex = state) {

                        titles.forEachIndexed { index, title ->

                            Tab(

                                selected = state == index,
                                unselectedContentColor = Color.Gray,
                                onClick = { state = index },
                                text = {
                                    if (state == index) {
                                        Text(
                                            //modifier = Modifier.fillMaxSize(),
                                            fontWeight = FontWeight.ExtraBold,
                                            text = title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            )
                        }
                    }

                }
            }


        }

    ) { values ->
        val navController = NavHostBottomBar(values = values, viewModel = viewModel, editViewModel = editViewModel, navHostControllerMain = navHostController)

        onPaddingChange(values)
        when (state) {
            0 -> {
                if (navController.currentDestination?.route != BottomNavRoute.CameraScreenIn.route)
                navController.navigate(BottomNavRoute.CameraScreenIn.route){
                    popUpTo(navController.graph.id){
                        inclusive = true
                    }
                }
                //BarCodeScreen(values = values, viewModel, navHostController = navHostController,inNavController = navController )
            }

            1 -> {
                if (navController.currentDestination?.route != BottomNavRoute.ListScreenIn.route)
                    navController.navigate(BottomNavRoute.ListScreenIn.route){
                    popUpTo(navController.graph.id){
                        inclusive = true
                    }
                }
            }

        }




    }

}
