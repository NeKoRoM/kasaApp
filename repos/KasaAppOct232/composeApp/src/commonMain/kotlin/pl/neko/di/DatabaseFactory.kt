package pl.neko.di

import pl.neko.database.AppDatabase

expect class DatabaseFactory() {
    fun createRoomDatabase(): AppDatabase



}