package com.example.exelioshop.fragments.fragmentsscan


import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.exelioshop.MainApp
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.fragments.BaseFragment
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView


/**
 * =================================================================================================
 * ScannerFragmentScan — Оптичний сканер штрихкодів через камеру в режимі продажу на касі.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Працює у верхній половині екрана продажу (TwoFragment).
 * При розпізнаванні штрихкоду передає значення у dataModelM.barCode, звідки нижній фрагмент
 * (LogFragment) автоматично знаходить товар у локальній базі, додає ціну до чека та передає команду в РРО.
 * =================================================================================================
 */
class ScannerFragmentScan : BaseFragment(), ZBarScannerView.ResultHandler {
    /** Видошукач ZBar */
    private lateinit var zbView: ZBarScannerView
    val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }

    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        zbView = ZBarScannerView(activity)
        zbView.setAutoFocus(true)
        return zbView
    }

    override fun onResume() {
        super.onResume()
        zbView.setResultHandler(this)
        zbView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        zbView.stopCamera()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScannerFragmentScan()
    }

    /**
     * Обробка розпізнаного штрихкоду: публікація в LiveData для підрахунку у чеку продажу
     */
    override fun handleResult(result: Result?) {
        dataModelM.barCode.value = result?.contents.toString()
        Toast.makeText(activity, "${result?.contents.toString()}", Toast.LENGTH_SHORT).show()

        // Затримка 2 секунди перед відновленням перегляду камери
        val handler = Handler()
        handler.postDelayed({ zbView.resumeCameraPreview(this) }, 2000)
        zbView.setAutoFocus(true)
    }
}