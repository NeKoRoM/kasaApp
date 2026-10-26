package com.example.exelioshop

import android.app.Application
import com.example.exelioshop.db.MainDataBase

/**
 * [MainApp] — головний клас Application для застосунку ExelioShop.
 * 
 * Відповідає за глобальний життєвий цикл процесу Android та ліниву (lazy) ініціалізацію
 * єдиного екземпляра локальної бази даних Room [MainDataBase].
 */
class MainApp : Application() {
    /**
     * Лінива ініціалізація бази даних Room при першому зверненні.
     */
    val database by lazy {
        MainDataBase.getDatabase(this)
    }
}