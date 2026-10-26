// =================================================================================================
// РЕАЛІЗАЦІЯ СХОВИЩА ТЕСТОВИХ ДАНИХ ЧЕРЕЗ ROOM DATABASE
//
// Архітектурний шар: Data Layer (Storage Implementation).
// Призначення:
// Реалізує контракт TestStorage за допомогою Room TestDao.
// Забезпечує асинхронний запис у фоновому потоці Dispatchers.IO та синхронну вибірку.
// =================================================================================================

package com.nekorom.kasaapp.data.storage

import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.roomDB.TestEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Конкретна імплементація [TestStorage], що зберігає сутності [TestEntity] у локальній базі Room SQLite.
 *
 * @param database Екземпляр [AppDatabase] для отримання доступу до [TestDao].
 */
class RoomTestStorage(database: AppDatabase): TestStorage {

    // DAO для низькорівневих CRUD-операцій з таблицею тестування
    val dao = database.testDao()

    // Окремий CoroutineScope на фоновому диспетчері вводу-виводу для незалежних операцій запису
    private val myScope = CoroutineScope(Dispatchers.IO)

    /**
     * Асинхронна вставка сутності [TestEntity] у фоновому потоці.
     * @return true одразу після запуску корутини.
     */
    override fun save(test: TestEntity): Boolean {
        myScope.launch {
            dao.insertTest(test)
        }
        return true
    }

    /**
     * Отримання повного списку тестових записів із бази даних Room.
     */
    override fun get(): List<TestEntity> {
        return dao.getAllTest()
    }
}