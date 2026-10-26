package com.nekorom.kasaapp.screens.settings

import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.exellio20.roomDB.entities.GroupItem
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.USER

/**
 * Екран налаштування товарних груп та контролю підакцизності (Commodity Groups & Excise Settings).
 *
 * Призначений для:
 * 1. Створення та редагування товарних груп (відділів РРО).
 * 2. Встановлення ознаки [GroupItem.groupExcise] ("КОНТРОЛЬ АКЦИЗНИХ МАРОК"):
 *    - Якщо прапорець встановлено, додавання будь-якого товару цієї групи у чек обов'язково
 *      вимагатиме сканування або ручного введення унікальної державної акцизної марки.
 * 3. Перегляду та видалення існуючих груп із локальної бази Room.
 *
 * @param settingsViewModel ViewModel налаштувань.
 */
@Composable
fun GroupSettings(settingsViewModel: SettingsViewModel) {
    val groupList = settingsViewModel.getAllGroup(USER?.userId ?: 0).observeAsState().value

    var groupName by remember {
        mutableStateOf("")
    }
    var groupValue by remember {
        mutableStateOf("")
    }
    var groupExcise by remember {
        mutableStateOf(false)
    }
    var groupId by remember {
        mutableStateOf(0)
    }
    Scaffold {
        Column(modifier = Modifier
            .padding(it)
            .fillMaxWidth(), horizontalAlignment = BiasAlignment.Horizontal(0f) ) {
            TextField(value = groupName, onValueChange ={groupName = it.uppercase().take(25)}, label ={ Text(text = Constants.Keys.GROUP_NAME)},
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Characters))
            Spacer(modifier = Modifier.size(20.dp))
            TextField(value = groupValue, onValueChange ={groupValue = it.replace(Regex("[^\\d]"), "").take(2)}, label ={ Text(text = Constants.Keys.GROUP_VALUE )},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(modifier = Modifier.size(20.dp))
            Checkbox(checked =groupExcise , onCheckedChange ={groupExcise = it} )
            Text(text = "КОНТРОЛЬ АКЦИЗНИХ МАРОК")
            Spacer(modifier = Modifier.size(20.dp))
            Button(onClick = { settingsViewModel.insertGroup(GroupItem(id = groupId,groupValue = groupValue, groupName = groupName, groupExcise = groupExcise, userId = USER?.userId?:0))
                    groupId= 0
                groupValue = ""
                groupExcise = false
                groupName = ""
            }) {
                Text(text = "записати")

            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .size(4.dp)
                .background(Color(R.color.teal_700)))

            LazyColumn{
                groupList?.let {


                    items(groupList){
                        Spacer(modifier = Modifier.size(20.dp))
                        GroupCard(it,{ group ->
                            groupName = group.groupName
                            groupValue = group.groupValue
                            groupExcise = group.groupExcise
                            groupId = group.id

                        },{
                            settingsViewModel.deleteGroup(it.id)
                        }
                        )
                    }
                }

            }





        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CardPrevGroup() {
    val settingsViewModel : SettingsViewModel = hiltViewModel()


        //GroupSettings(settingsViewModel)
        GroupCard(GroupItem(groupName = "алкогольgggggg", groupValue = "99", groupExcise = true),{},{})




}

/**
 * Картка товарної групи в списку довідника.
 *
 * @param group Об'єкт групи [GroupItem].
 * @param onClick Колбек вибору групи для редагування в формі вище.
 * @param onClickDelete Колбек видалення групи з бази Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(group: GroupItem, onClick:(GroupItem)->Unit,onClickDelete:(GroupItem)->Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Card(modifier = Modifier.fillMaxWidth(0.9f), onClick = { onClick(group) },){
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column( horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.weight(0.8f)
                ) {

                    Row{
                        Text(text = "назва групи:  ")
                        Text(text = group.groupName)
                    }

                    Row {
                        Text(text = "номер групи:  ")
                        Text(text = group.groupValue)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (group.groupExcise)
                            Text(text = "акцизна")
                        else
                            Text(text = "не акцизна")
                        Checkbox(enabled = false,checked = group.groupExcise, onCheckedChange = {})
                    }

                }
                IconButton( modifier = Modifier.weight(0.2f), onClick = { onClickDelete(group) }) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "",
                        tint =Color.Red
                    )
                }

            }
            }

    }

}