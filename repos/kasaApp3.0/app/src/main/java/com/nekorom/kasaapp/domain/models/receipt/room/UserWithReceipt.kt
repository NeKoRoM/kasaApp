package com.nekorom.kasaapp.domain.models.receipt.room

import androidx.room.Embedded
import androidx.room.Relation
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.models.receipt.entites.Receipt

/**
 * Складна модель Room для зв'язку один-до-багатьох: Касир (UserEntity) та список його чеків (Receipt).
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/domain/models/receipt/room/UserWithReceipt.kt
 */
data class UserWithReceipt(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val receiptList: List<Receipt>
)