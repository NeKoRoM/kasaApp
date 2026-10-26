package com.nekorom.kasaapp.domain.models.receipt.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) для збереження та вибірки розрахункових документів (чеків).
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/domain/models/receipt/room/ReceiptDao.kt
 *
 * Архітектурна роль:
 * - Забезпечує доступ до збереженої історії продажів касира в таблиці `receipt`.
 * - Використовується екраном журналу чеків (`ReceiptScreen`) для відображення списку транзакцій.
 */
@Dao
interface ReceiptDao {

    /**
     * Отримання всіх збережених чеків конкретного касира (фільтрація за user_id).
     * Повертає реактивний LiveData-потік для автоматичного оновлення інтерфейсу.
     */
    @Query("SELECT * FROM receipt WHERE user_id = :userId")
    fun getUsersWithReceipt(userId: Int): LiveData<List<ReceiptDbEntity>>

    /**
     * Збереження нового чека або оновлення існуючого після завершення друку на РРО.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addReceipt(receipt: ReceiptDbEntity)
}