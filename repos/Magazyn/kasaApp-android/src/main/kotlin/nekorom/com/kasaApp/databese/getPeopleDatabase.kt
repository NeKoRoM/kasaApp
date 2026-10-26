package nekorom.com.kasaApp.databese

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import database.Database

/**
 * ====================================================================================================
 * ФАБРИКА СТВОРЕННЯ БАЗИ ДАНИХ ДЛЯ ПЛАТФОРМИ ANDROID (ROOM KMP FACTORY)
 * ====================================================================================================
 *
 * Будує екземпляр мультиплатформної бази даних Room [Database] для платформи Android.
 *
 * АРХІТЕКТУРНІ ОСОБЛИВОСТІ:
 * - Файл бази `people.db` зберігається у захищеному сховищі додатку (`context.getDatabasePath`).
 * - [BundledSQLiteDriver]: використовує вбудовану мультиплатформну бібліотеку SQLite, що гарантує
 *   однакову поведінку та версію SQLite на Android, Desktop і Web незалежно від версії ОС Android.
 *
 * @param context Контекст Android-додатку.
 * @return Повністю налаштований екземпляр бази даних [Database].
 */
fun getPeopleDatabase(context: Context): Database {
    val dbFile = context.getDatabasePath("people.db")

    return Room.databaseBuilder<Database>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}