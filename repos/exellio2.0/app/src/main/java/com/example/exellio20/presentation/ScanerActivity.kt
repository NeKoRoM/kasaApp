package com.example.exellio20.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

/**
 * [ScanerActivity] — екран оптичного сканування штрихкодів за допомогою вбудованої камери пристрою.
 * 
 * Використовує бібліотеку ZBar (`ZBarScannerView`) для швидкого розпізнавання 1D/2D штрихкодів
 * (EAN-13, Code 128, QR-коди тощо) при додаванні чи пошуку товарів у фіскальному застосунку.
 * Результат сканування повертається викликаючій Activity або фрагменту через стандартний
 * механізм Activity Result (`setResult(RESULT_OK, intent)` з ключем "bar").
 */
class ScanerActivity : AppCompatActivity(), ZBarScannerView.ResultHandler {
    /**
     * Повноекранний View компонент сканера ZBar, що керує прев'ю камери та детектором штрихкодів.
     */
    private lateinit var zbView: ZBarScannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ініціалізація сканера та встановлення його як головного контенту Activity
        zbView = ZBarScannerView(this)
        setContentView(zbView)
    }

    /**
     * При поверненні активності у фокус підключаємо обробник результатів та запускаємо камеру.
     */
    override fun onResume() {
        super.onResume()
        zbView.setResultHandler(this)
        zbView.startCamera()
    }

    /**
     * При переході у фоновий режим обов'язково зупиняємо камеру для звільнення апаратних ресурсів.
     */
    override fun onPause() {
        super.onPause()
        zbView.stopCamera()
    }

    /**
     * Колбек успішного розпізнавання штрихкоду детектором ZBar.
     * 
     * @param result об'єкт результату сканування з типом та текстовим змістом штрихкоду
     */
    override fun handleResult(result: Result?) {
        // Пакуємо розпізнаний штрихкод в Intent із ключем "bar"
        val editIntent = Intent().apply {
            putExtra("bar", result?.contents)
        }
        // Встановлюємо статус успіху та закриваємо екран сканування
        setResult(RESULT_OK, editIntent)
        finish()
    }
}