package nekorom.com.kasaapp.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import database.Database
import java.io.File

/**
 * ====================================================================================================
 * ФАБРИКА СТВОРЕННЯ БАЗИ ДАНИХ ДЛЯ DESKTOP JVM (ROOM KMP DESKTOP FACTORY)
 * ====================================================================================================
 *
 * Будує екземпляр бази даних Room [Database] для запуску на настільних платформах (Windows, Linux, macOS).
 *
 * ОСОБЛИВОСТІ ЗБЕРІГАННЯ ТА ДРАЙВЕРА:
 * - Файл бази даних `my_room.db` розташовується в системній тимчасовій папці користувача (`java.io.tmpdir`).
 * - [BundledSQLiteDriver]: використовує збірку нативної бібліотеки SQLite, що вбудована у jar,
 *   забезпечуючи абсолютну незалежність від наявності встановленого SQLite в операційній системі.
 *
 * @return Готовий до роботи екземпляр [Database].
 */
fun getPeopleDatabase(): Database {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "my_room.db")

    return Room.databaseBuilder<Database>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
}