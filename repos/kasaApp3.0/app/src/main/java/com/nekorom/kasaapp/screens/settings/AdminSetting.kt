
package com.nekorom.kasaapp.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nekorom.kasaapp.MainViewModel

/**
 * Інженерний термінал низькорівневої діагностики та відладки протоколу Datecs (Admin Protocol Console).
 *
 * Призначений для сервісних інженерів та розробників:
 * 1. Дозволяє формувати та надсилати довільні текстові кадри команд прямо в Bluetooth-канал каси ([mainViewModel.kasaSendMess]).
 * 2. Містить кнопку швидкої вставки розділювача `[tab]` (`\t`), оскільки протокол Datecs використовує табуляцію для розділення полів.
 * 3. Відображає реверсивний журнал отриманих відповідей у реальному часі через спостереження за [BtKasaExellioRepository.lastGetAns].
 *
 * @param mainViewModel Головна [MainViewModel] для доступу до Bluetooth-репозиторію.
 */
@Composable
fun AdminSettings(mainViewModel: MainViewModel) {
    var list by remember {
        mutableStateOf(listOf(""))
    }
    mainViewModel.btKasaExellioRepository.lastGetAns.observe(LocalLifecycleOwner.current){
       list = list.plus(it)
    }
    Column {
        var text by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = text,
            onValueChange = {
                text = TextFieldValue(it.text.replace("[\\t]","\t"), selection = TextRange(it.text.replace("[\\t]","\t").length))
            }
        )
        Row {
            Button(onClick = { mainViewModel.kasaSendMess(text.text) }) {
                Text(text = "відправити")
            }
            Button(onClick = { text = TextFieldValue(text.text + "\t", selection = TextRange((text.text + "\t").length)) }) {
                Text(text = "[tab]")
            }
        }

        // Журнал відповідей касового апарата Datecs
        LazyColumn(reverseLayout = true) {
            items(list) {
                Text(text = it)
            }
        }
    }
}