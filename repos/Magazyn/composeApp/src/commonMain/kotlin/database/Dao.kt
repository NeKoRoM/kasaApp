package database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
/**
 * Data Access Object (DAO) мультиплатформної бази даних Room для Magazyn.
 *
 * Надає реактивні потоки даних ([Flow]) та методи асинхронного доступу ([suspend]) для:
 * 1. Персоналу торгової точки ([Person]).
 * 2. Складських залишків номенклатури ([Product]).
 * 3. Відділів / складів магазину ([Department]).
 *
 * Підтримує атомарні транзакції повного оновлення таблиць ([clearAndInsert], [clearAndInsertDepartments]).
 */
@Dao
interface Dao {

    /** Додає або оновлює співробітника */
    @Upsert
    suspend fun upsertPerson(person: Person)

    /** Видаляє співробітника */
    @Delete
    suspend fun deletePerson(person: Person)

    /** Отримує реактивний потік усіх співробітників */
    @Query("SELECT * FROM Person")
    fun getAllPeople(): Flow<List<Person>>


    @Upsert
    suspend fun upsertProduct(product: Product)

    /**
     * Вставляє список продуктів. Якщо продукт з таким id вже існує, він буде замінений.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Delete
    suspend fun deleteProduct(product: Product)


    @Query ("SELECT * FROM Product")
    fun getAllProducts(): Flow<List<Product>>

    @Query("DELETE FROM Product")
    suspend fun clearAll()
    /**
     * Очищує таблицю та вставляє новий список продуктів в одній транзакції.
     */
    @Transaction
    suspend fun clearAndInsert(products: List<Product>) {
        clearAll()
        insertAll(products)
    }
    
    
    @Upsert
    suspend fun upsertDepartment(department: Department)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDepartments(departments: List<Department>)

    @Delete
    suspend fun deleteDepartment(department: Department)


    @Query("SELECT * FROM Department")
    fun getAllDepartments(): Flow<List<Department>>

    @Query("DELETE FROM Department")
    suspend fun clearAllDepartments()

    @Transaction
    suspend fun clearAndInsertDepartments(departments: List<Department>) {
        clearAllDepartments()
        insertAllDepartments(departments)
    }
    
    
    


}