
package com.nekorom.kasaapp.screens.tablet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.domain.models.receipt.entites.ProductForReceipt
import com.nekorom.kasaapp.domain.models.receipt.entites.Receipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для планшетного інтерфейсу касира (Tablet POS UI).
 *
 * Відповідає за:
 * - Утримання стану поточних списків чеків ([receipts]).
 * - Асинхронне збереження закритих фіскальних чеків у локальну базу даних Room ([addReceipt]).
 *
 * @param database Екземпляр основної бази даних [AppDatabase].
 */
@HiltViewModel
class TabletViewModel @Inject constructor(
    database: AppDatabase
): ViewModel() {
    val receiptDao = database.receiptDao()

    /** Список товарів для чеків */
    var receipts = listOf<List<ProductForReceipt>>()

    /**
     * Зберігає сформований фіскальний чек у базу даних Room у фоновому потоці [Dispatchers.IO].
     *
     * @param receipt Об'єкт чека [Receipt].
     * @param onSuccess Колбек успішного збереження.
     */
    fun addReceipt(receipt: Receipt, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            receiptDao.addReceipt(receipt.toReceiptDbEntity())
            onSuccess()
        }
    }
}