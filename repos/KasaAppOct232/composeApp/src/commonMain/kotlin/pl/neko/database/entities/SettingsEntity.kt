package pl.neko.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class SettingsEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val isDarkMode: Boolean,

    )