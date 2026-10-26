package com.nekorom.kasaapp.screens.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.chargemap.compose.numberpicker.ListItemPicker
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.MyDialog
import com.nekorom.kasaapp.screens.settings.SettingsViewModel
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_CODE
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_NAME
import com.nekorom.kasaapp.utils.Constants.Keys.ARTICLE_PRICE
import com.nekorom.kasaapp.utils.Constants.Keys.FRACTION_AMOUNT
import com.nekorom.kasaapp.utils.USER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview()
@Composable
fun preview() {
    val navController = rememberNavController() // створюємо NavController для тестування
    val drawerState =
        rememberDrawerState(DrawerValue.Closed) // створюємо DrawerState для тестування
    val viewModel: EditCodeViewModel = hiltViewModel()
    //EditProductScreen(navController = navController, drawerState = drawerState, viewModel)
}


/**
 * Екран редагування та створення картки товару (Product Edit & Creation Screen).
 *
 * Підтримує два режими роботи ([mode]):
 * - `"edit"`: Редагування існуючого товару. Поле `productId` заблоковане для запобігання пошкодженню бази в РРО.
 * - `"new"`: Створення нового товару. Номер `productId` автоматично пропонується як найменший вільний PLU.
 *
 * Особливості:
 * 1. Формує поля відповідно до динамічних налаштувань видимості [ProductEnabledEntity].
 * 2. Інтегрує випадаючі списки для специфічних фіскальних параметрів: УКТ ЗЕД, ПДВ/акциз, товарна група, одиниця виміру, тип ціни.
 * 3. При збереженні викликає [MainViewModel.sendArticle] для програмування товару у фіскальну пам'ять касового апарата через протокол Datecs.
 *
 * @param navController Навігаційний контролер додатку.
 * @param drawerState Стан бічного навігаційного меню.
 * @param viewModel Головна [MainViewModel] касового модуля.
 * @param editCodeViewModel ViewModel для роботи з полями та вибором PLU.
 * @param settingsViewModel ViewModel налаштувань ставок та груп.
 * @param mode Режим відкриття: `"edit"` або `"new"`.
 */
@Composable
fun EditProductScreen(
    navController: NavHostController,
    drawerState: DrawerState,
    viewModel: MainViewModel,
    editCodeViewModel: EditCodeViewModel,
    settingsViewModel: SettingsViewModel,
    mode: String
) {
  //  settingsViewModel.searchGroup()
    //settingsViewModel.searchUktz()

    val editModeState  by rememberSaveable {
        mutableStateOf(mode=="edit")
    }
    val fieldsStates = editCodeViewModel.getVisibleFields().observeAsState().value?.getOrNull(0)?: ProductEnabledEntity()
    val groupList = settingsViewModel.getAllGroup(USER?.userId?:0).observeAsState().value?: listOf<GroupItem>(
        GroupItem(groupValue = "пусто")
    )
    val uktzList = settingsViewModel.getAllUktz(USER?.userId?:0).observeAsState().value?: listOf<UktzItem>(
        UktzItem(pdvValue = "пусто")
    )
    val pdvList = settingsViewModel.getAllPdv(USER?.userId?:0).observeAsState().value?: listOf<PdvItem>(
        PdvItem(pdvNds = "пусто")
    )
    if (!editModeState){
        editCodeViewModel.product.id = 0
    }
    var lastIncrementProductOrId=""
    if(editCodeViewModel.product.productId!= 0)
       lastIncrementProductOrId = editCodeViewModel.product.productId.toString()
    else {
        lastIncrementProductOrId = settingsViewModel.allProduct.value?.lastOrNull()?.productId?.plus(1).toString()?:""
        editCodeViewModel.product.productId = lastIncrementProductOrId.toIntOrNull()?:1
    }





    var productId by remember {
        mutableStateOf(lastIncrementProductOrId ) }

    var TAX by remember { mutableStateOf(editCodeViewModel.product.TAX) }
    var section by remember { mutableStateOf(editCodeViewModel.product.section) }
    var group by remember { mutableStateOf(editCodeViewModel.product.group) }
    var type by remember { mutableStateOf(editCodeViewModel.product.type) }
    var price by remember { mutableStateOf(editCodeViewModel.product.price) }
    var salesAmount by remember { mutableStateOf(editCodeViewModel.product.salesAmount) }
    var salesCount by remember { mutableStateOf(editCodeViewModel.product.salesCount) }
    var remainder by remember { mutableStateOf(editCodeViewModel.product.remainder) }
    var barCode1 by remember { mutableStateOf(editCodeViewModel.product.barCode1) }
    var barCode2 by remember { mutableStateOf(editCodeViewModel.product.barCode2) }
    var barCode3 by remember { mutableStateOf(editCodeViewModel.product.barCode3) }
    var barCode4 by remember { mutableStateOf(editCodeViewModel.product.barCode4) }
    var name by remember { mutableStateOf(editCodeViewModel.product.name) }
    var weightOrUnit by remember { mutableStateOf(editCodeViewModel.product.weightOrUnit) }
    var UKTZED by remember { mutableStateOf(editCodeViewModel.product.UKTZED) }
    var unitOfMeasurement by remember { mutableStateOf(editCodeViewModel.product.unitOfMeasurement) }
    var userId by remember { mutableStateOf(editCodeViewModel.product.userId.toString()) }




    Scaffold() { values ->

        Column(modifier = Modifier.imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,

                ) {




                ProductId({
                    productId = it
                    editCodeViewModel.product.productId = it.toIntOrNull()?:1

                }, productId,  editModeState, navController, editCodeViewModel,settingsViewModel)



                Spacer(modifier = Modifier.padding(top = 20.dp))
                if (fieldsStates?.name != false) {
                    Name({
                        name = it
                        editCodeViewModel.product.name = it
                    }, name)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.price != false) {
                    Price({
                        price = it
                        editCodeViewModel.product.price = it
                    }, price)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.UKTZED != false) {
                    UKTZED(OnChange = {
                        if(it.length<=10){
                        UKTZED = it
                        editCodeViewModel.product.UKTZED = it
                        }
                    }, UKTZED,uktzList.sortedBy { it.pdvValue.toLongOrNull()?:1 },  {
                        navController.navigate(NavRoute.SettingsUktz.route)
                    },
                        {
                            settingsViewModel.searchUktz()
                        })
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.TAX != false) {
                    TAX({
                        TAX = it
                    }, returnItem = {
                                    editCodeViewModel.product.TAX = it.pdvId.toString()
                    }, TAX, pdvList,{
                         viewModel.btKasaExellioRepository.pdvRequest()
                    })
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.group != false) {
                    Group({
                        group = it
                        editCodeViewModel.product.group = it

                    }, group,groupList,
                        {
                        navController.navigate(NavRoute.SettingsGroup.route)
                        },
                        {
                            settingsViewModel.searchGroup()
                            })
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.unitOfMeasurement != false) {
                    UnitOfMeasurement({
                        unitOfMeasurement = it
                        editCodeViewModel.product.unitOfMeasurement = it
                    }, unitOfMeasurement)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.weightOrUnit != false) {
                    WeightOrUnit({
                        weightOrUnit = it
                        editCodeViewModel.product.weightOrUnit = it
                    }, weightOrUnit)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.barCode1 != false) {
                    BarCode({
                        barCode1 = it
                        editCodeViewModel.product.barCode1 = it
                    }, barCode1, Constants.Keys.BAR_CODE1, navController = navController, 1)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.remainder != false) {
                    Remainder({
                        remainder = it
                        editCodeViewModel.product.remainder = it
                    }, remainder)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.type != false) {
                    Type(onChange = {
                        type = it
                        editCodeViewModel.product.type = it
                    }, type = type)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }

                if (fieldsStates?.type != false) {
                    Section({
                        section = it
                        editCodeViewModel.product.section = it
                    }, section)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.barCode2 != false) {

                    BarCode({
                        barCode2 = it
                        editCodeViewModel.product.barCode2 = it
                    }, barCode2, Constants.Keys.BAR_CODE2, navController, 2)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.barCode3 != false) {

                    BarCode({
                        barCode3 = it
                        editCodeViewModel.product.barCode3 = it
                    }, barCode3, Constants.Keys.BAR_CODE3, navController = navController, 3)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                if (fieldsStates?.barCode4 != false) {
                    BarCode({
                        barCode4 = it
                        editCodeViewModel.product.barCode4 = it
                    }, barCode4, Constants.Keys.BAR_CODE3, navController = navController, 4)
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                }
                Column(  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .padding(3.dp)
                    .background(MaterialTheme.colorScheme.background)) {
                    if (fieldsStates?.salesAmount != false) {
                        Sales(salesAmount, Constants.Keys.SALES_AMOUNT)
                        Spacer(modifier = Modifier.padding(top = 20.dp))
                    }
                    if (fieldsStates?.salesCount != false)
                        Sales(salesCount, Constants.Keys.SALES_COUNT)
                }



            }
            var idDialogState by remember {
                mutableStateOf(false)
            }
            Button(modifier = Modifier
                .fillMaxWidth()
                .imePadding(), onClick = {
                    if (!editModeState&&
                       editCodeViewModel.products?.indexOfFirst{it.productId.toString() ==productId}!=-1
                        ){

                       idDialogState= true
                    }else {
                        viewModel.sendArticle(editCodeViewModel.product)
                        navController.popBackStack()
                    }







            }, content = { Text(text = "SEND") })
            if (idDialogState) {
                MyDialog(
                    onDismissRequest = {
                        idDialogState = false
                    },
                    onConfirmation = {
                        productId = lastIncrementProductOrId
                        editCodeViewModel.product.productId = lastIncrementProductOrId.toInt()
                        idDialogState = false
                    },
                    dialogTitle = "помилка в полях",
                    dialogText = "код вже існує"
                )
            }

        }


    }


}


/**
 * Компонент вибору та валідації внутрішнього коду товару (PLU / ProductId).
 *
 * Особливості:
 * - У режимі створення нового товару дозволяє як пряме введення, так і швидкий вибір
 *   першого вільного PLU через коліщатко вибору [ListItemPicker].
 * - Обмеження довжини в 5 символів (фіскальний ліміт Datecs РРО на код артикулу).
 * - Перевірка на дублікати через [SettingsViewModel.checkContains].
 * - У режимі редагування поле заблоковане; при спробі натискання відображає діалог [MyDialog],
 *   який пропонує створити новий товар замість зміни існуючого ID.
 *
 * @param onChange Колбек зміни значення коду.
 * @param productId Поточне значення PLU.
 * @param editModeState Прапорець режиму редагування (`true` — редагування, `false` — створення).
 * @param navController Контролер навігації.
 * @param editCodeViewModel ViewModel для вибірки вільних ID.
 * @param settingsViewModel ViewModel для перевірки унікальності коду.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun ProductId(
    onChange: (String) -> Unit,
    productId: String,
    editModeState: Boolean,
    navController: NavHostController,
    editCodeViewModel: EditCodeViewModel,
    settingsViewModel: SettingsViewModel
) {
    val listAvailableId = editCodeViewModel.getAvailableId()

    var state by remember { mutableStateOf(listAvailableId.firstOrNull{it==productId}?:listAvailableId.last()) }



    var isError by rememberSaveable { mutableStateOf(false) }
    val charLimit = 5
    var textToIdError by rememberSaveable {
        mutableStateOf("")
    }
    fun validate(text: String) {

        if (text.length > charLimit) {
            isError = text.length > charLimit
            textToIdError = "Limit: ${productId.length}/$charLimit"
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                onChange(productId.substring(startIndex = 0,endIndex = charLimit))
                delay(1000)
                validate("")

            }

        }
        else if (settingsViewModel.checkContains(text)) {
            isError = settingsViewModel.checkContains(text)
            textToIdError = "код вже існує"
        }else isError = false
    }
        var isDialog by rememberSaveable {
            mutableStateOf(false)
        }

    if (isDialog)

        MyDialog(
            onDismissRequest = { isDialog = false  },
            onConfirmation = {  navController.navigate(NavRoute.EditProductScreen.route+"/new")},
            dialogTitle = stringResource(R.string.id_dialog_title),
            dialogText = stringResource(R.string.id_dialog_sub_title),
            icon = rememberVectorPainter(image = Icons.Default.DoNotTouch),
            confirmButtonText = stringResource(id = R.string.tak)
        )



   Row(verticalAlignment = Alignment.CenterVertically) {
       TextField(
           modifier = Modifier.clickable {
               if (editModeState)
                   isDialog = true

           },
           enabled = !editModeState,
           value = productId,
           singleLine = true,
           isError = isError,
           onValueChange = {
               state = it
               onChange(it)
               if (it.toIntOrNull()!=null && productId!="") {
                   editCodeViewModel.product.productId = it.toIntOrNull()!!
               }
               validate(it)

           },
           supportingText = {
               if (isError) {
                   Text(
                       modifier = Modifier.fillMaxWidth(),
                       text = textToIdError,
                       color = MaterialTheme.colorScheme.error
                   )
               }},
           trailingIcon = {
               if (isError)
                   Icon(Icons.Filled.Error, "error", tint = MaterialTheme.colorScheme.error)
           },
           keyboardActions = KeyboardActions { onChange(productId) },
           label = { Text(text = ARTICLE_CODE) },
           keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
       )


               val values = remember { (1..99).map { it.toString() } }
              // val valuesPickerState = rememberPickerState(PickerState(0))



        if (!editModeState){


       ListItemPicker(
           modifier = Modifier.background(Color.White),
           label = { it },
           value = state,
           onValueChange = {
               state = it
               onChange(it)
               isError=false},
           list = listAvailableId
       )}







   }
   }




/**
 * Поле введення найменування товару.
 * Обмежується 52 символами верхнього регістру (максимальна довжина назви для друку в чеках Datecs РРО).
 */
@Composable
fun Name(onChange: (String) -> Unit, name: String) {
    val charLimit = 52
    var textToName by rememberSaveable {
        mutableStateOf("")
    }

    TextField(value = name,
        onValueChange = {
            if (it.length <= 52)
                onChange(it.uppercase())
            textToName = "Limit: ${it.length}/$charLimit"
        },
        label = { Text(text = ARTICLE_NAME) },
        supportingText = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = textToName,
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Characters
        )
    )
}

/**
 * Поле введення ціни товару з автоматичним форматуванням до копійок (.00) при втраті фокусу.
 * Регулярний вираз перевіряє до 8 цифр цілої частини та до 2 цифр копійок.
 */
@SuppressLint("SuspiciousIndentation")
@Composable
fun Price(onChange: (String) -> Unit, price: String) {
    TextField(
        modifier = Modifier.onFocusChanged { hasFocus ->
            if (!hasFocus.hasFocus) {
                var value = price
                if (value.isEmpty()) {
                    value = "0.00"
                } else {
                    val decimalIndex = value.indexOf('.')
                    if (decimalIndex == -1) {
                        value += ".00"
                    } else {
                        val decimals = value.substring(decimalIndex + 1)
                        if (decimals.length == 1) {
                            value += "0"
                        } else if (decimals.length == 0) {
                            value += "00"
                        }
                    }
                }
                onChange(value)
            }
        },
        value = price,
        onValueChange = {
            val pattern = Regex("""^\d{0,8}(\.\d{0,2})?$""")
            if (pattern.matches(it)) {
                onChange(it)
            }
        },
        label = { Text(text = ARTICLE_PRICE) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Перемикач ознаки вагової або штучної продукції (дозвіл продажу дробової кількості: "так"=1, "ні"=0).
 */
@Composable
fun WeightOrUnit(OnChange: (String) -> Unit, weightOrUnit: String) {
    var radioYes by remember { mutableStateOf(false) }
    var radioNo by remember { mutableStateOf(false) }
    radioNo = weightOrUnit == "0"
    radioYes = weightOrUnit == "1"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(3.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = FRACTION_AMOUNT)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "так", textAlign = TextAlign.Center)
            RadioButton(selected = radioYes, onClick = {
                radioYes = true
                radioNo = false
                OnChange("1")
            })
            Spacer(modifier = Modifier.size(50.dp))
            Text(text = "ні")
            RadioButton(selected = radioNo, onClick = {
                radioYes = false
                radioNo = true
                OnChange("0")
            })
        }
    }
}

/**
 * Поле введення штрихкоду товару (до 13 цифр) із кнопкою швидкого запуску камери для сканування.
 *
 * @param OnChange Колбек оновлення штрихкоду.
 * @param barCode Поточний штрихкод.
 * @param label Підпис поля.
 * @param navController Контролер для переходу до екрана сканера [NavRoute.BarCodeScanScreen].
 * @param barNum Індекс штрихкоду (1, 2, 3 або 4).
 */
@Composable
fun BarCode(
    OnChange: (String) -> Unit,
    barCode: String,
    label: String,
    navController: NavHostController,
    barNum: Int
) {
    TextField(
        label = { Text(label) },
        value = barCode,
        onValueChange = { OnChange(it.replace(Regex("[^\\d]"), "").take(13)) },
        trailingIcon = {
            IconButton(onClick = {
                navController.navigate(NavRoute.BarCodeScanScreen.route + "?barnum=$barNum")
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_document_scanner_24),
                    contentDescription = "Scan barcode"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Поле введення залишку товару на складі з автоформатуванням до 3 десяткових знаків (грами/кілограми).
 */
@Composable
fun Remainder(onChange: (String) -> Unit, remainder: String) {
    TextField(
        modifier = Modifier.onFocusChanged { hasFocus ->
            if (!hasFocus.hasFocus) {
                var value = remainder
                if (value.isEmpty()) {
                    value = "0.000"
                } else {
                    val decimalIndex = value.indexOf('.')
                    if (decimalIndex == -1) {
                        value += ".000"
                    } else {
                        val decimals = value.substring(decimalIndex + 1)
                        if (decimals.length == 1) {
                            value += "00"
                        } else if (decimals.length == 0) {
                            value += "000"
                        } else if (decimals.length == 2) {
                            value += "0"
                        }
                    }
                }
                onChange(value)
            }
        },
        value = remainder,
        onValueChange = {
            val pattern = Regex("""^-?\d{0,6}(\.\d{0,3})?$""")
            if (pattern.matches(it)) {
                onChange(it)
            }
        },
        label = { Text(text = Constants.Keys.REMAINDER) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Поле введення секції/відділу каси (1..99).
 */
@Composable
fun Section(OnChange: (String) -> Unit, section: String) {
    TextField(
        value = section,
        onValueChange = { OnChange(it.replace(Regex("[^\\d]"), "").take(2)) },
        label = { Text(text = Constants.Keys.SECTION) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Відображення накопичувальних даних продажу товару (кількість або загальна сума).
 */
@Composable
fun Sales(value: String, name: String) {
    Column(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
        Column(modifier = Modifier.padding()) {
            Text(text = name)
        }
        Column(
            Modifier
                .padding(end = 50.dp)
                .align(Alignment.End),
            horizontalAlignment = Alignment.End
        ) {
            Row(Modifier.padding(end = 5.dp)) {
                Spacer(modifier = Modifier.size(20.dp))
                Text(text = value)
            }
        }
    }
}


// Додайте аналогічні блоки для інших полів

//            Spacer(modifier = Modifier.height(16.dp))
//
//            Button(
//                onClick = { /* обробник натискання кнопки */ },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .heightIn(min = 48.dp)
//            ) {
//                Text(text = "Set Article")
//            }


    
