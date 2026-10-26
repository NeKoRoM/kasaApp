package com.nekorom.kasaapp.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.USER

/**
 * Екран керування довідником кодів УКТ ЗЕД (UKTZED Customs Classification Settings).
 *
 * Призначений для:
 * 1. Введення та збереження офіційних 10-значних кодів УКТ ЗЕД із текстовою назвою ([UktzItem]).
 * 2. Використовується для обов'язкового фіскального кодування підакцизних та імпортних товарів згідно з вимогами законодавства України.
 * 3. Редагування та видалення кодів із локальної бази Room.
 *
 * @param viewModel ViewModel налаштувань.
 */
@Composable
fun UKTZSettings(viewModel: SettingsViewModel ) {

    val uktzList = viewModel.getAllUktz(USER?.userId?:0).observeAsState().value?.sortedBy { it.pdvValue.toLongOrNull()?:1 }

    var uktzId by remember {
        mutableStateOf(0)
    }
    var uktzName by remember {
        mutableStateOf("")
    }
    var uktzValue by remember {
        mutableStateOf("")
    }
    Scaffold { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxWidth(), horizontalAlignment = BiasAlignment.Horizontal(0f) ) {
            TextField( value = uktzName, onValueChange ={uktzName = it.uppercase().take(25)}, label ={ Text(text = Constants.Keys.UKTZ_NAME)},
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Characters))
            Spacer(modifier = Modifier.size(20.dp))
            TextField(value = uktzValue,
                onValueChange ={uktzValue = it.replace(Regex("[^\\d]"), "").take(10)},
                label ={ Text(text = Constants.Keys.UKTZ_VALUE)},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.size(20.dp))
            Button(onClick = {
                viewModel.insertUktz(
                    UktzItem(id = uktzId,
                    pdvName = uktzName,
                    pdvValue = uktzValue,
                        userId = USER?.userId?:0)
                )
                uktzId = 0
                uktzName = ""
                uktzValue = ""

            }) {
                Text(text = "записати")
                
            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .size(4.dp)
                .background(Color(R.color.teal_700)))

            LazyColumn{
                uktzList?.let {
                    items(uktzList){ it ->
                        Spacer(modifier = Modifier.size(20.dp))

                        UktzCard(uktz = it,{item->
                            uktzId = item.id
                            uktzName = item.pdvName
                            uktzValue  = item.pdvValue
                        },
                            {
                                viewModel.deleteUktz(it.id)
                            })
                    }
                }

            }





        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CardPrevUktz() {
    //GroupCard(GroupItem(groupName = "алкоголь", groupValue = "99", groupExcise = true)){


}

/**
 * Картка коду УКТ ЗЕД у списку довідника.
 *
 * @param uktz Елемент довідника [UktzItem].
 * @param onClick Вибір для редагування.
 * @param onClickDelete Видалення запису з бази Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UktzCard(uktz: UktzItem, onClick:(UktzItem)->Unit, onClickDelete:(UktzItem)->Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Card(modifier = Modifier.fillMaxWidth(0.9f), onClick = { onClick(uktz) },){
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {

                    Row {
                        Text(text = "назва УКТЗ:  ")
                        Text(text = uktz.pdvName)
                    }

                    Row {
                        Text(text = "номер УКТЗ:  ")
                        Text(text = uktz.pdvValue)
                    }


                }
                IconButton(modifier = Modifier
                    .padding(end = 10.dp)
                    .weight(0.2f), onClick = { onClickDelete(uktz) }) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "",
                        tint =Color.Red
                    )
                }


            }
        }
    }

}