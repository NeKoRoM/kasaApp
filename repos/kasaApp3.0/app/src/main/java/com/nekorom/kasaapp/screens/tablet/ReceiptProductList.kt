package com.nekorom.kasaapp.screens.tablet

import android.app.Activity
import android.content.pm.ActivityInfo
import android.text.TextUtils.substring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.exellio20.roomDB.entities.GroupItem
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.models.receipt.entites.ProductForReceipt
import com.nekorom.kasaapp.domain.models.receipt.entites.Receipt
import com.nekorom.kasaapp.domain.usecase.CodeOfArticleToCodeForKasaUseCase
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.DialogForReceipt
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.MyDialog
import com.nekorom.kasaapp.screens.settings.SettingsViewModel
import com.nekorom.kasaapp.utils.USER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.google.accompanist.systemuicontroller.rememberSystemUiController as rememberSystemUiController

class DefaultTextInputService(platformTextInputService: PlatformTextInputService) :
    TextInputService(platformTextInputService) {
    // Реалізація методів
}
/**
 * Планшетний POS-інтерфейс робочого місця касира (Tablet POS Receipt Terminal).
 *
 * Ключова бізнес-логіка та архітектура:
 * 1. **Ландшафтна орієнтація**: Примусово встановлює [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE]
 *    та приховує системний StatusBar для максимальної робочої площі екрана.
 * 2. **Мульти-чек (3 паралельні чеки)**: Підтримує вкладки "чек1", "чек2", "чек3", дозволяючи
 *    відкласти незавершений чек одного покупця та обслугувати іншого без втрати набраного кошика.
 * 3. **Апаратний сканер штрихкодів**:
 *    - Підтримка швидкого сканування без виклику екранної клавіатури.
 *    - Підтримка синтаксису кількості: `<кількість> <штрихкод>` (наприклад, `5 482000000000\n`).
 *    - Автоматична детекція та прив'язка акцизної марки до останньої позиції підакцизного товару.
 *    - Розпізнавання посилань ДПС/Дія на електронні акцизні марки (`https://t.gov.ua/...`).
 * 4. **Фіскалізація та друк**:
 *    - Перевірка та надсилання команд на касовий апарат Datecs через [MainViewModel.sendCode].
 *    - Автоматичне чергування друку товарів та фіксації акцизних марок із необхідними затримками протоколу.
 *    - Збереження фіскалізованого чека в локальну базу даних Room через [TabletViewModel.addReceipt].
 *
 * @param viewModel Головна [MainViewModel] касового модуля.
 * @param navController Навігаційний контролер додатку.
 * @param drawerState Стан бічного меню.
 * @param editViewModel ViewModel редагування номенклатури.
 * @param settingsViewModel ViewModel налаштувань груп та ставок.
 * @param tabletViewModel ViewModel збереження чеків.
 */
@Composable
fun ReceiptProductList(
    viewModel: MainViewModel,
    navController: NavHostController,
    drawerState: DrawerState,
    editViewModel: EditCodeViewModel,
    settingsViewModel: SettingsViewModel,
    tabletViewModel: TabletViewModel
) {
    val focusManager = LocalFocusManager.current
    val focusRequesterForSearch = remember { FocusRequester() }

    val context = LocalContext.current
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    val systemUiController = rememberSystemUiController()

    DisposableEffect(key1 = true) {
        systemUiController.isStatusBarVisible = false // Status bar
        onDispose {
            systemUiController.isStatusBarVisible = false // Status bar
        }
    }

        val products = viewModel.articleListF(USER ?: UserEntity(userId = 100)).observeAsState()


        val groupList = settingsViewModel.getAllGroup(USER?.userId ?: 0).observeAsState().value
            ?: listOf<GroupItem>(
                GroupItem(groupValue = "пусто")
            )

    val idForReceipt =0



        var code by remember {
            mutableStateOf("")
        }
        var isPrintDialog by remember {
            mutableStateOf(false)
        }

        //val receipts = mutableListOf<List<ProductForReceipt>>(listOf(), listOf(), listOf())


        var receipts = listOf(
            remember {
                mutableStateListOf<ProductForReceipt>()
            },
            remember {
                mutableStateListOf<ProductForReceipt>()
            },
            remember {
                mutableStateListOf<ProductForReceipt>()
            }
        )
    LaunchedEffect(1) {
        receipts[0].addAll(tabletViewModel.receipts.getOrNull(0)?: listOf())
        receipts[1].addAll(tabletViewModel.receipts.getOrNull(1)?: listOf())
        receipts[2].addAll(tabletViewModel.receipts.getOrNull(2)?: listOf())

    }

    DisposableEffect (receipts[0],receipts[1],receipts[2]){
            onDispose {
                tabletViewModel.receipts = receipts

            }

        }
        var selectedTabIndex by remember { mutableStateOf(0) }

        Row(modifier = Modifier.fillMaxSize(1f)) {
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxSize()
            ) {
                val titles = listOf("чек1", "чек2", "чек3")

                TabRow(selectedTabIndex = selectedTabIndex) {

                    titles.forEachIndexed { index, title ->

                        Tab(
                            modifier = Modifier.padding(0.dp),

                            selected = selectedTabIndex == index,
                            unselectedContentColor = Color.Gray,
                            onClick = {


                                selectedTabIndex = index


                            },
                            text = {
                                if (selectedTabIndex == index) {
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
                val scrollState = rememberLazyListState()

                val corroutineScope = rememberCoroutineScope()






                LazyColumn(Modifier.weight(0.9f), state = scrollState) {

                    corroutineScope.launch {
                        if (receipts[selectedTabIndex].lastIndex != -1)
                            scrollState.animateScrollToItem(receipts[selectedTabIndex].lastIndex)
                    }
                    items(receipts[selectedTabIndex]) { item ->

                        SaleItem(
                            item = item,
                            onAmountChange = { text ->


                                if (groupList.indexOfFirst { item.product.group == it.groupValue && it.groupExcise } == -1) {
                                    val index = receipts[selectedTabIndex].indexOf(item)
                                    val newAmount =
                                        receipts[selectedTabIndex].firstOrNull { item == it }
                                            ?.copy(amount = text.toDoubleOrNull() ?: 1.0)
                                    if (index != -1) {
                                        receipts[selectedTabIndex][index] = newAmount ?: item
                                    }
                                }


                            },
                            onDelete = {
                                receipts[selectedTabIndex].remove(item)
                            },
                            amount = item.amount.toString(),
                            group = groupList,
                            onExciseChange = { text ->
                                var excise = item.excise
                                if (
                                        text.take(4).all { it.isLetter() } &&
                                        text.drop(4).all { it.isDigit() })
                                    excise = (text.uppercase().take(10)
                                            .replace("\u000a","")
                                            .replace("\u000d",""))
                                else if ( text.take(4).all { it.isLetter() })
                                    excise = (text.uppercase().take(4)
                                            .replace("\u000a","")
                                            .replace("\u000d",""))


                                val index = receipts[selectedTabIndex].indexOfFirst { it.id==item.id }
                                val newExcise =
                                    receipts[selectedTabIndex].firstOrNull { item.id == it.id }
                                        ?.copy(excise = excise)
                                if(index!=-1) {
                                    receipts[selectedTabIndex][index] = newExcise ?: item
                                }
                            },
                            focusManager = focusManager,
                            focusRequester = focusRequesterForSearch
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(top = 5.dp)
                        .background(MaterialTheme.colorScheme.background),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier)
                    Row(
                        Modifier
                            .background(MaterialTheme.colorScheme.onSecondaryContainer)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(text = "сума: ", fontSize = 20.sp)
                        Text(
                            text = String.format("%.2f", countReceiptPrice(receipts[selectedTabIndex])),
                            fontSize = 26.sp
                        )

                    }
                    Button(onClick = {
                        isPrintDialog = !isPrintDialog

                    }) {
                        Text(text = "оплата")

                    }
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .size(2.dp)

                    .background(MaterialTheme.colorScheme.onBackground)
            )
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize()
            ) {


                var searchText by remember {
                    mutableStateOf("")
                }
                var searchState by remember {
                    mutableStateOf(false)
                }


                var menuState by remember {
                    mutableStateOf(false)
                }
                Box {
                    ListOfItemProduct(
                        values = PaddingValues(),
                        searchText = searchText,
                        navHostController = navController,
                        editViewModel = editViewModel,
                        viewModel = viewModel
                    ) { product ->
                        searchText = ""
                        focusManager.clearFocus()
                        focusRequesterForSearch.requestFocus()
                        val indexOfProduct =
                            receipts[selectedTabIndex].indexOfFirst { it.product.productId == product.productId }
                        if (groupList.indexOfFirst { product.group == it.groupValue && it.groupExcise } == -1) {

                            if (indexOfProduct == -1)
                                receipts[selectedTabIndex].add(
                                    ProductForReceipt(product, "", 1.0)
                                )
                            else {
                                val amount = receipts[selectedTabIndex][indexOfProduct].amount
                                receipts[selectedTabIndex][indexOfProduct] =
                                    receipts[selectedTabIndex][indexOfProduct]
                                        .copy(amount = amount + 1.0)
                            }
                        } else {
                            receipts[selectedTabIndex].add(
                                ProductForReceipt(product, "", 1.0)
                            )
                        }

                    }
                    Row {
                        SearchField(
                            searchText = searchText,
                            searchState = searchState,
                            focusManager = focusManager,
                            focusRequester = focusRequesterForSearch,
                            onChangeText = { text ->

                                val exciseText = text.replace("https://t.gov.ua/", "").take(10)
                                val indexOfStar = text.indexOfFirst { it == ' ' }
                                val isBarCode = (text.all { it.isDigit() } ||
                                        text.substring(indexOfStar + 1).dropLast(1)
                                            .all { it.isDigit() })
                                        &&
                                        text.substring(
                                            if (text.lastIndex != -1) text.lastIndex
                                            else 0
                                        ) == "\u000a"

                                val isExcise =
                                    text.length == 11 &&
                                            text.take(4).all { it.isLetter() } &&
                                            text.drop(4).dropLast(1).all { it.isDigit() } &&
                                            text.substring(
                                                if (text.lastIndex != -1) text.lastIndex
                                                else 0
                                            ) == "\u000a"
                                val isExciseLink =
                                    exciseText.length == 10 &&
                                            exciseText.take(4).all { it.isLetter() } &&
                                            exciseText.drop(4).dropLast(1).all { it.isDigit() } &&
                                            text.substring(
                                                if (text.lastIndex != -1) text.lastIndex
                                                else 0
                                            ) == "\u000a"
                                if (isExcise) {
                                    if (receipts[selectedTabIndex].lastIndex != -1) {
                                        receipts[selectedTabIndex][receipts[selectedTabIndex].lastIndex] =
                                            receipts[selectedTabIndex].last().copy(excise = text)
                                    }

                                    searchText = ""

                                } else if (isBarCode) {
                                    val barCode = text.substring(indexOfStar + 1).dropLast(1)
                                    val product = products.value?.firstOrNull {
                                        it.barCode1 == barCode ||
                                                it.barCode2 == barCode ||
                                                it.barCode3 == barCode
                                    }
                                    if (product != null) {
                                        val indexOfBarProduct = receipts[selectedTabIndex]
                                            .indexOfFirst { it.product.productId == product.productId }
                                        val groupIndex =
                                            groupList.indexOfFirst { product.group == it.groupValue && it.groupExcise } != -1
                                        if (indexOfBarProduct == -1 || groupIndex) {

                                            if (indexOfStar != -1&&groupIndex) {
                                                var amount =text.substring(0, indexOfStar)
                                                        .toDoubleOrNull() ?: 1.0
                                                var timeIncrement: Long = 4
                                                while(amount!=0.0) {
                                                    timeIncrement +=5

                                                    receipts[selectedTabIndex].add(
                                                            ProductForReceipt(
                                                                    product, "",
                                                                    1.0,
                                                                    id  = System.currentTimeMillis()+timeIncrement
                                                            )
                                                    )
                                                    amount-=1.0
                                                }
                                            }else if(!groupIndex&&indexOfStar != -1){
                                                receipts[selectedTabIndex].add(
                                                        ProductForReceipt(
                                                                product, "",
                                                                text.substring(0, indexOfStar)
                                                                        .toDoubleOrNull() ?: 1.0
                                                        ))

                                            }

                                                else {
                                                receipts[selectedTabIndex].add(
                                                    ProductForReceipt(
                                                        product, "",
                                                        1.0
                                                    )
                                                )

                                            }
                                        } else {
                                            var amount = 1.0
                                            if (indexOfStar != -1) {
                                                amount = text.substring(0, indexOfStar)
                                                    .toDoubleOrNull() ?: 1.0
                                            }
                                            receipts[selectedTabIndex][indexOfBarProduct] =
                                                receipts[selectedTabIndex][indexOfBarProduct].copy(
                                                    amount =
                                                    receipts[selectedTabIndex][indexOfBarProduct].amount +
                                                            amount
                                                )
                                        }


                                    }


                                    searchText = ""
                                } else if(isExciseLink) {
                                    if (receipts[selectedTabIndex].lastIndex != -1) {
                                        receipts[selectedTabIndex][receipts[selectedTabIndex].lastIndex] =
                                            receipts[selectedTabIndex].last().copy(excise = exciseText)
                                    }

                                    searchText = ""
                                }else{
                                    searchText = text
                                }


                            }
                        ) {
                            searchState = it
                        }

                        SettingsMenu(navController, NavRoute.ReceiptProductListScreen.route)

//                    MenuForList(
//                        menuState = menuState,
//                        values = PaddingValues(),
//                        viewModel = viewModel,
//                        editViewModel = editViewModel,
//                        navHostController = navController
//                    ) {
//                        menuState = it
//
//                    }


                    }

                }

            }
            val coroutine = rememberCoroutineScope()
            if (isPrintDialog)
                DialogForReceipt(
                    sum = String.format("%.2f",
                        countReceiptPrice(receipts[selectedTabIndex]))
                        .replace(",",".").toDouble(),
                    onDismissRequest = { isPrintDialog = !isPrintDialog },
                    onConfirmation = {

                        val forSend = receipts[selectedTabIndex].sortedBy { it.excise }
                        coroutine.launch(Dispatchers.Default) {
                            forSend.forEach{ receiptProduct->
                                val barCode = CodeOfArticleToCodeForKasaUseCase()
                                viewModel.sendCode(
                                    barCode.execute(
                                        receiptProduct.product.productId.toString(),
                                        receiptProduct.amount.toInt().toString(),
                                        ""
                                    )
                                )
                                val b =
                                    groupList.indexOfFirst { it.groupExcise && receiptProduct.product.group == it.groupValue } != -1
                                if (b) {
                                    delay(200)
                                    viewModel.sendCode(receiptProduct.excise)
                                    delay(4000)


                                }

                            }
                        }
                    }) {
                    tabletViewModel.addReceipt(
                        Receipt(
                            id = 0,
                            userId = USER?.userId ?: 1,
                            kasaId = USER?.kasaId?:"",
                            products = receipts[selectedTabIndex]
                        )
                    ){
                        receipts[selectedTabIndex].clear()

                    }
                    isPrintDialog = !isPrintDialog
                    focusRequesterForSearch.requestFocus()

                    }

        }


}

/**
 * Розраховує підсумкову вартість товарів у чеку (множить ціну кожного товару на кількість).
 *
 * @param productList Список товарних позицій чека [ProductForReceipt].
 * @return Загальна вартість у гривнях.
 */
fun countReceiptPrice(productList: List<ProductForReceipt>): Double {
    var price = 0.0
    productList.forEach {
        price += it.product.price.toDouble() * it.amount
    }
    return price
}

@Preview
@Composable
private fun ListOfProduct() {
}

/**
 * Рядок товарної позиції всередині відкритого чека (POS Receipt Line Item).
 *
 * Відображає:
 * - Назву та ціну за одиницю товару.
 * - Поле редагування кількості товару з підтримкою апаратного вводу.
 * - Поле акцизної марки для алкогольних/тютюнових виробів ([GroupItem.groupExcise] == true).
 * - Розрахункову підсумкову суму рядка: `price * amount`.
 * - Кнопку швидкого видалення позиції з чека ([onDelete]).
 * - При кліку на картку автоматично додає +1 до кількості товару.
 *
 * @param item Товарна позиція [ProductForReceipt].
 * @param onDelete Колбек видалення позиції.
 * @param onAmountChange Колбек зміни кількості.
 * @param amount Поточна кількість у вигляді рядка.
 * @param group Список товарних груп для перевірки ознаки підакцизності.
 * @param onExciseChange Колбек оновлення акцизної марки.
 * @param focusManager Менеджер фокусу вводу.
 * @param focusRequester Запитувач фокусу для перемикання після сканування.
 */
@Composable
fun SaleItem(
    item: ProductForReceipt,
    onDelete: () -> Unit,
    onAmountChange: (String) -> Unit,
    amount: String,
    group: List<GroupItem>,
    onExciseChange:(String)->Unit,
    focusManager: FocusManager = LocalFocusManager.current,
    focusRequester: FocusRequester? = null
) {

    var amountText by remember {
        mutableStateOf(amount)
    }
    amountText = amount

    Card(modifier = Modifier.fillMaxWidth(), onClick = {

        onAmountChange(
            (amount
                .toDoubleOrNull()
                ?.plus(1) ?: 1.0).toString()
        )
    }) {


        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(0.7f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    Modifier
                        .padding(5.dp)
                ) {
                    Column(
                        Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "назва")
                        Text(text = item.product.name, maxLines = 2)
                    }
                    Spacer(modifier = Modifier.size(5.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ціна")
                        Text(text = item.product.price)
                    }
                    Spacer(modifier = Modifier.size(10.dp))

                }
                if (group.firstOrNull{item.product.group==it.groupValue}?.groupExcise== true) {

                    Column(
                        Modifier
                            .background(Color.Blue)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.clickable {
                                focusManager.clearFocus()
                                focusRequester?.requestFocus()
                            }
                        ){
                            TextField(value = item.excise, onValueChange = { text ->
                                onExciseChange(text)




                                if(text.contains("\u000a")||text.contains("\u000d")) {
                                    focusManager.clearFocus()
                                    focusRequester?.requestFocus()
                                }





                            }, label = { Text(text = "акцизна марка")}, singleLine = true,)
                        }


                    }
                }

            }

            Row(Modifier.weight(0.3f)) {
                Box(modifier = Modifier
                    .clickable {

                        focusManager.clearFocus()
                        focusRequester?.requestFocus()
                        onAmountChange(amountText)
                    }
                    .weight(2f)){

                        TextField(modifier = Modifier

                            .onFocusChanged {
                                if (it.isFocused)
                                    amountText = ""
                                else
                                    onAmountChange(amountText)
                            },
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                if (it.contains("\u000a")) {
                                    focusManager.clearFocus()
                                    focusRequester?.requestFocus()
                                    onAmountChange(it)
                                }
                                if (it.contains("\u000d")) {
                                    focusManager.clearFocus()
                                    focusRequester?.requestFocus()
                                    onAmountChange(it)
                                }
                            },
                            label = { Text(text = item.product.toUnitOfMeasurementShortName()) })


                }


                Column {
                    Text(text = "сума")
                    val sum = item.product.price.toDouble() * (amount.toDoubleOrNull() ?: 1.0)
                    Text(text = (sum).toString().take(sum.toString().indexOf(".") + 2))
                }


                IconButton(modifier = Modifier.weight(0.7f), onClick = { onDelete() }) {
                    Icon(Icons.TwoTone.DeleteForever, contentDescription = "", tint = Color.Red)
                }
            }

        }

    }

}





