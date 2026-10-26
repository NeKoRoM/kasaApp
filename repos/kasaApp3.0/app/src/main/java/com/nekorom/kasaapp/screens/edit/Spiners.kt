package com.nekorom.kasaapp.screens.edit

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.PopupProperties
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.nekorom.kasaapp.utils.Constants

/**
 * Випадаючий список вибору та введення коду УКТ ЗЕД (Українська класифікація товарів зовнішньоекономічної діяльності).
 *
 * Особливості:
 * - Дозволяє як пряме введення до 10 цифр ([KeyboardType.Decimal]), так і вибір зі списку збережених кодів ([UktzItem]).
 * - Містить швидкі кнопки переходу до налаштувань ([onClickSettings]) та пошуку ([onClickSearch]).
 *
 * @param OnChange Колбек зміни коду УКТ ЗЕД.
 * @param UKTZED Поточне значення коду.
 * @param options Список збережених позицій УКТ ЗЕД із Room.
 * @param onClickSettings Відкриття екрана налаштувань УКТ ЗЕД.
 * @param onClickSearch Відкриття розширеного пошуку.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun UKTZED(OnChange: (String) -> Unit, UKTZED : String,options: List<UktzItem>,onClickSettings:()->Unit,onClickSearch:()->Unit) {
    //val options = listOf("UKTZED", "Bill Payment", "Recharges", "Outing", "Other")
    var expanded by remember { mutableStateOf(false) }


    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val focusRequester = remember { FocusRequester() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
        },

    ) {
        TextField(

            modifier = Modifier.menuAnchor(),
            value = UKTZED,
            enabled = true,
            readOnly = false,
            onValueChange = { OnChange(it.replace(Regex("[^\\d]"), "").take(10)) },
            label = { Text(Constants.Keys.ARTICLE_UKTZ) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        DropdownMenu(
            modifier = Modifier.exposedDropdownSize(true),
            expanded = expanded,
            properties = PopupProperties(focusable = false),
            onDismissRequest = {
                expanded = false
            }
        ) {
            val filteringOptions by remember {
                mutableStateOf(options.filter { it.pdvValue.contains(UKTZED, ignoreCase = true) })
            }
            options.sortedBy { it.pdvValue }.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        OnChange(selectionOption.pdvValue.toString())
                        expanded = false


                    },
                    text = {
                        Text(text = selectionOption.toString())
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = {onClickSettings() }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
                Button(onClick = { onClickSearch() }) {
                    Icon(imageVector = Icons.Default.ManageSearch, contentDescription = null)
                }
            }
        }
    }


}

/**
 * Випадаючий список вибору податкової групи (ПДВ / акциз).
 *
 * Перелік ставок завантажується безпосередньо з фіскального накопичувача каси (групи А-К).
 *
 * @param onChange Колбек оновлення числового ID податкової ставки.
 * @param returnItem Колбек повернення повного об'єкта [PdvItem].
 * @param TAX Поточний ідентифікатор податкової групи.
 * @param options Доступні податкові ставки користувача.
 * @param onClickSearch Відкриття розширеного пошуку ставок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TAX(onChange: (String) -> Unit,returnItem:(PdvItem)->Unit , TAX : String, options: List<PdvItem>, onClickSearch:()->Unit) {
    //val options = listOf("TAX", "Bill Payment", "Recharges", "Outing", "Other")
    var expanded by remember { mutableStateOf(false) }

    Log.d("textField", options.getOrNull(options.indexOfFirst {it.pdvId.toString() == TAX  }).toString())

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = options.getOrNull(options.indexOfFirst {it.pdvId.toString() == TAX  }).toString(),
            onValueChange = {onChange(it) },

            label = { Text(Constants.Keys.ARTICLE_TAX) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()

        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        onChange(selectionOption.pdvId.toString())
                        returnItem(selectionOption)
                        expanded = false
                    },
                    text = {
                        Text(text = selectionOption.toString())
                    }
                )
            }
            Button(onClick = { onClickSearch() }) {
                Icon(imageVector = Icons.Default.ManageSearch, contentDescription = null)
            }
        }
    }
}

/**
 * Випадаючий список вибору товарної групи (відділу).
 *
 * @param OnChange Колбек зміни номера групи (до 2 цифр).
 * @param group Поточний номер групи.
 * @param options Налаштовані товарні групи [GroupItem].
 * @param onClickSettings Перехід до налаштувань груп.
 * @param onClickSearch Пошук по групах.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Group(OnChange: (String) -> Unit, group: String, options: List<GroupItem>,onClickSettings:()->Unit,onClickSearch:()->Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0].toString()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = false,
            value = group,
            onValueChange = { OnChange(it.replace(Regex("[^\\d]"), "").take(2)) },
            label = { Text(Constants.Keys.ARTICLE_GROUP) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        OnChange(selectionOption.groupValue)
                        expanded = false
                    },
                    text = {
                        Text(text = selectionOption.toString())
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = {onClickSettings() }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
                Button(onClick = { onClickSearch() }) {
                    Icon(imageVector = Icons.Default.ManageSearch, contentDescription = null)
                }
            }
        }
    }
}

/**
 * Випадаючий список одиниць виміру (Штуки, Кілограми, Метри, Літри, Грами).
 *
 * @param OnChange Колбек зміни індексу одиниці виміру ("0".."4").
 * @param unitOfMeasurementIndex Поточний числовий індекс одиниці виміру.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitOfMeasurement(OnChange: (String) -> Unit, unitOfMeasurementIndex: String) {
    val options = listOf("ШТУКИ", "КЛОГРАМИ", "МЕТРИ", "ЛІТРИ", "ГРАМИ")
    var expanded by remember { mutableStateOf(false) }
    var unitOfMeasurement by remember { mutableStateOf(options[unitOfMeasurementIndex.toIntOrNull()?:0]) }

    var selectedOptionText by remember { mutableStateOf(options[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = unitOfMeasurement,
            onValueChange = { },
            label = { Text(Constants.Keys.ARTICLE_UNIT_OF_MEASUREMENT) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.withIndex().forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        OnChange(selectionOption.index.toString())
                        unitOfMeasurement  = selectionOption.value
                        expanded = false
                    },
                    text = {
                        Text(text = selectionOption.value)
                    }
                )
            }
        }
    }
}

/**
 * Випадаючий список вибору типу ціноутворення для касового апарата:
 * 0: Програмована фіксована ціна.
 * 1: Вільна або програмована ціна (дозволяє касиру коригувати суму).
 * 2: Програмована або нижча ціна (дозволяє знижки).
 *
 * @param onChange Колбек зміни типу ціни.
 * @param type Поточний рядковий індекс типу ціни ("0", "1", "2").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Type(onChange: (String) -> Unit, type: String) {
    val options = listOf("ПРОГРАМОВАНА ЦІНА", "ВІЛЬНА АБО ПРОГРАМОВАНА ЦІНА", "ПРОГРАМОВАНА АБО НИЖЧАЦІНА")
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(options[type.toIntOrNull()?:0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = text,
            onValueChange = { },
            label = { Text(Constants.Keys.ARTICLE_TYPE) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.withIndex().forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        onChange(selectionOption.index.toString())
                        expanded = false
                        text = selectionOption.value
                    },
                    text = {
                        Text(text = selectionOption.value)
                    }
                )
            }
        }
    }


}
