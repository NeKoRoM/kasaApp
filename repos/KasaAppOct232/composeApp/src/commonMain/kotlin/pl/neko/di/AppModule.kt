package pl.neko.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import pl.neko.database.AppDatabase
import pl.neko.di.DatabaseFactory

@Module
class AppModule {
@Single
fun provideRoomDatabase(): AppDatabase {

    return DatabaseFactory().createRoomDatabase()

}



}


