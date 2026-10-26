package com.example.exelioshop.bluetooth

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.example.exelioshop20.bluetooth.BtConnection

/**
 * ====================================================================================================
 * ФОНОВИЙ СЕРВІС BLUETOOTH-З'ЄДНАННЯ (ANDROID SERVICE - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Android Service, призначений для утримання стабільного бездротового каналу зв'язку
 * з касовим апаратом Exellio у фоновому режимі, запобігаючи скиданню сокета при зміні орієнтації екрана.
 */
class BtService : Service() {
    lateinit var btConnection: BtConnection

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "start service", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Встановлює активний екземпляр контролера Bluetooth-з'єднання.
     */
    fun setBT(btConnectionF: BtConnection) {
        btConnection = btConnectionF
    }
}