package com.example.exellio20.presentation.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.exellio20.R
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint
import me.dm7.barcodescanner.zbar.BarcodeFormat
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

/**
 * [ScannerCodeFragment] — дочірній фрагмент постійного оптичного сканування штрихкодів за допомогою камери.
 * 
 * Особливості роботи:
 * - Інтегрує [ZBarScannerView] з підтримкою автофокусу.
 * - Підтримує широкий перелік 1D/2D стандартів штрихкодів: EAN-13, EAN-8, Code 128, QR, DataBar тощо.
 * - При успішному розпізнаванні відтворює звуковий сигнал підтвердження (`R.raw.beep`).
 * - Передає штрихкод у ViewModel через [BluetoothDBViewModel.codeSend] для пошуку товару в базі або обробки вагового коду.
 * - Запобігає зависанню драйвера камери на застарілих пристроях шляхом затримки відновлення прев'ю (1000 мс).
 */
@AndroidEntryPoint
class ScannerCodeFragment : BaseFragment(), ZBarScannerView.ResultHandler {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var zbView: ZBarScannerView

    /** Список підтримуваних форматів штрихкодів */
    val formats: List<BarcodeFormat> = listOf(
        BarcodeFormat.CODE128,
        BarcodeFormat.CODABAR,
        BarcodeFormat.CODE39,
        BarcodeFormat.DATABAR,
        BarcodeFormat.DATABAR_EXP,
        BarcodeFormat.EAN13,
        BarcodeFormat.EAN8,
        BarcodeFormat.I25,
        BarcodeFormat.ISBN10,
        BarcodeFormat.ISBN13,
        BarcodeFormat.PARTIAL,
        BarcodeFormat.PDF417,
        BarcodeFormat.QRCODE,
        BarcodeFormat.UPCA,
        BarcodeFormat.UPCE
    )

    private val vm: BluetoothDBViewModel by activityViewModels()

    override fun onClickNew() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Ініціалізація аудіо-сигналу "beep" для зворотного зв'язку касиру
        mediaPlayer = MediaPlayer.create(activity, R.raw.beep)

        // Ініціалізація сканера ZBar із увімкненим автофокусом
        zbView = ZBarScannerView(activity)
        zbView.setAutoFocus(true)

        return zbView
    }

    override fun onResume() {
        super.onResume()
        zbView.setResultHandler(this)
        zbView.startCamera()
        zbView.setAutoFocus(true)
    }

    override fun onPause() {
        super.onPause()
        zbView.stopCamera()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScannerCodeFragment()
    }

    /**
     * Колбек успішного розпізнавання штрихкоду детектором ZBar.
     * 
     * @param result об'єкт розпізнавання зі значенням та типом штрихкоду
     */
    override fun handleResult(result: Result?) {
        val barcodeString = result?.contents.toString()

        // Відправляємо штрихкод у ViewModel для пошуку товару в локальній Room БД та касі
        vm.codeSend(barcodeString)

        Toast.makeText(activity, barcodeString, Toast.LENGTH_SHORT).show()
        Toast.makeText(activity, "${result?.barcodeFormat?.name}", Toast.LENGTH_SHORT).show()

        if (vm.startScanFrag.value == true) {
            vm.startListFrag.value = true
            vm.startScanFrag.value = false
        }

        // Програємо звук успішного сканування
        mediaPlayer?.start()

        // Відновлюємо сканування із затримкою 1 секунда для запобігання дублюванню та зависанню камери
        Handler().postDelayed({ zbView.resumeCameraPreview(this) }, 1000)
        zbView.setAutoFocus(true)
    }
}