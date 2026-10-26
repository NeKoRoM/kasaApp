package com.example.exelioshop

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.databinding.ActivityEditItemBinding
import com.example.exelioshop.entities.ArticleList
import com.example.exelioshop.presentation.models.SpinnerData

/**
 * =================================================================================================
 * EditItem — Екран створення та детального редагування картки товару (PLU).
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Введення та валідація всіх фіскальних параметрів артикулу відповідно до вимог ДПС України:
 *    - Внутрішній код PLU (до 6 символів);
 *    - Найменування товару (до 52 символів для друку в чеку);
 *    - Роздрібна ціна (формат 8 знаків до коми, 2 після);
 *    - Податкова група ПДВ (TAX 1..8: А, Б, В, Г, Д, Е тощо);
 *    - Товарна група (01..99) та відділ (секція);
 *    - Код УКТЗЕД (до 10 цифр для підакцизних/пільгових товарів);
 *    - 4 додаткових штрихкоди (EAN-13, ITF-14);
 *    - Одиниця виміру (штуки, кг, метри, літри, грами);
 *    - Ознака вагового/штучного товару або товару в наборі.
 * 2. Підтримка швидкого сканування штрихкоду з камери через ScanerActivity.
 * 3. Повертає сформовану модель Article.ArticleData для подальшого запису в РРО Datecs та Room.
 * =================================================================================================
 */
class EditItem : AppCompatActivity() {
    lateinit var binding: ActivityEditItemBinding
    private var barCodeLauncher: ActivityResultLauncher<Intent>? = null
    private var articleList: ArticleList? = null
    private val dataModelM: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((this.applicationContext as MainApp).database)
    }
    /** Прапорець валідності полів вводу: 0 - все вірно, 1 - є помилки */
    private var isAllETOK = 0
    /** Спінер ознаки "так/ні" (акцизний/штучний) */
    private val spinnerYNList = listOf(SpinnerData("так", "1"), SpinnerData("ні", "0"))
    /** Спінер типів одиниць виміру */
    private val spinnerPISList = listOf(
        SpinnerData("штуки", "0"),
        SpinnerData("кілограми", "1"),
        SpinnerData("метри", "2"),
        SpinnerData("літри", "3"),
        SpinnerData("грами", "4")
    )

    private var unitValue = "1"
    var PISValue = "0"

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getArticle()
        checkWriting()
        adaptersInit()

        // Лаунчер для підстановки відсканованого штрихкоду у поле BarCode1
        barCodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    Toast.makeText(this, "$barCod", Toast.LENGTH_SHORT).show()
                    binding.editBar1.setText(barCod.toString())
                }
            }

        binding.apply() {
            // Кнопка відкриття оптичного сканера штрихкоду
            buttonBar.setOnClickListener {
                barCodeLauncher?.launch(Intent(this@EditItem, ScanerActivity::class.java))
            }

            // Кнопка збереження товару
            button2.setOnClickListener {
                if (editCode.text.toString() != "" &&
                    editName.text.toString() != "" &&
                    editPrice.text.toString() != "" &&
                    isAllETOK == 0
                ) {
                    val article: Article.ArticleData = Article.ArticleData(
                        code = editCode.text.toString(),
                        TAX = editTAX.text.toString(),
                        section = editSection.text.toString(),
                        group = editGroup.text.toString(),
                        type = editType.text.toString(),
                        price = editPrice.text.toString(),
                        salesAmount = editSalesAmount.text.toString(),
                        salesCount = editSalesCount.text.toString(),
                        remainder = editRemainder.text.toString(),
                        barCode1 = editBar1.text.toString(),
                        barCode2 = editBar2.text.toString(),
                        barCode3 = editBar3.text.toString(),
                        barCode4 = editBar4.text.toString(),
                        name = editName.text.toString(),
                        unit = unitValue.toString(),
                        UKTZED = editUKTZED.text.toString(),
                        productInSet = PISValue
                    )
                    val editIntent = Intent().apply {
                        putExtra("articleToSend", article)
                    }
                    setResult(RESULT_OK, editIntent)
                    finish()
                } else
                    Toastmsg()
            }
        }
    }

    /**
     * Ініціалізація адаптерів спінерів для вибору одиниці виміру та ознаки товару в наборі
     */
    private fun adaptersInit() {
        val adaterUnit = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            spinnerYNList
        )

        binding.spinnerUnit.adapter = adaterUnit
        binding.spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                unitValue = spinnerYNList[position].containsValue
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        val adaterPIS = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            spinnerPISList
        )

        binding.spinnerProductInSet.adapter = adaterPIS
        binding.spinnerProductInSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                PISValue = spinnerPISList[position].containsValue
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }

    private fun checkWriting() = with(binding) {

        //editPrice.visibility = View.INVISIBLE
       // button2.
        // Валідація довжини найменування товару (максимум 52 символи для Datecs)
        editName.doAfterTextChanged {
            if (editName.text.toString().length > 52) {
                editName.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                isAllETOK = 1
            } else {
                editName.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                isAllETOK = 0
            }
        }

        // Валідація ціни за регулярним виразом (до 8 цифр цілої частини, до 2 десяткових знаків)
        editPrice.doAfterTextChanged {
            val pattern = Regex("""^\d{0,8}(\.\d{0,2})?$""")
            editPrice.doAfterTextChanged {
                if (!pattern.matches(editPrice.text.toString())) {
                    editPrice.setText("0")
                }
            }

            // Валідація коду УКТЗЕД (до 10 цифр для фіскальної звітності)
            editUKTZED.doAfterTextChanged {
                if (editUKTZED.text.toString().length > 10) {
                    editUKTZED.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                    isAllETOK = 1
                } else {
                    editUKTZED.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                    isAllETOK = 0
                }
            }

            // Валідація коду PLU (максимум 6 цифр, діапазон 1..999999)
            editCode.doAfterTextChanged {
                if (editCode.text.toString().length > 6) {
                    editCode.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                    isAllETOK = 1
                } else {
                    editCode.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                    isAllETOK = 0
                }
            }

            // Валідація податкової групи ПДВ (діапазон 1..8)
            editTAX.doAfterTextChanged {
                if (editTAX.text.toString().toIntOrNull() in 1..8) {
                    editTAX.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                    isAllETOK = 0
                } else {
                    editTAX.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                    isAllETOK = 1
                }
            }

            // Валідація номера товарної групи (до 2 цифр)
            editGroup.doAfterTextChanged {
                if (editGroup.text.toString().length > 2) {
                    editGroup.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                    editGroup.setText(editGroup.text.toString().substring(0, 2))
                    editPrice.setSelection(editPrice.length())
                    isAllETOK = 1
                } else {
                    editGroup.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                    isAllETOK = 0
                }
            }
        }
    }

    fun Toastmsg() {
        Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
    }

    /**
     * Отримання сутності товару, переданої через Intent для редагування
     */
    private fun getArticle() {
        articleList = intent.getSerializableExtra("to_edit") as? ArticleList
        fillArticle()
    }

    /**
     * Заповнення екранних полів значеннями з ArticleList та вибір позицій у спінерах
     */
    private fun fillArticle() = with(binding) {
        if (articleList != null && articleList?.id != null) {
            editName.setText(articleList!!.name)
            editBar1.setText(articleList!!.barCode1)
            editBar2.setText(articleList!!.barCode2)
            editBar3.setText(articleList!!.barCode3)
            editBar4.setText(articleList!!.barCode4)
            editCode.setText(articleList!!.id.toString())
            editGroup.setText(articleList!!.group)
            editTAX.setText(articleList!!.TAX)
            editPrice.setText(articleList!!.price)
            editProduktInSet.setText(articleList!!.productInSet)
            editRemainder.setText(articleList!!.remainder)
            editSalesAmount.setText(articleList!!.salesAmount)
            editSalesCount.setText(articleList!!.salesCount)
            editSection.setText(articleList!!.section)
            editType.setText(articleList!!.type)
            editUKTZED.setText(articleList!!.UKTZED)

            when (articleList!!.productInSet) {
                "0" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(0) })
                "1" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(1) })
                "2" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(2) })
                "3" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(3) })
                "4" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(4) })
            }
            if (articleList!!.unit == "1") {
                spinnerUnit.post(Runnable { spinnerUnit.setSelection(0) })
            } else if (articleList!!.unit == "0") {
                spinnerUnit.post(Runnable { spinnerUnit.setSelection(1) })
            }

            // Заборона зміни первинного PLU-коду для вже існуючого товару
            editCode.keyListener = null

        } else if (articleList != null && articleList?.id == null) {
            editBar1.setText(articleList!!.barCode1)
        }
    }
}



