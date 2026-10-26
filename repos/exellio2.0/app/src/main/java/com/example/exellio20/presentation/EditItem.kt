package com.example.exellio20.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.exellio20.R
import com.example.exellio20.databinding.ActivityEditItemBinding
import com.example.exellio20.models.SpinnerData
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import dagger.hilt.android.AndroidEntryPoint

// =========================================================================================
// Константи налаштувань видимості полів картки товару (SharedPreferences)
// Дозволяють кастомізувати інтерфейс введення товару під конкретний бізнес (спрощений/повний)
// =========================================================================================

const val APP_EDIT_ITEM_PREFERENCES = "APP_EDIT_ITEM_PREFERENCES"
const val APP_EDIT_ITEM_SECTION = "APP_EDIT_ITEM_SECTION"
const val APP_EDIT_ITEM_TYPE = "APP_EDIT_ITEM_TYPE"
const val APP_EDIT_ITEM_SALES_COUNT = "APP_EDIT_ITEM_SALES_COUNT"
const val APP_EDIT_ITEM_SALES_AMOUNT = "APP_EDIT_ITEM_SALES_AMOUNT"
const val APP_EDIT_ITEM_REMAINDER = "APP_EDIT_ITEM_REMAINDER"
const val APP_EDIT_ITEM_BAR2 = "APP_EDIT_ITEM_BAR2"
const val APP_EDIT_ITEM_BAR3 = "APP_EDIT_ITEM_BAR3"
const val APP_EDIT_ITEM_BAR4 = "APP_EDIT_ITEM_BAR4"
const val APP_EDIT_ITEM_UKTZ = "APP_EDIT_ITEM_UKTZ"
const val APP_EDIT_ITEM_GROUP = "APP_EDIT_ITEM_GROUP"
const val APP_EDIT_ITEM_PDV = "APP_EDIT_ITEM_PDV"

/**
 * [EditItem] — активність створення та редагування картки товару (артикулу/PLU) для РРО Exellio/Datecs.
 * 
 * Відповідає за:
 * - Введення та валідацію фіскальних реквізитів товару:
 *   - PLU/Код (перевірка унікальності в БД, автоінкремент)
 *   - Назва (ліміт до 52 символів згідно з фіскальними вимогами друку чеків)
 *   - Ціна (до 2 знаків після коми, підтримка вільної/програмованої ціни)
 *   - Податкова група ПДВ (ставка від 1 до 8)
 *   - Товарна група
 *   - Код УКТЗЕД (10-значний код для підакцизних товарів)
 *   - Одиниця вимірювання (штуки, кг, літри, метри, грами)
 *   - Ознака вагового товару
 *   - До 4 штрихкодів (з можливістю сканування через камеру [ScanerActivity])
 * - Динамічне налаштування видимості полів на основі конфігурації в SharedPreferences
 * - Передачу сформованого об'єкта [ArticleItem] назад у викликаючий компонент через Intent
 */
@AndroidEntryPoint
class EditItem : AppCompatActivity() {
    lateinit var binding: ActivityEditItemBinding
    private var barCodeLauncher: ActivityResultLauncher<Intent>? = null
    private var bar2CodeLauncher: ActivityResultLauncher<Intent>? = null
    private var bar3CodeLauncher: ActivityResultLauncher<Intent>? = null
    private var articleItem: ArticleItem? = null

    private val vm : BluetoothDBViewModel by viewModels()


    private lateinit var preferencesEditItem: SharedPreferences

    private var isAllETOK = 0
    //private val spinnerList = listOf(SpinnerData("так", "1"), SpinnerData("ні", "0"))
    private val spinnerYNList = listOf(SpinnerData("так", "1"), SpinnerData("ні", "0"))
    private val spinnerTypePriceList = listOf(SpinnerData("програмована ціна", "0"), SpinnerData("вільна ціна", "1"), SpinnerData("програмована або менше", "2"))
    private val spinnerPISList = listOf(SpinnerData("штуки", "0"), SpinnerData("кілограми", "1"),SpinnerData("метри", "2"),SpinnerData("літри", "3"),SpinnerData("грами", "4"))

    private var uktzData = mutableListOf<UktzItem>(UktzItem(0,"",""))
    private var groupData = mutableListOf<GroupItem>(GroupItem(0,"","1"))//listOf<UktzItem>()
    private var pdvData = mutableListOf<PdvItem>()
    private val allArticleItem = mutableListOf<ArticleItem>()



    private var unitValue = "1"
    var PISValue = "0"
    private var pdvValue = "1"
    private var groupValue = "1"
    private var typeValue = "0"


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesEditItem = getSharedPreferences(
            com.example.exellio20.presentation.fragments.APP_EDIT_ITEM_PREFERENCES,
            Context.MODE_PRIVATE
        )
        super.onCreate(savedInstanceState)

        binding = ActivityEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Спостереження за даними
        observe()
        // 2. Отримання товару з Intent (якщо режим редагування)
        getArticle()
        // 3. Валідація введення в реальному часі (ціна, назва, код)
        checkWriting()
        // 4. Ініціалізація випадаючих списків (ПДВ, групи, УКТЗЕД, типи)
        adaptersInit()
        // 5. Застосування налаштувань видимості полів
        visibilityState()
        // 6. Реєстрація лаунчерів оптичного сканера для штрихкодів 1-3
        barCodesLaunchers()
        // 7. Прив'язка кнопок сканування штрихкодів
        barCodesButtons()

        binding.apply {
            // Кнопка підтвердження та збереження товару
            buttonSendArticle.setOnClickListener {
                var value = binding.editPrice.text.toString()
                if (value.isEmpty()) {
                    value = "0.00"
                } else {
                    val decimalIndex = value.indexOf('.')
                    if (decimalIndex == -1) {
                        value += ".00"
                    } else {
                        val decimals = value.substring(decimalIndex + 1)
                        if (decimals.length == 1) {
                            value += "0"
                        } else if (decimals.isEmpty()) {
                            value += "00"
                        }
                    }
                }
                binding.editPrice.setText(value)

                // Перевірка заповненості обов'язкових полів: Код, Назва, Ціна та відсутність помилок валідації
                if (editCode.text.toString() != "" &&
                    editName.text.toString() != "" &&
                    editPrice.text.toString() != "" &&
                    isAllETOK == 0
                ) {
                    val article = ArticleItem(
                        id = editCode.text.toString().toInt(),
                        TAX = pdvValue,
                        section = editSection.text.toString(),
                        group = groupValue,
                        type = typeValue,
                        price = editPrice.text.toString(),
                        salesAmount = editSalesAmount.text.toString(),
                        salesCount = editSalesCount.text.toString(),
                        remainder = editRemainder.text.toString(),
                        barCode1 = editBar1.text.toString(),
                        barCode2 = editBar2.text.toString(),
                        barCode3 = editBar3.text.toString(),
                        barCode4 = editBar4.text.toString(),
                        name = editName.text.toString(),
                        weightOrUnit = unitValue,
                        UKTZED = editUKTZED.text.toString(),
                        unitOfMeasurement = PISValue
                    )
                    // Повертаємо сформований артикул у викликаючу активність/фрагмент
                    val editIntent = Intent().apply {
                        putExtra("articleToSend", article)
                    }
                    setResult(RESULT_OK, editIntent)
                    finish()
                } else {
                    Toastmsg()
                }
            }
        }
    }

    /**
     * Прив'язка кнопок сканування штрихкодів через камеру пристрою.
     */
    private fun barCodesButtons() = with(binding) {
        buttonBar.setOnClickListener {
            barCodeLauncher?.launch(Intent(this@EditItem, ScanerActivity::class.java))
        }
        buttonBar2.setOnClickListener {
            bar2CodeLauncher?.launch(Intent(this@EditItem, ScanerActivity::class.java))
        }
        buttonBar3.setOnClickListener {
            bar3CodeLauncher?.launch(Intent(this@EditItem, ScanerActivity::class.java))
        }
    }

    /**
     * Реєстрація лаунчерів результату від [ScanerActivity] для 3 додаткових штрихкодів товару.
     */
    private fun barCodesLaunchers() {
        barCodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    Toast.makeText(this, "$barCod", Toast.LENGTH_SHORT).show()
                    binding.editBar1.setText(barCod.toString())
                }
            }
        bar2CodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    Toast.makeText(this, "$barCod", Toast.LENGTH_SHORT).show()
                    binding.editBar2.setText(barCod.toString())
                }
            }
        bar3CodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    Toast.makeText(this, "$barCod", Toast.LENGTH_SHORT).show()
                    binding.editBar3.setText(barCod.toString())
                }
            }
    }

    private fun observe() {
    }

    /**
     * Динамічне налаштування видимості полів введення на формі
     * відповідно до збережених у SharedPreferences налаштувань.
     */
    private fun visibilityState() = with(binding) {
        // Залишок товару
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_REMAINDER, false)) {
            remainder.visibility = View.GONE
            editRemainder.visibility = View.GONE
        } else {
            remainder.visibility = View.VISIBLE
            editRemainder.visibility = View.VISIBLE
        }
        // Відділ / секція
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_SECTION, false)) {
            section.visibility = View.GONE
            editSection.visibility = View.GONE
        } else {
            section.visibility = View.VISIBLE
            editSection.visibility = View.VISIBLE
        }
        // Тип ціни (фіксована/вільна)
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_TYPE, false)) {
            type.visibility = View.GONE
            editType.visibility = View.GONE
            spinnerType.visibility = View.GONE
        } else {
            type.visibility = View.VISIBLE
            spinnerType.visibility = View.VISIBLE
        }
        // Додатковий штрихкод 2
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR2, false)) {
            bar2.visibility = View.GONE
            editBar2.visibility = View.GONE
            buttonBar2.visibility = View.GONE
        } else {
            bar2.visibility = View.VISIBLE
            editBar2.visibility = View.VISIBLE
            buttonBar2.visibility = View.VISIBLE
        }
        // Додатковий штрихкод 3
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR3, false)) {
            bar3.visibility = View.GONE
            editBar3.visibility = View.GONE
            buttonBar3.visibility = View.GONE
        } else {
            bar3.visibility = View.VISIBLE
            editBar3.visibility = View.VISIBLE
            buttonBar3.visibility = View.VISIBLE
        }
        // Додатковий штрихкод 4
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR4, false)) {
            bar4.visibility = View.GONE
            editBar4.visibility = View.GONE
        } else {
            bar4.visibility = View.VISIBLE
            editBar4.visibility = View.VISIBLE
        }
        // Кількість продажів
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_SALES_COUNT, false)) {
            salesCount.visibility = View.GONE
            editSalesCount.visibility = View.GONE
        } else {
            salesCount.visibility = View.VISIBLE
            editSalesCount.visibility = View.VISIBLE
        }
        // Сума продажів
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_SALES_AMOUNT, false)) {
            salesAmount.visibility = View.GONE
            editSalesAmount.visibility = View.GONE
        } else {
            salesAmount.visibility = View.VISIBLE
            editSalesAmount.visibility = View.VISIBLE
        }
        // Код УКТЗЕД
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_UKTZ, false)) {
            editUKTZED.visibility = View.GONE
            spinnerUktz.visibility = View.GONE
            UKTZED.visibility = View.GONE
        } else {
            editUKTZED.visibility = View.VISIBLE
            spinnerUktz.visibility = View.VISIBLE
            UKTZED.visibility = View.VISIBLE
        }
        // Товарна група
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_GROUP, false)) {
            spinnerGroup.visibility = View.GONE
            group.visibility = View.GONE
        } else {
            spinnerGroup.visibility = View.VISIBLE
            group.visibility = View.VISIBLE
        }
        // Податкова ставка (ПДВ)
        if (preferencesEditItem.getBoolean(APP_EDIT_ITEM_PDV, false)) {
            spinnerPdv.visibility = View.GONE
            TAX.visibility = View.GONE
        } else {
            spinnerPdv.visibility = View.VISIBLE
            TAX.visibility = View.VISIBLE
        }
    }
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

            }

        }
        /////
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

                PISValue= spinnerPISList[position].containsValue
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }


        val adaterType = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            spinnerTypePriceList
        )

        binding.spinnerType.adapter = adaterType
        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                typeValue= spinnerTypePriceList[position].containsValue
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        ///////

        val adaterUKTZ = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            uktzData
        )
        binding.spinnerUktz.adapter= adaterUKTZ
        binding.spinnerUktz.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if(uktzData[position].pdvValue == ""){
                    binding.editUKTZED.setText(articleItem?.UKTZED)

                }else{
                binding.editUKTZED.setText(uktzData[position].pdvValue)}



            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        vm.allUktz.observe(this){
            uktzData.clear()
            uktzData.add(UktzItem(0,"",""))
            uktzData.addAll(it.sortedBy { it.pdvValue })
            adaterUKTZ.notifyDataSetChanged()
            val indexUktz = uktzData.indexOfFirst { it.pdvValue == articleItem?.UKTZED }
            if (indexUktz != -1){
                binding.spinnerUktz.post(Runnable { binding.spinnerUktz.setSelection(indexUktz) })
            }
        }


        val adaterGroup = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            groupData
        )

        binding.spinnerGroup.adapter= adaterGroup
        binding.spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                /*if(uktzData[position].pdvValue == ""){
                    binding.editUKTZED.setText(articleItem?.UKTZED)

                }else{
                    binding.editUKTZED.setText(uktzData[position].pdvValue)}*/
                if(groupData.isNotEmpty()) {
                    groupValue = groupData[position].groupValue
                }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        vm.allGroup.observe(this){
            groupData.clear()
           // groupData.add(GroupItem(0,"","1"))
            groupData.addAll(it.sortedBy { it.groupValue.toInt() })
            if (groupData.isEmpty()){
                vm.insertGroup(GroupItem(0,"","1"))
            }else{

            adaterGroup.notifyDataSetChanged()
            val indexGroup = groupData.indexOfFirst { it.groupValue == articleItem?.group }
            if (indexGroup != -1){
                binding.spinnerGroup.post(Runnable { binding.spinnerGroup.setSelection(indexGroup) })
            }}
        }




        val adaterPdv = ArrayAdapter(
            this,
            R.layout.simple_spinner_my,
            android.R.id.text1,
            pdvData
        )
        binding.spinnerPdv.adapter= adaterPdv
        binding.spinnerPdv.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                pdvValue = pdvData[position].id.toString()
                //Toast.makeText(this@EditItem, "${pdvData[position].id.toString()}", Toast.LENGTH_SHORT).show()

                /*if(uktzData[position].pdvValue == ""){
                    binding.editUKTZED.setText(articleItem?.UKTZED)

                }else{
                    binding.editUKTZED.setText(uktzData[position].pdvValue)}*/



            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
        vm.allPdv.observe(this){
            pdvData.clear()
            pdvData.addAll(it)
            adaterPdv.notifyDataSetChanged()
            val indexPdv = pdvData.indexOfFirst { it.id.toString() == articleItem?.TAX }
            //Toast.makeText(this, "${indexPdv }--${pdvData.size} ", Toast.LENGTH_SHORT).show()
            if (indexPdv != -1){
                binding.spinnerPdv.post(Runnable { binding.spinnerPdv.setSelection(indexPdv) })
            }
//            val indexUktz = uktzData.indexOfFirst { it.pdvValue == articleItem?.UKTZED }
//            if (indexUktz != -1){
//                binding.spinnerUktz.post(Runnable { binding.spinnerUktz.setSelection(indexUktz) })
//            }else{
//                binding.spinnerUktz.post(Runnable { binding.spinnerUktz.setSelection(0) })
//            }
        }

    }

    /**
     * Налаштування валідації текстових полів введення в реальному часі (до та після зміни тексту):
     * - `editCode`: Перевіряє наявність коду в БД; якщо знайдено дублікат — підсвічує червоним і попереджає через Toast.
     *   Якщо поле порожнє — пропонує наступний вільний PLU (`allArticleItem.last().id + 1`).
     * - `editName`: Обмежує довжину назви 52 символами (фіскальний ліміт довжини рядка в чеку Datecs/Exellio).
     * - `editPrice`: Перевіряє формат ціни за регулярним виразом `^\d{0,8}(\.\d{0,2})?$`.
     *   При втраті фокусу додає відсутні нулі після коми (`.00` або `0`).
     * - `editUKTZED`: Обмежує довжину коду УКТЗЕД до 10 цифр.
     * - `editGroup`: Обмежує номер групи двома знаками.
     */
    private fun checkWriting() = with(binding) {
        // Спостереження за списком артикулів для автопідстановки наступного PLU та контролю унікальності
        vm.allArticle.observe(this@EditItem) {
            allArticleItem.clear()
            allArticleItem.addAll(it)
            if (editCode.text.toString() == "" || editCode.text.toString() == "0") {
                val nextCode = if (allArticleItem.isNotEmpty()) {
                    allArticleItem.last().id + 1
                } else {
                    1
                }
                editCode.setText(nextCode.toString())
            }
        }

        // Перевірка коду товару на унікальність
        editCode.doAfterTextChanged {
            if (editCode.text.toString() != "") {
                val indexArt =
                    allArticleItem.indexOfFirst { it.id == editCode.text.toString().toInt() }
                if (indexArt != -1) {
                    val toast = Toast.makeText(this@EditItem, "дана позиція існує", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP, 0, 100)
                    toast.show()
                    editCode.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                } else {
                    editCode.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                }
            }
        }

        // Контроль довжини назви товару (максимум 52 символи)
        editName.doAfterTextChanged {
            if (editName.text.toString().length > 52) {
                editName.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                isAllETOK = 1
            } else {
                editName.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                isAllETOK = 0
            }
        }

        // Контроль формату ціни під час введення
        editPrice.doAfterTextChanged {
            val pattern = Regex("""^\d{0,8}(\.\d{0,2})?$""")
            if (!pattern.matches(editPrice.text.toString())) {
                editPrice.setText("0")
            }
        }

        // Форматування ціни до 2 знаків після коми при втраті фокусу (наприклад, 10 -> 10.00)
        binding.editPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                var value = binding.editPrice.text.toString()
                if (value.isEmpty()) {
                    value = "0.00"
                } else {
                    val decimalIndex = value.indexOf('.')
                    if (decimalIndex == -1) {
                        value += ".00"
                    } else {
                        val decimals = value.substring(decimalIndex + 1)
                        if (decimals.length == 1) {
                            value += "0"
                        } else if (decimals.isEmpty()) {
                            value += "00"
                        }
                    }
                }
                binding.editPrice.setText(value)
            }
        }

        // Контроль довжини коду УКТЗЕД (до 10 цифр)
        editUKTZED.doAfterTextChanged {
            if (editUKTZED.text.toString().length > 10) {
                editUKTZED.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                isAllETOK = 1
            } else {
                editUKTZED.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                isAllETOK = 0
            }
        }

        // Контроль довжини номера товарної групи (макс 2 цифри)
        editGroup.doAfterTextChanged {
            if (editGroup.text.toString().length > 2) {
                editGroup.setTextColor(ContextCompat.getColor(this@EditItem, R.color.red))
                editGroup.setText(editGroup.text.toString().substring(0, 2))
                isAllETOK = 1
            } else {
                editGroup.setTextColor(ContextCompat.getColor(this@EditItem, R.color.green))
                isAllETOK = 0
            }
        }
    }

    fun Toastmsg() {
        Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
    }

    /**
     * Отримання об'єкта товару з Intent для редагування та заповнення полів форми.
     */
    private fun getArticle() {
        articleItem = intent.getSerializableExtra("to_edit") as? ArticleItem
        fillArticle()
    }

    /**
     * Заповнення UI полів та випадаючих списків (Spinner) даними з об'єкта [articleItem].
     */
    private fun fillArticle() = with(binding) {
        if (articleItem != null && articleItem?.id != null) {
            editName.setText(articleItem!!.name)
            editBar1.setText(articleItem!!.barCode1)
            editBar2.setText(articleItem!!.barCode2)
            editBar3.setText(articleItem!!.barCode3)
            editBar4.setText(articleItem!!.barCode4)
            editCode.setText(articleItem!!.id.toString())
            editGroup.setText(articleItem!!.group)
            val indexGroup = groupData.indexOfFirst { it.groupValue == articleItem!!.group }
            if (indexGroup != -1) {
                spinnerPdv.post(Runnable { spinnerPdv.setSelection(indexGroup) })

                //editTAX.setText(articleItem!!.TAX)
                val indexPdv = pdvData.indexOfFirst { it.id.toString() == articleItem!!.TAX }
                if (indexPdv != -1) {
                    spinnerPdv.post(Runnable { spinnerPdv.setSelection(indexPdv) })
                } else {
                    spinnerPdv.post(Runnable { spinnerPdv.setSelection(0) })
                }
                editPrice.setText(articleItem!!.price)
                //editProduktInSet.setText(articleList!!.productInSet)
                editRemainder.setText(articleItem!!.remainder)
                editSalesAmount.setText(articleItem!!.salesAmount)
                editSalesCount.setText(articleItem!!.salesCount)
                editSection.setText(articleItem!!.section)


                editType.setText(articleItem!!.type)
                when (articleItem!!.type) {
                    "0" -> spinnerType.post(Runnable { spinnerType.setSelection(0) })
                    "1" -> spinnerType.post(Runnable { spinnerType.setSelection(1) })
                    "2" -> spinnerType.post(Runnable { spinnerType.setSelection(2) })


                }

                val indexUktz = uktzData.indexOfFirst { it.pdvValue == articleItem!!.UKTZED }
                if (indexUktz != -1) {
                    spinnerUktz.post(Runnable { spinnerUktz.setSelection(indexUktz) })
                }
                editUKTZED.setText(articleItem!!.UKTZED)



                when (articleItem!!.unitOfMeasurement) {
                    "0" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(0) })
                    "1" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(1) })
                    "2" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(2) })
                    "3" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(3) })
                    "4" -> spinnerProductInSet.post(Runnable { spinnerProductInSet.setSelection(4) })

                }
                if (articleItem!!.weightOrUnit == "1") {
                    spinnerUnit.post(Runnable { spinnerUnit.setSelection(0) })
                } else if (articleItem!!.weightOrUnit == "0") {
                    spinnerUnit.post(Runnable { spinnerUnit.setSelection(1) })
                } else {
                }
                editCode.keyListener = null


                //editUnit.setText(articleList!!.unit)
            } else if (articleItem != null && articleItem?.id == null) {
                editBar1.setText(articleItem!!.barCode1)
            } else {


            }
            editPrice.setText(articleItem!!.price)


        }else{


        }
    }
}
