package com.nekorom.kasaapp.screens.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.utils.USER

/**
 * Плаваюче швидке меню дій над номенклатурою (Floating Action Menu).
 *
 * Надає швидкий доступ до:
 * 1. Кнопки розгортання/згортання меню (іконка плюса/хрестика).
 * 2. Створення нового товару з дефолтними параметрами фіскалізації ([NavRoute.EditProductScreen] `/new`).
 * 3. Повного вичитування бази товарів із РРО через Bluetooth ([MainViewModel.readKasaDB]).
 *
 * @param menuState Прапорець розгорнутого стану меню.
 * @param values Системні відступи [PaddingValues].
 * @param viewModel Головна [MainViewModel].
 * @param editViewModel ViewModel редагування номенклатури.
 * @param navHostController Навігаційний контролер додатку.
 * @param onStateChange Колбек зміни стану розкриття меню.
 */
@Composable
fun MenuForList(
    menuState: Boolean,
    values: PaddingValues,
    viewModel: MainViewModel,
    editViewModel: EditCodeViewModel,
    navHostController: NavHostController,
    onStateChange: (Boolean) -> Unit
) {
    val modifierForMenu = Modifier.background(Color(R.color.teal_200), CircleShape)
    val btRequestState = viewModel.btRequestState.observeAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(values),
        verticalArrangement = Arrangement.Top,
        //horizontalAlignment = BiasAlignment.Horizontal(0.9f)
    )

    {
        IconButton(modifier = Modifier
            .background(Color(R.color.teal_700), CircleShape)
            .border(1.dp, Color.Green, CircleShape)
            .size(50.dp),
            onClick = { onStateChange(!menuState) })
        {
            if (menuState) {
                Icon(Icons.Default.Close, contentDescription = null)
            } else
                Icon(Icons.Default.Add, contentDescription = "menu")


        }
        Spacer(modifier = Modifier.size(25.dp))
        if (menuState && btRequestState.value == false) {


//            IconButton(modifier = modifierForMenu,
//                onClick = { onStateChange(!menuState) }) {
//                Icon(Icons.Default.Info, contentDescription = null)
//            }
//            Spacer(modifier = Modifier.size(15.dp))
//
//            IconButton(modifier = modifierForMenu,
//                onClick = { navHostController.navigate(NavRoute.BarCodeScanScreen.route + "?barnum=51") }) {
//                Icon(
//                    painter = painterResource(id = R.drawable.barcode_search),
//                    contentDescription = null
//                )
//            }
//            Spacer(modifier = Modifier.size(15.dp))
            IconButton(modifier = modifierForMenu,
                onClick = {
                    editViewModel.product = ProductEntity(
                        price = "0.00",
                        TAX = "1",
                        group = "1",
                        unitOfMeasurement = "0",
                        weightOrUnit = "0",
                        barCode1 = "0",
                        barCode2 = "0",
                        barCode3 = "0",
                        barCode4 = "0",
                        remainder = "0.000",
                        type = "0",
                        section = "1"
                    )
                    navHostController.navigate(NavRoute.EditProductScreen.route + "/new")
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.resource_new),
                    contentDescription = null
                )

            }
            Spacer(modifier = Modifier.size(15.dp))
            IconButton(modifier = modifierForMenu,
                onClick = {
                    viewModel.readKasaDB(USER ?: UserEntity())
                }) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
            }

        }
        Spacer(modifier = Modifier.size(20.dp))


    }
}