package com.nekorom.kasaapp.data.roomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Тестова сутність Room для перевірки міграцій та коректності конфігурації SQLite бази даних.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/TestEntity.kt
 */
@Entity
data class TestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int
)