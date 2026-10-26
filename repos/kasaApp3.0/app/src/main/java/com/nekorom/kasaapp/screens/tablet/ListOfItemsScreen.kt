package com.nekorom.kasaapp.screens.tablet

import android.media.MediaPlayer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.scan.ItemProductForList
import com.nekorom.kasaapp.utils.USER

/**
 * Компактна панель списку товарів для планшетного інтерфейсу (Tablet Catalog Split-View).
 *
 * Розміщується у правій колонці екрана поруч із відкритим чеком:
 * - Фільтрує номенклатуру за рядком пошуку [searchText] (назва або код PLU).
 * - Відображає картки товарів [ItemProductForList] у спрощеному режимі (`isEditable = false`).
 * - Клік по товару [onItemClick] миттєво додає одиницю товару до активного чека у лівій колонці.
 *
 * @param values Системні відступи [PaddingValues].
 * @param searchText Поточний текст фільтра пошуку.
 * @param navHostController Навігаційний контролер.
 * @param editViewModel ViewModel редагування товару.
 * @param viewModel Головна [MainViewModel].
 * @param onItemClick Колбек вибору товару для додавання в чек.
 */
@Composable
fun ListOfItemProduct(
    values: PaddingValues,
    searchText: String,
    navHostController: NavHostController,
    editViewModel: EditCodeViewModel,
    viewModel: MainViewModel,
    onItemClick: (ProductEntity) -> Unit
) {
    val group = editViewModel.group.value
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val context = LocalContext.current
    val products = viewModel.articleListF(USER ?: UserEntity(userId = 100))
        .observeAsState().value?.sortedBy { it.productId }
    val filteredProducts = products?.filter {
        it.name.contains(searchText, ignoreCase = true ) ||
                it.productId.toString().contains(searchText, ignoreCase = true)


    }


    LazyColumn(
        androidx.compose.ui.Modifier
            .padding(values)
            .padding(top = 45.dp)
    ) {
        filteredProducts?.let {
            items(filteredProducts) { item ->

                    ItemProductForList(item, onClick =  {
                        onItemClick(item)
//                        val barCode = CodeOfArticleToCodeForKasaUseCase()
//                        viewModel.sendCode(barCode.execute(item.productId.toString(), "1", ""))
//                        mediaPlayer = MediaPlayer.create(context, R.raw.beep)
//                        mediaPlayer?.start()
//
//                        if (group?.filter { it.groupValue.toString() == item.group }
//                                ?.lastOrNull()?.groupExcise == true) {}
                            //inNavController.navigate(BottomNavRoute.CameraScreenInOne.route)

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
                            navHostController.navigate(NavRoute.EditProductScreen.route+"/new")
                        },
                        isEditable = false)



            }
        }

    }

}