package com.nekorom.kasaapp.screens.settings

import android.util.Log
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.utils.USER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
// search uktz
//load pdv

/**
 * ViewModel для екранів налаштувань конфігурації додатку (Settings Module ViewModel).
 *
 * Відповідає за:
 * 1. **Автоматичний збір та індексацію довідників**:
 *    - Сканує всю наявну номенклатуру товарів ([allProduct]) і автоматично створює записи в довідниках
 *      товарних груп ([searchGroup]) та кодів УКТ ЗЕД ([searchUktz]), якщо таких ще немає в базі.
 * 2. **Керування довідниками**: Додавання, редагування та видалення товарних груп ([GroupItem]) та кодів УКТ ЗЕД ([UktzItem]).
 * 3. **Конфігурація інтерфейсу**: Збереження прапорців видимості полів форми редагування ([ProductEnabledEntity]).
 * 4. **Валідація PLU**: Перевірка наявності ідентифікатора товару в активній базі ([checkContains]).
 *
 * @param database Екземпляр основної бази даних [AppDatabase].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    database: AppDatabase
): ViewModel() {
    private val dao = database.dao()
    val productEnabledEntity = getVisibleFields()
    val allGroup = getAllGroup(USER?.userId ?: 0)
    val allUktz = getAllUktz(USER?.userId ?: 0)
    var allProduct = articleList(USER ?: UserEntity(0))

    init {
        allGroup.observeForever {}
        allUktz.observeForever {}

        // Слухач для автонаповнення довідників груп та УКТ ЗЕД при надходженні нових товарів
        allProduct.observeForever { productList ->
            if (allGroup.value != null && allGroup.value?.indexOfFirst { it.groupValue == productList.lastOrNull()?.group } == -1)
                insertGroup(GroupItem(groupValue = productList.lastOrNull()?.group ?: "", userId = USER?.userId ?: 0))
            if (allUktz.value != null && allUktz.value?.indexOfFirst { it.pdvValue == productList.lastOrNull()?.UKTZED } == -1)
                insertUktz(UktzItem(pdvValue = productList.lastOrNull()?.UKTZED ?: "", userId = USER?.userId ?: 0))
        }
    }

    /**
     * Перевіряє, чи вже зайнято вказаний PLU ([productId]) в активній базі товарів.
     * @param productId Рядковий числовий код товару.
     * @return `true`, якщо код уже закріплений за існуючим товаром.
     */
    fun checkContains(productId: String): Boolean {
        return allProduct.value?.map { it.productId.toString() }?.contains(productId) ?: false
    }

    /** Отримує реактивний список товарів користувача */
    fun articleList(user: UserEntity): LiveData<List<ProductEntity>> = dao.getProductsForUser(user.userId)

    /** Отримує список зареєстрованих ставок ПДВ каси */
    fun getAllPdv(userId: Int): LiveData<List<PdvItem>> = dao.getAllPdv(userId)

    //group

    fun getAllGroup(userId: Int): LiveData<List<GroupItem>> = dao.getAllGroup(userId)

    fun insertGroup(groupItem: GroupItem)= viewModelScope.launch {
        if (groupItem.groupValue!="") {
            dao.deleteGroup(groupItem.id)
            dao.insertGroup(groupItem)
        }
    }

    fun searchGroup(){


        viewModelScope.launch {
            delay(500)
            val allGroup = allGroup.value?.map { GroupItem(groupValue = it.groupValue, userId = USER?.userId?:0) }?.toMutableSet()?: mutableSetOf<GroupItem>()

            val groupSet= allProduct.value?.map {GroupItem(groupValue = it.group, userId = USER?.userId?:0) }?.toSet()?:setOf<GroupItem>()

            for (group in groupSet){
                if (allGroup.indexOfFirst { it.groupValue == group.groupValue }==-1)
                    insertGroup(group)
            }
        }


//        viewModelScope.launch(Dispatchers.IO) {dao.deleteAllGroup(USER?.userId?:0)
//        groupSet.forEach{
//            dao.insertGroup(it)
//        }
//        }
//        var productGroupSet = allProduct.value?.map {GroupItem(groupValue = it.group, userId = USER?.userId?:0) }?.toSet()?:setOf<GroupItem>()
//        for (productGroup in productGroupSet) {
//            Log.d("searchGroup",productGroup.toString())
//            if (!groupSet.contains(productGroup)) {
//                insertGroup(productGroup)
//            }
//           }
//        missingGroups.forEach{
//            insertGroup(it)
//        }
    }

    fun searchUktz(){

        viewModelScope.launch { delay(500)
            var allUktz = allUktz.value//?.map { GroupItem(groupValue = it.groupValue, userId = USER?.userId?:0) }?.toMutableSet()?: mutableSetOf<GroupItem>()

            val uktzSet= allProduct.value?.map {UktzItem(pdvValue = it.UKTZED, userId = USER?.userId?:0) }?.toSet()?:setOf<UktzItem>()

            for (uktz in uktzSet){
                if (allUktz?.indexOfFirst{it.pdvValue == uktz.pdvValue}==-1)
                    insertUktz(uktz)
            }

        }

    }
    fun deleteGroup(id: Int)= viewModelScope.launch {
        dao.deleteGroup(id)
    }


    //uktz
    fun getAllUktz(userId: Int): LiveData<List<UktzItem>> = dao.getAllUKTZ(userId)

    fun insertUktz(uktzItem: UktzItem)= viewModelScope.launch {
        if (uktzItem.pdvValue!="")
        dao.insertUktz(uktzItem)
    }
    fun deleteUktz(id: Int)= viewModelScope.launch {
        dao.deleteUktz(id)
    }


    //visibility
    fun getVisibleFields(): LiveData<List<ProductEnabledEntity>> = dao.getProductsFields()

    fun setVisibilityFields(fieldsVisibility: ProductEnabledEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.addProductFields(fieldsVisibility?: ProductEnabledEntity(1))
        }
    }

}