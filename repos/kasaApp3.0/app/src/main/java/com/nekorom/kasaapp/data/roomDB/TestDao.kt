package com.nekorom.kasaapp.data.roomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object (DAO) для виконання тестових запитів до Room DB.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/TestDao.kt
 */
@Dao
interface TestDao {

    /**
     * Отримання всіх тестових записів із таблиці testentity.
     */
    @Query("SELECT * FROM testentity")
    fun getAllTest(): List<TestEntity>

    /**
     * Вставка тестового запису в базу даних.
     */
    @Insert
    fun insertTest(channel: TestEntity)
}