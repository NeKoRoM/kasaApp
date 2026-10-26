package pl.neko

import android.app.Application
import org.koin.android.ext.koin.androidContext
import pl.neko.di.initKoin

class KasaApp: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@KasaApp)
        }
    }
}