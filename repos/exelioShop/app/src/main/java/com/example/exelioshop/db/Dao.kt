package com.example.exelioshop.db

import androidx.room.*
import androidx.room.Dao
import com.example.exelioshop.entities.ArticleList
import kotlinx.coroutines.flow.Flow

/**
 * [Dao] — Data Access Object (DAO) для роботи з таблицею артикулів `list_name`.
 * 
 * Надає методи реактивного зчитування та асинхронного запису позицій каталогу товарів:
 * - [getAllArticle]: повертає [Flow] списку товарів для автоматичного оновлення UI при змінах
 * - [insertArticle]: асинхронний запис товару зі стратегією [OnConflictStrategy.REPLACE]
 */
@Dao
interface Dao {
    /**
     * Отримання повного переліку збережених товарів у вигляді реактивного потоку [Flow].
     */
    @Query("SELECT * FROM list_name")
    fun getAllArticle(): Flow<List<ArticleList>>

    /**
     * Додавання або оновлення товару (при збігу первинного ключа старий запис перезаписується).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleList)
}