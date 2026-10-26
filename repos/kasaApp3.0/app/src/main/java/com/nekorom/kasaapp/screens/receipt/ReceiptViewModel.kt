package com.nekorom.kasaapp.screens.receipt

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.room.Database
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.domain.models.receipt.room.ReceiptDbEntity
import com.nekorom.kasaapp.domain.models.receipt.room.UserWithReceipt
import com.nekorom.kasaapp.utils.USER
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для екрана історії та перегляду фіскальних чеків ([ReceiptScreen]).
 *
 * Завантажує локальний журнал проведених чеків ([ReceiptDbEntity]) для активного касира ([USER]),
 * що зберігаються в локальній базі Room для подальшого звітування та аналітики.
 *
 * @param database Екземпляр основної бази даних додатку [AppDatabase].
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    database: AppDatabase
): ViewModel() {
    val receiptDao = database.receiptDao()

    /**
     * Отримує реактивний список збережених чеків для поточного касира.
     */
    fun getReceipts(): LiveData<List<ReceiptDbEntity>> = receiptDao.getUsersWithReceipt(USER?.userId ?: 100)
}