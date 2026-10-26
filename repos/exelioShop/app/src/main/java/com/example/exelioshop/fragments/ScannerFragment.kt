package com.example.exelioshop.fragments


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.example.exelioshop.EditItem
import com.example.exelioshop.MainApp
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.entities.ArticleList
import com.example.exelioshop.fragments.BaseFragment
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView


/**
 * =================================================================================================
 * ScannerFragment — Фрагмент оптичного сканування штрихкодів для додавання/редагування товарів.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Використовує бібліотеку ZBar для сканування штрихкоду товару через камеру.
 * Після успішного розпізнавання відкриває екран EditItem із заповненим полем barCode1,
 * що дозволяє касиру швидко завести новий товар за лічені секунди.
 * =================================================================================================
 */
class ScannerFragment : BaseFragment(), ZBarScannerView.ResultHandler {
    /** Видошукач сканера ZBar */
    private lateinit var zbView: ZBarScannerView
    val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }

    /** Лаунчер переходу до форми редагування товару EditItem */
    lateinit var editLauncher: ActivityResultLauncher<Intent>

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onEditResult()
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
        fun newInstance() = ScannerFragment()
    }

    /**
     * Обробка розпізнаного камерою штрихкоду:
     * Формує заготовку ArticleList з підставленим штрихкодом та відкриває екран EditItem
     */
    override fun handleResult(result: Result?) {
        val article = ArticleList(
            null, "", "", "", "", "", "", "", "", "",
            result?.contents.toString(), "", "", "", "", "", ""
        )
        val intent = Intent(activity, EditItem::class.java).apply {
            putExtra("to_edit", article)
        }
        editLauncher.launch(intent)
        Toast.makeText(activity, "${result?.contents.toString()}", Toast.LENGTH_SHORT).show()

        // Затримка 2 секунди перед відновленням перегляду камери для уникнення зависань
        val handler = Handler()
        handler.postDelayed({ zbView.resumeCameraPreview(this) }, 2000)
        zbView.setAutoFocus(true)
    }

    /**
     * Обробка збереження товару в EditItem: передача в чергу відправки до РРО
     */
    private fun onEditResult() {
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val itemToKasa: Article.ArticleData =
                    it.data?.getSerializableExtra("articleToSend") as Article.ArticleData
                Log.d("MyLog", "editR")
                dataModelM.articleNew.value = itemToKasa
            }
        }
    }
}