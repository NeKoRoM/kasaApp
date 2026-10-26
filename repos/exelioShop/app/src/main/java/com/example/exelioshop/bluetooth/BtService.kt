package com.example.exelioshop.bluetooth

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import java.security.Provider

/**
 * =================================================================================================
 * BtService — Android Service для підтримки безперервної роботи Bluetooth у фоні.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Забезпечує життєвий цикл фонового процесу зв'язку з фіскальним реєстратором або сканером штрихкодів,
 * запобігаючи несподіваному перериванню сокету операційною системою при переході між екранами (Activities/Fragments).
 * =================================================================================================
 */
class BtService : Service() {
    /** Екземпляр менеджера Bluetooth-з'єднання */
    lateinit var btConnection: BtConnection

    /**
     * Викликається системою при кожному виклику startService(intent).
     * Повертає START_STICKY для автоматичного перезапуску служби системою у разі браку пам'яті.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Повідомлення користувачу про старт фонової служби зв'язку
        Toast.makeText(this, "start service", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    /**
     * Служба не підтримує зв'язування через IPC (Bind), зв'язок ведеться через спільні об'єкти в пам'яті.
     */
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    /**
     * Ін'єкція екземпляра менеджера з'єднання BtConnection у службу
     */
    fun setBT(btConnectionF: BtConnection) {
        btConnection = btConnectionF
    }
}