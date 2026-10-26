package com.nekorom.kasaapp.data.roomDB

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.nekorom.kasaapp.data.roomDB.entity.ProductEnabledEntity
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserWithProducts

/**
 * Головний Data Access Object (DAO) для роботи з локальною базою даних Room у kasaApp3.0.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/data/roomDB/Dao.kt
 *
 * Архітектурна роль:
 * 1. Забезпечує CRUD операції для всіх довідників торговельної точки:
 *    - Ставки ПДВ та акцизного збору (таблиця `pdv`).
 *    - Користувачі / робочі місця касирів (таблиця `userentity`).
 *    - Товарна номенклатура артикулів РРО (таблиця `productentity`).
 *    - Конфігурація відображення полів у формі редагування (таблиця `productenabledentity`).
 *    - Коди товарної класифікації УКТЗЕД (таблиця `uktz`).
 *    - Категорії та товарні групи (таблиця `group`).
 * 2. Усі запити ізольовані за `userId` (Multi-tenant Scoping), що дозволяє кільком касирам
 *    працювати на одному планшеті з власними каталогами товарів та ставками податків.
 * 3. Реактивність: методи вибірки повертають `LiveData`, що автоматично оновлює Compose UI.
 */
@Dao
interface Dao {

    // ========================================================================
    // 1. Податкові ставки ПДВ та акцизний збір (таблиця `pdv`)
    // ========================================================================

    /** Отримання списку всіх ставок ПДВ касира у вигляді реактивного LiveData */
    @Query("SELECT * FROM pdv WHERE userId = :userId ")
    fun getAllPdv(userId: Int): LiveData<List<PdvItem>>

    /** Збереження або оновлення ставки ПДВ (OnConflictStrategy.REPLACE) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdv(pdvItem: PdvItem)

    /** Повне очищення довідника ставок ПДВ для вказаного користувача */
    @Query("DELETE FROM pdv WHERE userId = :userId")
    suspend fun deleteAllPdv(userId: Int)

    // ========================================================================
    // 2. Користувачі та касири (таблиця `userentity`)
    // ========================================================================

    /** Транзакційне завантаження профілю касира разом із повним списком його товарів */
    @Transaction
    @Query("SELECT * FROM userentity WHERE userId = :userId")
    fun getUsersWithProducts(userId: Int): LiveData<UserWithProducts>

    /** Додавання або оновлення профілю касира */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUser(userEntity: UserEntity)

    /** Видалення касира за його userId */
    @Query("DELETE FROM UserEntity WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    /** Отримання переліку всіх зареєстрованих касирів на пристрої */
    @Query("SELECT * FROM userentity")
    fun getAllUsers(): LiveData<List<UserEntity>>

    // ========================================================================
    // 3. Товари та артикули РРО (таблиця `productentity`)
    // ========================================================================

    /** Пошук конкретного товару за номером артикулу (PLU) касира */
    @Query("SELECT * FROM productentity WHERE userId = :userId AND productId = :productId ")
    fun getProductForUserAndCode(userId: Int, productId: Int): LiveData<List<ProductEntity>>

    /** Отримання повної номенклатури товарів користувача для вітрини POS */
    @Query("SELECT * FROM productentity WHERE userId = :userId")
    fun getProductsForUser(userId: Int): LiveData<List<ProductEntity>>

    /** Вставка нового товару в базу даних */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProduct(product: ProductEntity)

    /** Оновлення існуючого товару */
    @Update
    suspend fun updateProduct(product: ProductEntity)

    /** Видалення всієї товарної бази касира (наприклад, при синхронізації з хмарою) */
    @Query("DELETE FROM productentity WHERE userId = :userId")
    suspend fun deleteAllArticle(userId: Int)

    /** Видалення конкретного товару за системним ID рядка */
    @Query("DELETE FROM productentity WHERE id = :id")
    suspend fun deleteArticle(id: Int)

    // ========================================================================
    // 4. Налаштування видимості полів товару (таблиця `productenabledentity`)
    // ========================================================================

    /** Отримання поточної конфігурації увімкнених полів картки товару */
    @Query("SELECT * FROM productenabledentity")
    fun getProductsFields(): LiveData<List<ProductEnabledEntity>>

    /** Збереження налаштувань видимості полів товару */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProductFields(productEnabledEntity: ProductEnabledEntity)

    // ========================================================================
    // 5. Класифікатор кодів УКТЗЕД (таблиця `uktz`)
    // ========================================================================

    /** Отримання довідника кодів УКТЗЕД касира */
    @Query("SELECT * FROM uktz WHERE userId = :userId")
    fun getAllUKTZ(userId: Int): LiveData<List<UktzItem>>

    /** Видалення запису УКТЗЕД за ідентифікатором */
    @Query("DELETE FROM uktz WHERE id IS :id")
    suspend fun deleteUktz(id: Int)

    /** Додавання або оновлення коду УКТЗЕД */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUktz(uktzItem: UktzItem)

    /** Видалення всіх кодів УКТЗЕД для користувача */
    @Query("DELETE FROM uktz WHERE userId = :userId")
    suspend fun deleteAllUktz(userId: Int)

    // ========================================================================
    // 6. Товарні групи та категорії (таблиця `group`)
    // ========================================================================

    /** Отримання групи товарів за числовим значенням у РРО */
    @Query("SELECT * FROM `group` WHERE userId = :userId AND groupValue = :groupValue LIMIT 1")
    fun getGroup(groupValue: String, userId: Int): List<GroupItem>?

    /** Отримання списку всіх категорій товарів користувача */
    @Query("SELECT * FROM `group` WHERE userId = :userId ")
    fun getAllGroup(userId: Int): LiveData<List<GroupItem>>

    /** Видалення категорії за ідентифікатором */
    @Query("DELETE FROM `group` WHERE id IS :id")
    suspend fun deleteGroup(id: Int)

    /** Очищення всіх товарних груп користувача */
    @Query("DELETE FROM `group` WHERE userId = :userId")
    suspend fun deleteAllGroup(userId: Int)

    /** Додавання або заміна товарної групи */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(groupItem: GroupItem)
}