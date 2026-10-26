package com.example.exellio20.roomDB

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem

/**
 * ====================================================================================================
 * ГОЛОВНА БАЗА ДАНИХ ROOM (MAIN DATABASE - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Центральний клас абстракції Room SQLite для додатку "exellio2.0".
 *
 * СХЕМА БАЗИ ТА ТАБЛИЦІ:
 * - [ArticleItem]: каталог товарів з усіма атрибутами касового апарату Exellio.
 * - [UktzItem]: довідник кодів УКТЗЕД для підакцизних груп.
 * - [PdvItem]: таблиця ставок податку ПДВ та акцизу.
 * - [GroupItem]: перелік товарних груп каси.
 *
 * МІГРАЦІЇ:
 * - Поточна версія: 6.
 * - Автоматична міграція між версіями 5 та 6 ([AutoMigration(from = 5, to = 6)]).
 */
@Database(
    entities = [
        ArticleItem::class,
        UktzItem::class,
        PdvItem::class,
        GroupItem::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 5, to = 6)]
)
abstract class MainDataBase : RoomDatabase() {

    /**
     * Повертає DAO інтерфейс доступу до всіх таблиць бази даних.
     */
    abstract fun getDao(): Dao
}