package com.example.exelioshop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

/**
 * =================================================================================================
 * ScanerActivity — Повноекранне оптичне сканування штрихкодів через камеру смартфона (ZBar).
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Дозволяє швидко сканувати лінійні штрихкоди (EAN-13, Code128 тощо) або QR-коди акцизних марок
 * без підключення апаратного Bluetooth-сканера.
 * Повертає зчитаний рядок штрихкоду через ActivityResult з ключем "bar".
 * =================================================================================================
 */
class ScanerActivity : AppCompatActivity(), ZBarScannerView.ResultHandler {
    /** Представлення видошукача та розпізнавача ZBar */
    private lateinit var zbView: ZBarScannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        zbView = ZBarScannerView(this)
        setContentView(zbView)
    }

    /**
     * Активація камери та встановлення обробника результату при виході на передній план
     */
    override fun onResume() {
        super.onResume()
        zbView.setResultHandler(this)
        zbView.startCamera()
    }

    /**
     * Звільнення ресурсів камери при згортанні екрану
     */
    override fun onPause() {
        super.onPause()
        zbView.stopCamera()
    }

    /**
     * Обробка розпізнаного штрихкоду від бібліотеки ZBar:
     * Формування результату RESULT_OK з кодом у полі "bar" та закриття екрану
     */
    override fun handleResult(result: Result?) {
        val editIntent = Intent().apply {
            putExtra("bar", result?.contents)
        }
        setResult(RESULT_OK, editIntent)
        finish()
    }
}