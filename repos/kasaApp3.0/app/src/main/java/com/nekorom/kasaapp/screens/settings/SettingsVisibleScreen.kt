package com.nekorom.kasaapp.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_GROUP
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_NAME
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_PRICE
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_TAX
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_TYPE
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_UKTZ
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_UNIT_OF_MEASUREMENT
import com.nekorom.kasaapp.utils.Constants.Keys.BAR_CODE1
import com.nekorom.kasaapp.utils.Constants.Keys.BAR_CODE2
import com.nekorom.kasaapp.utils.Constants.Keys.BAR_CODE3
import com.nekorom.kasaapp.utils.Constants.Keys.BAR_CODE4
import com.nekorom.kasaapp.utils.Constants.Keys.FRACTION_AMOUNT
import com.nekorom.kasaapp.utils.Constants.Keys.REMAINDER
import com.nekorom.kasaapp.utils.Constants.Keys.SALES_AMOUNT
import com.nekorom.kasaapp.utils.Constants.Keys.SALES_COUNT
import com.nekorom.kasaapp.utils.Constants.Keys.SECTION

/**
 * Екран налаштування видимості полів картки товару (Field Visibility Settings Screen).
 *
 * Дозволяє користувачу налаштувати форму редагування товарів під власні потреби:
 * - Приховувати непотрібні поля (наприклад, акцизні коди або додаткові штрихкоди),
 *   щоб спростити роботу касира та прискорити набір номенклатури.
 * - Усі прапорці зберігаються в єдиній Room-сутності [ProductEnabledEntity].
 *
 * @param viewModel ViewModel налаштувань.
 */
@Composable
fun SettingsVisibleScreen(viewModel: SettingsViewModel) {
    var fieldsVisibility = viewModel.getVisibleFields().observeAsState().value?.getOrNull(0) ?: ProductEnabledEntity(1)

    Scaffold {
        Column(modifier = Modifier
            .padding(it)
            .verticalScroll(rememberScrollState())) {


            OneProductActive(ARTICLE_NAME,fieldsVisibility.name){
                fieldsVisibility= fieldsVisibility.copy(name = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_PRICE,fieldsVisibility.price){
                fieldsVisibility = fieldsVisibility.copy(price = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_UKTZ,fieldsVisibility.UKTZED){
                fieldsVisibility = fieldsVisibility.copy(UKTZED = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_TAX,fieldsVisibility.TAX){
                fieldsVisibility = fieldsVisibility.copy(TAX = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_GROUP,fieldsVisibility.group){
                fieldsVisibility = fieldsVisibility.copy(group = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_UNIT_OF_MEASUREMENT,fieldsVisibility.unitOfMeasurement){
                fieldsVisibility = fieldsVisibility.copy(unitOfMeasurement = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(FRACTION_AMOUNT,fieldsVisibility.weightOrUnit){
                fieldsVisibility = fieldsVisibility.copy(weightOrUnit = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }

            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(BAR_CODE1,fieldsVisibility.barCode1){
                fieldsVisibility = fieldsVisibility.copy(barCode1 = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(BAR_CODE2,fieldsVisibility.barCode2){
                fieldsVisibility = fieldsVisibility.copy(barCode2 = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(BAR_CODE3,fieldsVisibility.barCode3){
                fieldsVisibility = fieldsVisibility.copy(barCode3 = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(BAR_CODE4,fieldsVisibility.barCode4){
                fieldsVisibility = fieldsVisibility.copy(barCode4 = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(REMAINDER,fieldsVisibility.remainder){
                fieldsVisibility = fieldsVisibility.copy(remainder = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(ARTICLE_TYPE,fieldsVisibility.type){
                fieldsVisibility = fieldsVisibility.copy(type = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(SECTION,fieldsVisibility.section){
                fieldsVisibility = fieldsVisibility.copy(section = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(SALES_AMOUNT,fieldsVisibility.salesAmount){
                fieldsVisibility = fieldsVisibility.copy(salesAmount = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))
            OneProductActive(SALES_COUNT,fieldsVisibility.salesCount){
                fieldsVisibility = fieldsVisibility.copy(salesCount = it)
viewModel.setVisibilityFields(fieldsVisibility)
            }
            Spacer(modifier = Modifier.size(20.dp))

        }
    }

}


/**
 * Елемент списку з чекбоксом для вмикання/вимикання відображення одного фіскального атрибута.
 *
 * @param text Назва поля.
 * @param state Поточний стан видимості (`true` — активне/зелене, `false` — приховане/сіре).
 * @param onChange Колбек зміни стану.
 */
@Composable
fun OneProductActive(text:String,state:Boolean,onChange:(Boolean)->Unit) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
            , verticalAlignment = BiasAlignment.Vertical(0f)) {
            Spacer(modifier = Modifier.weight(0.2f))

            Checkbox(modifier = Modifier.weight(0.4f),checked = state, onCheckedChange = {onChange(it)})
            Text(modifier = Modifier.weight(1.5f),text = text, color = if(state) Color.Green else Color.Gray )


        }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OneproductActiveprev() {


}