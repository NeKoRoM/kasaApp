package pl.neko.di

import android.app.Application
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import pl.neko.database.AppDatabase

actual class DatabaseFactory(
    private val app: Application,
) {
    actual fun createRoomDatabase(): AppDatabase {
        val dbFile = app.getDatabasePath("kasa_app.db")
        return Room
            .databaseBuilder<AppDatabase>(
                context = app,
                name = dbFile.absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }


}