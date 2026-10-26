package pl.neko.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.neko.database.entities.SettingsEntity

@Dao
interface Dao {
    // Create / Update
    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)

    // Read all settings
    @Query("SELECT * FROM settingsentity")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    // Read settings by user ID
    @Query("SELECT * FROM settingsentity WHERE userId = :userId LIMIT 1")
    fun getSettingsByUserId(userId: String): Flow<SettingsEntity?>

    // Delete by entity
    @Delete
    suspend fun deleteSettings(settings: SettingsEntity)
}