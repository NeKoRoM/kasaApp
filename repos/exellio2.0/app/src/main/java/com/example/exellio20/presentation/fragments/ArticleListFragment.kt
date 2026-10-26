package com.example.exellio20.presentation.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exellio20.R
import com.example.exellio20.databinding.FragmentArticleListBinding
import com.example.exellio20.presentation.APP_BLUETOOTH_PREFERENCES
import com.example.exellio20.presentation.APP_LOGIN_PREFERENCES
import com.example.exellio20.presentation.BluetoothDBViewModel
import com.example.exellio20.presentation.EditItem
import com.example.exellio20.presentation.LoginActivity
import com.example.exellio20.presentation.PREF_CONST_MAC
import com.example.exellio20.presentation.PREF_LOGIN_EMAIL
import com.example.exellio20.presentation.PREF_LOGIN_PASSWORD
import com.example.exellio20.presentation.ScanerActivity
import com.example.exellio20.presentation.adapters.ArticleAdapter
import com.example.exellio20.presentation.adapters.UserActionListener
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.UktzItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [ArticleListFragment] — центральний екран керування базою товарів (артикулів/PLU) у фіскальному РРО Datecs/Exellio.
 * 
 * Ключові фіскальні та апаратні функції:
 * - Відображення повного списку запрограмованих товарів із локальної Room БД з підтримкою швидкого пошуку.
 * - Пошук та редагування товару за допомогою оптичного сканування штрихкоду через камеру ([ScanerActivity]).
 * - Автоматична синхронізація структури каси: виявлення невідомих товарних груп та кодів УКТЗЕД з пам'яті РРО
 *   та автоматичне додавання їх у локальну базу даних ([addSpinersVal]).
 * - Контроль захисту касового апарата: заборона редагування та видалення товарів, які брали участь у продажах
 *   поточної зміни, без попереднього зняття фіскального Z-звіту (помилка "товар в обігу").
 * - Контроль відповідності фізичної каси: перевірка заводського номера підключеного РРО із збереженим
 *   в обліковому записі касира (при заміні апарата ініціюється автоматичний вихід та перереєстрація).
 * - Повне вивантаження бази товарів із пам'яті каси через Bluetooth-потік ([BluetoothDBViewModel.startReadKasaDB]).
 */
@AndroidEntryPoint
class ArticleListFragment : BaseFragment(), UserActionListener {
    lateinit var binding: FragmentArticleListBinding
    private lateinit var articleAdapter: ArticleAdapter
    private var barCodeLauncher: ActivityResultLauncher<Intent>? = null

    // Множини для відстеження нових товарних груп та кодів УКТЗЕД з пам'яті РРО
    val mutableSetGroupInMemory = mutableSetOf<String>()
    val mutableSetGroupInKasa = mutableSetOf<String>()

    val mutableSetUktzInMemory = mutableSetOf<String>()
    val mutableSetUktzInKasa = mutableSetOf<String>()

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    private lateinit var preferencesLogin: SharedPreferences
    private lateinit var preferencesMac: SharedPreferences

    private val vm: BluetoothDBViewModel by activityViewModels()

    var fabVisible = false
    var stateFragIsStart = false

    var jobProcessBar: Job? = null
    var barCodScaned = "no"

    lateinit var editLauncher: ActivityResultLauncher<Intent>
    var allArticle = mutableListOf<ArticleItem>()

    override fun onClickNew() {
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentArticleListBinding.inflate(layoutInflater, container, false)

        stateFragIsStart = false
        // 1. Ініціалізація списку RecyclerView та пошукового рядка
        initRcView()
        // 2. Ініціалізація налаштувань SharedPreferences
        initSharedPreferences()
        // 3. Підключення спостерігачів за подіями РРО та даними БД
        observer()
        // 4. Реєстрація лаунчера екрана створення/редагування товару
        onEditResult()
        // 5. Ініціалізація розгортання плаваючої кнопки дій (FAB)
        fabInit()
        // 6. Обробники додавання нового товару та повторного зчитування БД каси
        onClickNewItem()
        onClickRepeatItem()
        // 7. Обробка системної кнопки "Назад"
        onBackPress()
        // 8. Реєстрація сканера штрихкодів через камеру
        barCodesLauncher()

        return binding.root
    }
    /**
     * Реєстрація запуску камери-сканера [ScanerActivity] для швидкого пошуку товару за штрихкодом.
     * Після сканування шукає відповідний [ArticleItem] серед 4 полів штрихкодів товару
     * та ініціює відкриття на редагування.
     */
    private fun barCodesLauncher() {
        barCodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    barCodScaned = barCod.toString()

                    // Пошук товару за будь-яким із 4 запрограмованих штрихкодів
                    val findArticleList = allArticle.filter { item ->
                        item.barCode1 == barCodScaned ||
                                item.barCode2 == barCodScaned ||
                                item.barCode3 == barCodScaned ||
                                item.barCode4 == barCodScaned
                    }
                    val findArticle =
                        if (findArticleList.isEmpty())
                            ArticleItem(barCode1 = barCodScaned)
                        else findArticleList.last()

                    if (findArticleList.isNotEmpty()) {
                        progresBar500L()
                        stateFragIsStart = true
                        articleG = findArticle
                        // Перевіряємо зв'язок та ідентифікатор каси
                        vm.sendCommand("00??IDnumber\t\t\t")
                    } else {
                        Toast.makeText(activity, "штрихкод не існує!!!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        binding.findWithBarCode.setOnClickListener {
            barCodeLauncher?.launch(Intent(activity, ScanerActivity::class.java))
        }
    }

    private fun initSharedPreferences() {
        preferencesLogin =
            requireActivity().applicationContext.getSharedPreferences(APP_LOGIN_PREFERENCES, Context.MODE_PRIVATE)
        preferencesMac =
            requireActivity().applicationContext.getSharedPreferences(APP_BLUETOOTH_PREFERENCES, Context.MODE_PRIVATE)
    }

    /**
     * Повернення до базового сканера при натисканні системної кнопки "Назад".
     */
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FragmentManager.setFragment(ScannerBaseFragment.newInstance(), activity as AppCompatActivity)
            }
        })
    }

    /**
     * Ініціалізація списку [RecyclerView] та налаштування текстового фільтра для миттєвого пошуку.
     */
    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(activity)
        articleAdapter = ArticleAdapter(this@ArticleListFragment)
        rcView.adapter = articleAdapter

        binding.searchView.setOnClickListener {
            searchView.isIconified = false
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                articleAdapter.filter.filter(newText)
                return true
            }
        })
    }

    var articleG: ArticleItem? = null
    var newStateButton = false





    /**
     * Спостереження за реактивними станами ViewModel та подіями фіскального реєстратора:
     * - `articleSold`: обробка помилки "товар в обігу" — повідомлення касиру про необхідність зняття Z-звіту.
     * - `btProblemMSG` / `btNotAnswer`: обробка обриву зв'язку чи відсутності каси в режимі програмування.
     * - `idKasa`: верифікація фізичного підключеного апарата. Якщо каса інша — автоматичний логаут і перехід до [LoginActivity].
     *   Якщо все вірно — запуск редактора товару [EditItem].
     * - `allArticle`: оновлення списку товарів у [ArticleAdapter] та виявлення нових товарних груп і кодів УКТЗЕД.
     * - `allGroup` / `allUktz`: оновлення локальних наборів довідників для подальшої синхронізації.
     */
    private fun observer() {
        // Помилка спроби редагування/видалення товару, який вже фігурував у чеках поточної зміни
        vm.articleSold.observe(viewLifecycleOwner) {
            if (it == "sold") {
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("товар в обігу")
                builder.setMessage("перед редагуванням зробіть z звіт")
                builder.setNeutralButton("зрозуміло") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
                vm.setArticleSold("")
            }
        }

        // Помилка обриву Bluetooth-зв'язку
        vm.btProblemMSG.observe(viewLifecycleOwner) {
            if (stateFragIsStart) {
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("Нема відповіді")
                builder.setMessage("виникли проблеми зі зєднанням")
                builder.setNeutralButton("bluetooth") { dialog, _ ->
                    vm.startBtActivity.value = "No_Answer"
                    dialog.dismiss()
                }
                builder.setPositiveButton("ВІДКРИТИ ДЛЯ ПЕРЕГЛЯДУ") { _, _ ->
                    if (articleG != null) {
                        val intent = Intent(activity, EditItem::class.java).apply {
                            putExtra("to_edit", articleG)
                        }
                        editLauncher.launch(intent)
                    } else {
                        editLauncher.launch(Intent(activity, EditItem::class.java))
                    }
                }
                builder.setNegativeButton("ЗАКРИТИ") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
                stateFragIsStart = true
            }
        }

        // Помилка: каса не переведена в режим програмування або не відповідає
        vm.btNotAnswer.observe(viewLifecycleOwner) {
            if (stateFragIsStart) {
                val builder = AlertDialog.Builder(activity)
                if (it == "No_Answer") {
                    builder.setTitle("Нема відповіді")
                    builder.setMessage("каса не в режимі програмування або виникли проблеми зі зєднанням")
                    builder.setPositiveButton("ВІДКРИТИ ДЛЯ ПЕРЕГЛЯДУ") { _, _ ->
                        if (articleG != null) {
                            val intent = Intent(activity, EditItem::class.java).apply {
                                putExtra("to_edit", articleG)
                            }
                            editLauncher.launch(intent)
                        } else {
                            editLauncher.launch(Intent(activity, EditItem::class.java))
                        }
                    }
                    builder.setNegativeButton("ЗАКРИТИ") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()
                    stateFragIsStart = true
                }
            }
        }

        // Отримання ідентифікатора фізичного РРО та валідація прив'язки облікового запису
        vm.idKasa.observe(viewLifecycleOwner) {
            Log.d("MyLogOnClick", "iswork")
            // Перевірка: чи відповідає підключена каса збереженому логіну (заводському номеру)
            if (!preferencesLogin.getString(PREF_LOGIN_EMAIL, "").toString().contains(it.lowercase().replace("000", ""))) {
                Log.d("MyLogKasaIdCheck", "${preferencesLogin.getString(PREF_LOGIN_EMAIL, "")}")
                vm.btConnect(preferencesMac.getString(PREF_CONST_MAC, "").toString())
                preferencesLogin.edit().putString(PREF_LOGIN_PASSWORD, "").apply()
                preferencesLogin.edit().putString(PREF_LOGIN_EMAIL, "").apply()
                Firebase.auth.signOut()
                val i = Intent(activity, LoginActivity::class.java).apply {
                    putExtra("to_new_kasa", "to_new_kasa")
                }
                startActivity(i)
            }

            // Відкриття екрана редагування/створення товару
            if (articleG != null) {
                val intent = Intent(activity, EditItem::class.java).apply {
                    putExtra("to_edit", articleG)
                }
                editLauncher.launch(intent)
                articleG = null
            } else if (newStateButton) {
                editLauncher.launch(Intent(activity, EditItem::class.java))
                newStateButton = false
            }
        }

        // Спостереження за повним списком товарів із Room БД
        vm.allArticle.observe(viewLifecycleOwner) {
            allArticle.clear()
            allArticle.addAll(it)
            progresBar500L()
            articleAdapter.submitList(it)
            if (it.isNotEmpty()) {
                mutableSetGroupInKasa.add(it.last().group)
                mutableSetUktzInKasa.add(it.last().UKTZED)
            }
        }

        // Спостереження за збереженими товарними групами
        vm.allGroup.observe(viewLifecycleOwner) {
            for (group in it) {
                mutableSetGroupInMemory.add(group.groupValue)
            }
        }

        // Спостереження за збереженими кодами УКТЗЕД
        vm.allUktz.observe(viewLifecycleOwner) {
            for (uktz in it) {
                mutableSetUktzInMemory.add(uktz.pdvValue)
            }
        }
    }

    /**
     * Відображення індикатора прогресу з автоматичним приховуванням через 500 мс
     * та запуском процедури синхронізації списків довідників [addSpinersVal].
     */
    fun progresBar500L() {
        binding.progressBar.visibility = View.VISIBLE
        jobProcessBar?.cancel()
        jobProcessBar = CoroutineScope(Dispatchers.IO).launch {
            delay(500L)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                addSpinersVal()
            }
        }
    }

    /**
     * Анімація розгортання та згортання плаваючого меню швидких дій (FAB):
     * - Створення нового товару
     * - Повторне зчитування всієї бази з апарата (stream PLU)
     * - Сканування штрихкоду через камеру
     */
    private fun fabInit() = with(binding) {
        fabVisible = false

        idFABAdd.setOnClickListener {
            if (!fabVisible) {
                idFABNewArticle.show()
                reapeatDB.show()

                idFABNewArticle.visibility = View.VISIBLE
                reapeatDB.visibility = View.VISIBLE
                findWithBarCode.visibility = View.VISIBLE

                idFABAdd.setImageDrawable(resources.getDrawable(R.drawable.ic_close))
                fabVisible = true
            } else {
                idFABNewArticle.hide()
                reapeatDB.hide()

                idFABNewArticle.visibility = View.GONE
                reapeatDB.visibility = View.GONE
                findWithBarCode.visibility = View.GONE

                idFABAdd.setImageDrawable(resources.getDrawable(R.drawable.ic_add))
                fabVisible = false
            }
        }
    }

    /**
     * Автоматична реєстрація нових груп та кодів УКТЗЕД у БД:
     * Порівнює множини значень, отриманих із пам'яті РРО, із локальними даними в Room.
     * Якщо виявлено нові коди, яких ще немає у довідниках застосунку — додає їх.
     */
    fun addSpinersVal() {
        for (group in mutableSetGroupInKasa.subtract(mutableSetGroupInMemory)) {
            vm.insertGroup(GroupItem(0, "", group, false))
        }

        for (uktz in mutableSetUktzInKasa.subtract(mutableSetUktzInMemory)) {
            vm.insertUktz(UktzItem(0, "", uktz))
        }
    }

    /**
     * Вибір товару для редагування:
     * - Зберігає посилання на товар у `articleG`
     * - Запитує заводський номер каси командою `"00??IDnumber\t\t\t"` для верифікації апарата
     * - Запускає синхронізацію довідників
     */
    override fun onItemClick(article: ArticleItem) {
        progresBar500L()
        stateFragIsStart = true
        articleG = article
        vm.sendCommand("00??IDnumber\t\t\t")
        Log.d("MyLog", "artList: send idNumber")
        addSpinersVal()
    }

    /**
     * Натискання кнопки додавання нового товару:
     * - Встановлює прапорець `newStateButton = true`
     * - Перевіряє зв'язок із РРО командою `"00??IDnumber\t\t\t"` перед відкриттям екрана [EditItem]
     */
    private fun onClickNewItem() = with(binding) {
        addSpinersVal()
        binding.idFABNewArticle.setOnClickListener {
            progresBar500L()
            articleG = null
            newStateButton = true
            stateFragIsStart = true
            vm.sendCommand("00??IDnumber\t\t\t")
        }
    }

    /**
     * Натискання кнопки повторного зчитування всієї бази артикулів із пам'яті каси.
     * Надсилає запит заводського номера, після чого викликає [BluetoothDBViewModel.startReadKasaDB].
     */
    private fun onClickRepeatItem() = with(binding) {
        reapeatDB.setOnClickListener {
            stateFragIsStart = true
            articleG = null
            vm.sendCommand("00??IDnumber\t\t\t")
            coroutineScope.launch {
                delay(200L)
                vm.startReadKasaDB()
            }
        }
    }

    /**
     * Обробка результату редагування/створення товару з [EditItem]:
     * Отримує об'єкт [ArticleItem] та записує його в апаратну пам'ять РРО через [BluetoothDBViewModel.sendArticleCode].
     */
    private fun onEditResult() {
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val itemToSend: ArticleItem =
                    it.data?.getSerializableExtra("articleToSend") as ArticleItem

                // Запис оновленого або нового товару в пам'ять фіскального реєстратора
                vm.sendArticleCode(itemToSend)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ArticleListFragment()
    }
}