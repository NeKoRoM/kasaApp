package com.nekorom.kasaapp.screens.scan

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.usecase.CodeOfArticleToCodeForKasaUseCase
import com.nekorom.kasaapp.navigation.BottomNavRoute
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.MyDialog
import com.nekorom.kasaapp.screens.tablet.SettingsMenu
import com.nekorom.kasaapp.utils.USER
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Екран каталогу товарів та формування позицій чека (Product Catalog & Receipt Staging Screen).
 *
 * Основна бізнес-логіка:
 * 1. **Пошук та фільтрація**: Пошук у реальному часі за назвою товару, внутрішнім кодом (PLU)
 *    або будь-яким із 3-х збережених штрихкодів ([ProductEntity.barCode1]..[ProductEntity.barCode3]).
 * 2. **Додавання товару в чек**:
 *    - За допомогою [CodeOfArticleToCodeForKasaUseCase] генерує 13-значний протокольний код товару.
 *    - Відправляє код на касу через [MainViewModel.sendCode].
 *    - Якщо податкова група товару підакцизна ([GroupItem.groupExcise] == true), автоматично
 *      відкриває екран камери [BottomNavRoute.CameraScreenInOne] для сканування/введення акцизної марки.
 * 3. **Синхронізація з Datecs РРО**:
 *    - Кнопка завантаження бази з каси ([MainViewModel.readKasaDB]).
 *    - Попередження про втрату локальних незбережених змін ([dialogIsIndependent]), якщо є товари з `isIndependent = true`.
 *    - Пакетна вигрузка нових/змінених товарів у касу ([MainViewModel.sendAllArticle]).
 * 4. **Керування карткою товару**: Редагування, дублювання, видалення з каси та бази Room.
 *
 * @param values Системні відступи [PaddingValues].
 * @param viewModel Головна [MainViewModel] касового модуля.
 * @param editViewModel ViewModel редагування номенклатури.
 * @param navHostController Кореневий навігатор додатку.
 * @param inNavController Навігатор нижньої панелі вкладок.
 */
@Composable
fun ListForSendScreen(
    values: PaddingValues,
    viewModel: MainViewModel,
    editViewModel: EditCodeViewModel,
    navHostController: NavHostController,
    inNavController: NavHostController
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val context = LocalContext.current
    var menuState by remember {
        mutableStateOf(false)
    }
    var btRequestState = viewModel.btRequestState.observeAsState()
    val modifierForMenu = Modifier.background(Color(R.color.teal_200), CircleShape)
    val group = editViewModel.group.value

    var searchText by remember {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var searchState by remember {
        mutableStateOf(false)
    }
    var sendingState by remember {
        mutableStateOf(false)
    }



    val products = viewModel.articleListF(USER ?: UserEntity(userId = 100))
        .observeAsState().value?.sortedBy { it.productId }
    val filteredProducts = products?.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.productId.toString().contains(searchText, ignoreCase = true,)||
                it.barCode1.contains(searchText.replace("\u000a","").replace("\u000d",""))||
                it.barCode2.contains(searchText.replace("\u000a","").replace("\u000d",""))||
                it.barCode3.contains(searchText.replace("\u000a","").replace("\u000d",""))||
                (!it.isIndependent&& sendingState)


    }





    Box {




        LazyColumn(
            Modifier
                .padding(values)
                .padding(top = 45.dp)
        ) {
            filteredProducts?.let {
                items(filteredProducts) { item ->

                        ItemProductForList(item, {
                            val barCode = CodeOfArticleToCodeForKasaUseCase()
                            viewModel.sendCode(
                                barCode.execute(
                                    item.productId.toString(),
                                    "1",
                                    ""
                                )
                            )
                            mediaPlayer = MediaPlayer.create(context, R.raw.beep)
                            mediaPlayer?.start()

                            if (group?.filter { it.groupValue.toString() == item.group }
                                    ?.lastOrNull()?.groupExcise == true)
                                inNavController.navigate(BottomNavRoute.CameraScreenInOne.route)

                        }, onClickEdit = {
                            viewModel.kasaSendId()
                            editViewModel.product = item
                            navHostController.navigate(NavRoute.EditProductScreen.route + "/edit")
                        },
                            onDelete = {
                                viewModel.btKasaExellioRepository.deleteArticleRequest(
                                    item.productId,
                                    item.id
                                )
                            },
                            onCopy = {

                                viewModel.kasaSendId()
                                editViewModel.product = item
                                navHostController.navigate(NavRoute.EditProductScreen.route + "/new")
                            },
                            onSend = {
                                viewModel.sendArticle(item)
                            }
                        )




                }
            }

        }

        var dialogIsIndependent by remember {
            mutableStateOf(false)
        }

        var infoState by remember {
            mutableStateOf(false)
        }
        Row {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                trailingIcon = {
                    IconButton(onClick = {
                        searchState = !searchState
                        if (searchState)
                            focusRequester.requestFocus()
                        else
                            focusManager.clearFocus()

                    })

                    {
                        if (searchState)
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        else
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Green
                            )

                    }

                },
                modifier = Modifier
                    .background(Color.Transparent)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.hasFocus)
                            searchState = true
                    },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(R.color.teal_700),
                    disabledContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),


                )




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
                    onClick = { menuState = !menuState })
                {
                    if (menuState) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    } else
                        Icon(Icons.Default.Add, contentDescription = "menu")


                }
                Spacer(modifier = Modifier.size(25.dp))
                if (menuState && btRequestState.value == false) {


                    IconButton(modifier = modifierForMenu,
                        onClick = { infoState = !infoState }) {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                 Spacer(modifier = Modifier.size(5.dp))

//                    IconButton(modifier = modifierForMenu,
//                        onClick = { navHostController.navigate(NavRoute.BarCodeScanScreen.route + "?barnum=51") }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.barcode_search),
//                            contentDescription = null
//                        )
//                    }
//                 Spacer(modifier = Modifier.size(5.dp))
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
                 Spacer(modifier = Modifier.size(5.dp))
                    IconButton(modifier = modifierForMenu,
                        onClick = {
                            if (products?.indexOfFirst { it.isIndependent }==-1)
                            viewModel.readKasaDB(USER ?: UserEntity())
                            else
                            {
                            dialogIsIndependent = true
                            }
                        }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    }

                    if (products?.indexOfFirst { it.isIndependent }!=-1){
                                          Spacer(modifier = Modifier.size(5.dp))
                        IconButton(modifier = modifierForMenu, onClick = {
                            products?.filter { it.isIndependent }
                                ?.let {
                                    sendingState = true
                                    viewModel.sendAllArticle(it){
                                    sendingState =  false
                                } }
                        }) {
                            Icon(imageVector = Icons.Default.SendTimeExtension, contentDescription = "")
                        }
                    }
                    SettingsMenu(navController = navHostController, NavRoute.Scanner.route)


                }
                Spacer(modifier = Modifier.size(20.dp))



            }


        }

        if (dialogIsIndependent){
            MyDialog(
                onDismissRequest = { dialogIsIndependent = false},
                onConfirmation = { dialogIsIndependent = false
                    viewModel.readKasaDB(USER ?: UserEntity())

                                 },
                dialogTitle = "ВТРАТА ДАНИХ",
                dialogText = "є не збережені позиції якщо ви оновете базу " +
                        "зміни не будуть збережені",
                confirmButtonText = "все одно загрузити з каси"
            )
        }
        if (infoState) {
            val infoSize = products?.size.toString()
            Column(
                modifier = Modifier
                    .padding(values)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                Text(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    text = "size =" + infoSize
                )
            }

        }
    }


}




/**
 * Картка товару в загальному каталозі (Catalog Product Item Card).
 *
 * Відображає:
 * - Внутрішній касовий код (PLU: [ProductEntity.productId]), ціну та назву товару.
 * - Колірну індикацію статусу синхронізації: товари з несинхронізованими змінами ([ProductEntity.isIndependent])
 *   виділяються кольором `tertiaryContainer` та мають кнопку прямої відправки в РРО ([onSend]).
 * - Розкривну панель дій: видалення з РРО ([onDelete]), створення копії ([onCopy]), повне редагування ([onClickEdit]).
 * - Клік на саму картку ([onClick]) ініціює додавання одиниці товару в активний чек.
 *
 * @param article Сутність товару з локальної бази даних Room.
 * @param onClick Додавання товару в поточний чек.
 * @param onClickEdit Відкриття екрана редагування картки товару.
 * @param onDelete Видалення товару з бази РРО та Room.
 * @param onCopy Дублювання товару для створення схожої позиції.
 * @param isEditable Дозвіл на відкриття меню редагування.
 * @param onSend Відправка одиничного оновленого товару в пам'ять каси.
 */
@Composable
fun ItemProductForList(
    article: ProductEntity,
    onClick: () -> Unit,
    onClickEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy:()->Unit,
    isEditable: Boolean = true,
    onSend:()->Unit = {}
) {
    var id: String = article.productId.toString()
    var name: String = article.name
    var price: String = article.price
    var dialogDeleteState by remember {
        mutableStateOf(false)
    }
    Card(
        elevation = CardDefaults.cardElevation(20.dp),
        shape = CardDefaults.shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor =
            if (article.isIndependent) MaterialTheme.colorScheme.tertiaryContainer
            else Color.Unspecified
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 5.dp, top = 5.dp)

        ) {
            Text(
                //modifier = Modifier.,
                textAlign = TextAlign.Right,
                text = "КОД: $id",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,

                )

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Right,
                text = "ЦІНА: $price",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,

                )
        }

        var additionalState by remember {
            mutableStateOf(false)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                modifier = Modifier
                    .padding(5.dp)
                    .weight(0.8f)
                    .fillMaxWidth(),
                text = "НАЗВА: $name",
                fontSize = 20.sp,
                textAlign = TextAlign.Start
            )

            if (article.isIndependent&& isEditable
                ){
                IconButton(onClick = { onSend() }) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "")

                }

            }
            if (isEditable) {
                IconButton(modifier = Modifier.weight(0.1f), onClick = {
                    additionalState = !additionalState
                }) {
                    if (!additionalState)
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = ""
                        )
                    else
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = ""
                        )

                }
            }
        }

        if (additionalState) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer)
                    .size(10.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                IconButton(
                    modifier = Modifier.shadow(shape = CircleShape, elevation = 1.dp),
                    onClick = { dialogDeleteState = true }) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "",
                        tint = Color.Red)
                }
                IconButton(
                    modifier = Modifier.shadow(shape = CircleShape, elevation = 1.dp),
                    onClick = {
                        onCopy()
                    }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "")
                }
                IconButton(
                    modifier = Modifier.shadow(shape = CircleShape, elevation = 1.dp),
                    onClick = { onClickEdit() }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                }

            }
            if (dialogDeleteState)
                MyDialog(
                    onDismissRequest = { dialogDeleteState = false },
                    onConfirmation = {
                        dialogDeleteState = false
                        onDelete()
                    },
                    dialogTitle = "??ВИДАЛИТИ??",
                    dialogText = "видалити продукт" + article.name + "??",
                    icon = rememberVectorPainter(image = Icons.Default.Delete),
                    titleColor = Color.Red
                )
        }
    }


}

@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    ItemProductForList(
        ProductEntity(
            productId = 50000,
            id = 5000,
            name = "Product ",
            price = "$99.99"
        ), {}, {},{},{})
}