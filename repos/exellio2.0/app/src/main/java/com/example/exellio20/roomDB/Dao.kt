package com.example.exellio20.roomDB

import androidx.room.*
import androidx.room.Dao
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import kotlinx.coroutines.flow.Flow

/**
 * ====================================================================================================
 * ІНТЕРФЕЙС ДОСТУПУ ДО ДАНИХ (ROOM DAO - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Визначає методи читання, запису та видалення даних для таблиць бази даних `article_list.db`:
 * - `article`: каталог товарів (PLU)
 * - `uktz`: коди УКТЗЕД
 * - `pdv`: ставки податку та акцизу
 * - `group`: товарні групи
 */
@Dao
interface Dao {

    // ------------------------------------------------------------------------------------------------
    // ОПЕРАЦІЇ З ТОВАРАМИ ТА АРТИКУЛАМИ (ARTICLE)
    // ------------------------------------------------------------------------------------------------

    /**
     * Повертає реактивний потік усіх товарів у локальній базі.
     */
    @Query("SELECT * FROM article")
    fun getAllArticle(): Flow<List<ArticleItem>>

    /**
     * Повертає реактивний потік лише активних товарів із ціною, відмінною від нуля.
     */
    @Query("SELECT * FROM article WHERE price <>'0.00'")
    fun getWithoutNullArticle(): Flow<List<ArticleItem>>

    /**
     * Очищує всю таблицю товарів (наприклад, перед повним повторним вивантаженням із каси).
     */
    @Query("DELETE FROM article")
    suspend fun deleteAllArticle()

    /**
     * Вставляє або замінює товар у базі даних за його первинним ключем.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(articleItem: ArticleItem)

    // ------------------------------------------------------------------------------------------------
    // ОПЕРАЦІЇ З ДОВІДНИКОМ КОДІВ УКТЗЕД (UKTZ)
    // ------------------------------------------------------------------------------------------------

    /**
     * Отримує потік усіх налаштованих кодів УКТЗЕД.
     */
    @Query("SELECT * FROM uktz")
    fun getAllUKTZ(): Flow<List<UktzItem>>

    /**
     * Видаляє запис УКТЗЕД за ідентифікатором.
     */
    @Query("DELETE FROM uktz WHERE id IS :id")
    suspend fun deleteUktz(id: Int)

    /**
     * Видаляє запис УКТЗЕД за числовим значенням коду.
     */
    @Query("DELETE FROM uktz WHERE pdvValue IS :uktzValue")
    suspend fun deleteUktzFromVal(uktzValue: String)

    /**
     * Вставляє або оновлює код УКТЗЕД.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUktz(uktzItem: UktzItem)

    // ------------------------------------------------------------------------------------------------
    // ОПЕРАЦІЇ ЗІ СТАВКАМИ ПДВ ТА АКЦИЗУ (PDV)
    // ------------------------------------------------------------------------------------------------

    /**
     * Отримує потік списку податкових груп ПДВ.
     */
    @Query("SELECT * FROM pdv")
    fun getAllPdv(): Flow<List<PdvItem>>

    /**
     * Зберігає податкову ставку.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdv(pdvItem: PdvItem)

    /**
     * Очищує таблицю податкових ставок.
     */
    @Query("DELETE FROM pdv")
    suspend fun deleteAllPdv()

    // ------------------------------------------------------------------------------------------------
    // ОПЕРАЦІЇ З ТОВАРНИМИ ГРУПАМИ (GROUP)
    // ------------------------------------------------------------------------------------------------

    /**
     * Отримує потік списку товарних груп.
     */
    @Query("SELECT * FROM `group`")
    fun getAllGroup(): Flow<List<GroupItem>>

    /**
     * Видаляє групу за ID.
     */
    @Query("DELETE FROM `group` WHERE id IS :id")
    suspend fun deleteGroup(id: Int)

    /**
     * Видаляє групу за її номером.
     */
    @Query("DELETE FROM `group` WHERE groupValue IS :valG")
    suspend fun deleteGroupFromVal(valG: String)

    /**
     * Вставляє або оновлює товарну групу.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(groupItem: GroupItem)
}