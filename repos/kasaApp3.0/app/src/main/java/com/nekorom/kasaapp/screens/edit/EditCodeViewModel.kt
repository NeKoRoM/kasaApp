package com.nekorom.kasaapp.screens.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.exellio20.roomDB.entities.GroupItem
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.utils.USER
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для екрана редагування та створення карток товарів ([EditProductScreen]).
 *
 * Відповідає за:
 * 1. Завантаження налаштувань видимості полів картки товару ([getVisibleFields]).
 * 2. Отримання актуального списку товарів та товарних груп для активного касира ([USER]).
 * 3. **Алгоритм пошуку вільних PLU ([getAvailableId])**: знаходить незайняті числові ідентифікатори
 *    у діапазоні від 1 до максимального існуючого ID для автоматичного присвоєння новому товару в базі РРО.
 * 4. Тимчасове збереження редагованого товару ([product]) між переходами навігації.
 */
@HiltViewModel
class EditCodeViewModel @Inject constructor(
    database: AppDatabase
): ViewModel() {
    val dao = database.dao()
    var articles = articleListF(USER!!)
    var group = getAllGroup(USER?.userId!!)

    init {
        articles.observeForever {
            products?.addAll(it)
        }
        group.observeForever {}
    }

    /**
     * Отримує прапорці видимості та блокування окремих полів форми редагування товару.
     */
    fun getVisibleFields(): LiveData<List<ProductEnabledEntity>> = dao.getProductsFields()

    /**
     * Отримує реактивний список товарів для вказаного користувача.
     */
    fun articleListF(user: UserEntity): LiveData<List<ProductEntity>> = dao.getProductsForUser(user.userId)

    /**
     * Отримує перелік товарних груп, налаштованих для користувача.
     */
    fun getAllGroup(userId: Int): LiveData<List<GroupItem>> = dao.getAllGroup(userId)

    /**
     * Обчислює список перших вільних числових ID (PLU) для створення нового товару в Datecs РРО.
     * Знаходить "дірки" у послідовності зайнятих номерів від 1 до (maxId + 1).
     *
     * @return Список доступних рядкових ідентифікаторів PLU.
     */
    fun getAvailableId(): List<String> {
        val availableList = mutableListOf<String>()
        val listOfArticle = articles?.value?.map { it.productId } ?: listOf()
        val last = listOfArticle.maxOrNull()?.plus(1) ?: 10000
        val lineList = (1..last).toMutableList()
        lineList.minus(listOfArticle.toSet()).forEach {
            availableList.add(it.toString())
        }
        return availableList
    }

    /** Поточний об'єкт товару в процесі редагування або створення */
    var product: ProductEntity = ProductEntity()

    /** Кешований набір товарів активного касира */
    var products: MutableSet<ProductEntity>? = mutableSetOf()
}