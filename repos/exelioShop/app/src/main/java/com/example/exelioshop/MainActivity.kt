package com.example.exelioshop


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import com.example.exelioshop.bluetooth.BtConnection
import com.example.exelioshop.bluetooth.ReceiveThread
import com.example.exelioshop.bluetooth.SelectDevice
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.classes.VMData
import com.example.exelioshop.databinding.ActivityMainBinding
import com.example.exelioshop.dataclass.ListItem
import com.example.exelioshop.domain.models.LogInf
import com.example.exelioshop.entities.ArticleList
import com.example.exelioshop.fragments.BDFragment
import com.example.exelioshop.fragments.FragmentSettings
import com.example.exelioshop.fragments.ScannerFragment
import com.example.exelioshop.fragments.fragmentsscan.BDFragmentScan
import com.example.exelioshop.fragments.fragmentsscan.FragmentSettingsScan
import com.example.exelioshop.fragments.fragmentsscan.TwoFragment
import com.example.exelioshop.presentation.usecase.CheckPermissionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ключ сховища налаштувань SharedPreferences додатку */
const val APP_PREFERENCES = "APP_PREFERENCES"
/** Ключ для збереження останньої успішно підключеної MAC-адреси Bluetooth */
const val PREF_CONST_VALUE = "PREF_CONST_VALUE"

/**
 * =================================================================================================
 * MainActivity — Головний екран та координатор POS-додатку exelioShop.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Реалізує двосторонній інтерфейс обміну з РРО Datecs (ReceiveThread.Listener) та контроль зв'язку (IsConnectListener).
 * 2. Підтримує перемикання між двома основними режимами роботи за допомогою перемикача у тулбарі (Switch):
 *    - Режим сканування/продажу (TwoFragment / BDFragmentScan / FragmentSettingsScan);
 *    - Режим конфігурації/бази товарів (ScannerFragment / BDFragment / FragmentSettings).
 * 3. Виконує покрокове зчитування таблиці артикулів (PLU) з фіскальної пам'яті через команду 0x44 (kasaFirstCode, kasaNext).
 * 4. Записує та верифікує заголовки чеків (кліше з 8 рядків).
 * 5. Обробляє таймаути протоколу (try_to_fix, !!!!!).
 * =================================================================================================
 */
class MainActivity : AppCompatActivity(), ReceiveThread.Listener, BtConnection.IsConnectListener {
    /** ViewModel базового стану для зв'язку між компонентами */
    private val dataModel: VMData by viewModels()
    /** Головна ViewModel з доступом до бази Room */
    private val dataModelM: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((this.applicationContext as MainApp).database)
    }
    /** UseCase перевірки дозволів Android (Bluetooth, Location, Camera) */
    private val checkPermissionUseCase = CheckPermissionUseCase()

    /** Лаунчер запиту множинних дозволів */
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    lateinit var binding: ActivityMainBinding
    /** Контролер Bluetooth-з'єднання */
    lateinit var btConnection: BtConnection
    /** Лаунчер вибору Bluetooth-пристрою */
    private lateinit var actListLauncher: ActivityResultLauncher<Intent>
    /** Лаунчер редагування товару */
    lateinit var editLauncher: ActivityResultLauncher<Intent>

    /** Обраний Bluetooth-пристрій */
    private var listItem: ListItem? = null

    /** Ідентифікатор останнього отриманого коду артикулу для контролю послідовності */
    var lastCodeInt: Int = 300
    /** Новий код артикулу для запису */
    var newIemCodeInt: Int = 300
    /** Очікуваний символ послідовності команди запиту наступного артикулу */
    var kpCodeChar: Char = '*'
    /** Очікуваний символ послідовності запису товару */
    var kwCodeChar: Char = '*'
    /** Очікуваний символ послідовності для зчитування рядка кліше чека */
    var headingCodeChar: Char = '*'
    /** ID товару, що наразі записується */
    var newItemId = "*"
    /** Поточний номер рядка заголовка чека (0..7) */
    var headingNum = 0
    /** Буфер для 8 рядків заголовка чека */
    val listHeading = mutableListOf<String>()

    /** Останній розпарсений артикул для виявлення кінця списку */
    lateinit var articleLast: Article
    /** Множина вивантажених з РРО товарів */
    var articleList: MutableSet<Article.ArticleData> = mutableSetOf()

    /** Статус активного підключення до РРО */
    var isConnect = false

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

        // Ініціалізація реєстрації лаунчерів та Bluetooth-сервісу
        onBtListResult()
        init()
        oberveNewItem()
        checkPermission()
        observeSettings()
        observeToKasaScan()
        setBottomNavListener(false)
        dataModelM.listHeadingToSet.observe(this) {
            setHeading(it)
        }
        logMess()

        // Початкове завантаження головного фрагмента продажів TwoFragment
        com.example.exelioshop.fragments.FragmentManager.setFragment(
            TwoFragment.newInstance(),
            this
        )
        super.onPostResume()
    }

    /**
     * Спостереження за журналом логів: відправка довільної команди в РРО та додавання в історію
     */
    private fun logMess() {
        dataModel.logmess.observe(this, {
            btConnection.sendMessage(it)
            dataModel.logItem.value = LogInf("send: $it")
        })
    }

    /**
     * Спостереження за командами конфігурації каси (налаштування заголовків/параметрів)
     */
    private fun observeSettings() {
        dataModelM.settingToKasa.observe(this) {
            btConnection.sendMessage(it)

            // Якщо передано команду налаштування заголовка
            if (it.toString().contains(getString(R.string.headingCommand))) {
                headingCodeChar = '1'
                headingNum += 1
            }
        }
    }

    /**
     * Асинхронне встановлення рядків кліше (заголовка чека) з інтервалом 100 мс між рядками
     */
    fun setHeading(list: List<String>) = CoroutineScope(Dispatchers.Default).launch {
        for ((index, value) in list.withIndex()) {
            btConnection.sendMessage("Header\t$index\t$value\t")
            delay(100L)
        }
    }

    /**
     * Спостереження за обраним товаром для сканування: надсилання штрихкоду у скануючий тракт РРО
     */
    private fun observeToKasaScan() {
        dataModelM.articleToScan.observe(this) {
            if ((it.barCode1.isNotEmpty() && it.barCode1 != "0") && (it.price != "0.00")) {
                btConnection.sendMessegeBar(it.barCode1.toString())
                Toast.makeText(this@MainActivity, "send${it.barCode1}", Toast.LENGTH_SHORT).show()
            } else if (it.price == "0.00") {
                Toast.makeText(this@MainActivity, "price isEmpty", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "barCod isEmpty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Спостереження за новим або зміненим артикулом: формування фіскальної команди запису PLU (006;P)
     */
    private fun oberveNewItem() {
        dataModelM.articleNew.observe(this, { itemToSend ->
            Toast.makeText(this, " ${itemToSend.name}", Toast.LENGTH_SHORT).show()
            // Формат команди Datecs: 006;P \t код \t ПДВ \t відділ \t група \t тип \t ціна \t\t залишок \t ШК1..4 \t назва \t од.вим \t УКТЗЕД \t штучний/ваговий \t ENQ
            val itemTokasaS = "006;P\t${itemToSend.code}\t${itemToSend.TAX}\t${itemToSend.section}\t${itemToSend.group}\t${itemToSend.type}\t${itemToSend.price}\t\t${itemToSend.remainder}\t${itemToSend.barCode1}\t${itemToSend.barCode2}\t${itemToSend.barCode3}\t${itemToSend.barCode4}\t${itemToSend.name}\t${itemToSend.unit}\t${itemToSend.UKTZED}\t${itemToSend.productInSet}\t\u0005"
            Log.d("myLog", itemTokasaS)
            btConnection.sendMessage(itemTokasaS)
            newIemCodeInt = btConnection.com.id
            newItemId = itemToSend.code
        })
    }

    /**
     * Ініціалізація Bluetooth-адаптера та автопідключення за збереженою адресою
     */
    private fun init() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter
        btConnection = BtConnection(btAdapter, this, this)
        if (preferences.getString(PREF_CONST_VALUE, "").toString() != "") {
            btConnection.connect(preferences.getString(PREF_CONST_VALUE, "").toString())
        }
    }

    /**
     * Створення верхнього меню дій: вибір пристрою, читання бази, та перемикач режимів (Switch)
     */
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.control_menu, menu)
        val mySwitch = menu.findItem(R.id.app_bar_switch).actionView as Switch
        mySwitch.setOnClickListener {
            if (mySwitch.isChecked) {
                // Перемикання в режим роботи з базою даних товарів
                com.example.exelioshop.fragments.FragmentManager.setFragment(
                    ScannerFragment.newInstance(),
                    this
                )
                super.onPostResume()
                setTitle(getString(R.string.modePk))
            } else {
                // Перемикання в режим швидкого продажу (sales mode)
                com.example.exelioshop.fragments.FragmentManager.setFragment(
                    TwoFragment.newInstance(),
                    this
                )
                setTitle("sales mode")
            }
            binding.BNav.selectedItemId = (R.id.bar_mode)
            setBottomNavListener(mySwitch.isChecked)
        }
        mySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            setBottomNavListener(isChecked)
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Обробка вибору пунктів меню: відкриття пошуку пристроїв або запуск зчитування товарів
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.id_list) {
            actListLauncher.launch(Intent(this, SelectDevice::class.java))
            preferences.edit().putString(PREF_CONST_VALUE, "").apply()
        } else if (item.itemId == R.id.id_connect) {
            listItem.let {
                if (lastCodeInt == 300 && kpCodeChar == '*') {
                    // Запит першого артикулу з каси (ініціалізація вивантаження PLU)
                    btConnection.sendMessage(getString(R.string.kasaLast))
                    lastCodeInt = btConnection.com.id
                    btConnection.isConnect()
                } else {
                    Toast.makeText(this, "$lastCodeInt error lastCode", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Перевірка та запит необхідних системних дозволів
     */
    private fun checkPermission() {
        if (!checkPermissionUseCase.checkPermission(context = this)) {
            registerPermissionListener()
            launchBtPermission()
        }
    }
    /**
     * Запуск діалогу дозволів Android (BLUETOOTH_CONNECT для Android 12+, Location, Camera)
     */
    private fun launchBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    /**
     * Реєстрація зворотного виклику запиту системних дозволів
     */
    private fun registerPermissionListener() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
        }
    }

    /**
     * Обробка вибору пристрою з вікна SelectDevice: збереження адреси та ініціалізація зв'язку
     */
    private fun onBtListResult() {
        actListLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    listItem = it.data?.getSerializableExtra(SelectDevice.DEVICE_KEY) as ListItem
                    btConnection.connect(listItem?.mac!!)
                }
            }
    }

    /**
     * Налаштування слухача нижньої панелі BottomNavigationView:
     * - Якщо isChecked == true (режим бази): навігація між сканером, базою товарів та налаштуваннями РРО.
     * - Якщо isChecked == false (режим продажу): навігація між екраном чека (TwoFragment), каталогом для продажу та швидкими налаштуваннями.
     */
    private fun setBottomNavListener(isChecked: Boolean) {
        if (isChecked) {
            binding.BNav.setOnItemSelectedListener {
                super.onPostResume()
                when (it.itemId) {
                    R.id.bar_mode -> {
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            ScannerFragment.newInstance(),
                            this
                        )
                        Log.d("MyLog", "barBD")
                    }
                    R.id.bd_mode -> {
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            BDFragment.newInstance(),
                            this
                        )
                        Log.d("MyLog", "bdBD")
                    }
                    R.id.settings -> {
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            FragmentSettings.newInstance(),
                            this
                        )
                        Log.d("MyLog", "settBD")
                    }
                }
                true
            }
        } else {
            binding.BNav.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.bar_mode -> {
                        Log.d("MyLog", "bar")
                        binding.placeHolder1.marginBottom
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            TwoFragment.newInstance(),
                            this
                        )
                    }
                    R.id.bd_mode -> {
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            BDFragmentScan.newInstance(),
                            this
                        )
                    }
                    R.id.settings -> {
                        com.example.exelioshop.fragments.FragmentManager.setFragment(
                            FragmentSettingsScan.newInstance(),
                            this
                        )
                        Log.d("MyLog", "sett")
                    }
                }
                true
            }
        }
    }

    fun switchChange(isChecked: Boolean) {
    }

    /** Тимчасовий буфер для склеювання розірваних пакетів протоколу Datecs */
    var tempMSG = ""

    /**
     * Обробка склеєного пакету, розділеного на частини при читанні через сокет
     */
    fun twoMess(message: String) {
        dataModel.logItem.value = LogInf(message)
        if (message.length > 4) {
            readKasaBD(message)
            writeKasa(message)
            setTimeOnTV(message)
            readHeading(message)
            tempMSG = ""
        }
    }

    /**
     * Головний обробник вхідних повідомлень від Bluetooth-потоку РРО (ReceiveThread.Listener):
     * 1. Фільтрує та збирає повні кадри протоколу (від пробілу до символу ENQ / \u0005).
     * 2. Передає кадри на дешифрування: оновлення артикулу, читання бази, запис кліше, час каси.
     * 3. Реагує на watchdog-повідомлення "try_to_fix": запитує наступний артикул за номером (006;F).
     * 4. Реагує на аварійний сигнал "!!!!!": скидає всі індекси та маркери черги в початковий стан.
     */
    override fun onReceive(message: String) {
        runOnUiThread {
            if (message.first() == ' ' && message.contains("\u0005")) {
                dataModel.logItem.value = LogInf(message)
                if (message.length > 4) {
                    updateOneArticle(message)
                    readKasaBD(message)
                    writeKasa(message)
                    setTimeOnTV(message)
                    readHeading(message)
                }
            } else if (message.first() == ' ') {
                tempMSG = message
            } else if (message.contains("\u0005")) {
                twoMess(tempMSG + message)
            } else if (message == "try_to_fix" && tempMSG != "" && kpCodeChar != '*') {
                btConnection.sendMessage("006;F\t${articleList.last().code.toInt() + 1}\t")
                kpCodeChar = btConnection.com.id.toChar()
            }

            if (message == "!!!!!") {
                lastCodeInt = 300
                kpCodeChar = '*'
                kwCodeChar = '*'
                headingCodeChar = '*'
                headingNum = 0
                Log.d("MyLog", "0 all")
            }
        }
    }

    /**
     * Оновлення одного товару: після відправки запису (006;P) перевіряє квитанцію та запитує
     * перечитування (006;R), після чого оновлює сутність ArticleList у Room.
     */
    private fun updateOneArticle(message: String) {
        var tabs = 0
        for (i in message) {
            if (i.toString() == "\t")
                tabs += 1
        }

        if (newIemCodeInt != 300) {
            newIemCodeInt = 300
            // Запит на перечитування щойно записаного товару для верифікації
            btConnection.sendMessage("006;R\t$newItemId\t")
        } else if (newItemId != "*" && tabs == 18) {
            val article = Article(message)
            newItemId != "*"

            dataModel.articleList.value = Article(message).article
            // Збереження верифікованого товару в Room DB
            dataModelM.insertArticle(
                ArticleList(
                    id = article.article.code.toInt(),
                    name = article.article.name,
                    price = article.article.price,
                    TAX = article.article.TAX,
                    section = article.article.section,
                    group = article.article.group,
                    type = article.article.type,
                    salesAmount = article.article.salesAmount,
                    salesCount = article.article.salesCount,
                    remainder = article.article.remainder,
                    barCode1 = article.article.barCode1,
                    barCode2 = article.article.barCode2,
                    barCode3 = article.article.barCode3,
                    barCode4 = article.article.barCode4,
                    productInSet = article.article.productInSet,
                    unit = article.article.unit,
                    UKTZED = article.article.UKTZED
                )
            )
        }
    }

    /**
     * Обробка зміни стану підключення до фізичного пристрою
     */
    override fun onConnect(boolean: Boolean, mac: String) {
        runOnUiThread {
            if (boolean) {
                Toast.makeText(this, "yeah", Toast.LENGTH_SHORT).show()
            }
            // Збереження MAC-адреси для автопідключення при наступних запусках
            preferences.edit().putString(PREF_CONST_VALUE, mac).apply()
        }
    }

    /**
     * Покрокове зчитування 8 рядків заголовка (кліше) чека з пам'яті РРО
     */
    fun readHeading(message: String) {
        if (headingCodeChar != '*' && headingNum < 8) {
            var head = ""
            var fT: Int = 0
            var lT: Int = 0
            for ((index, value) in message.withIndex()) {
                if (value.toString() == "\t" && fT == 0)
                    fT = index
                else if (value.toString() == "\t" && lT == 0)
                    lT = index
            }
            for ((index, value) in message.withIndex()) {
                if (index > fT && index < lT)
                    head += value
            }
            listHeading.add(head)
            Log.d("header", listHeading.toString())

            // Запит наступного рядка заголовка
            btConnection.sendMessage("Header\t$headingNum\t\t")
            headingNum += 1

        } else if (headingNum == 8) {
            // Завершення зчитування всіх 8 рядків заголовка
            headingNum = 0
            headingCodeChar = '*'
            dataModelM.listHeading.value = listHeading
            listHeading.clear()
        }
    }

    /**
     * Виділення та форматування системного часу з відповіді РРО на команду запиту дати/часу (0x3E / '>')
     */
    private fun setTimeOnTV(message: String) {
        if ((message[4] == '>' && message[5] == 'P') ||
            (message[3] == '>' && message[4] == 'P')
        ) {
            var writeOrNo = false
            var timeKasa = ""
            for (n in message) {
                if (writeOrNo == true) {
                    timeKasa = timeKasa + n
                }
                if (n.toString() == "\t" && writeOrNo == false) {
                    writeOrNo = true
                } else if (n.toString() == "\t" && writeOrNo == true) {
                    writeOrNo = false
                }
            }
            dataModelM.timeToSmart.value = timeKasa
        }
    }

    private fun writeHeading(message: String) {
        if (headingCodeChar == message[2] || headingCodeChar == message[3]) {
        }
    }

    /**
     * Підтвердження успішного запису параметрів у касу
     */
    private fun writeKasa(message: String) {
        if (kwCodeChar != '*') {
            Toast.makeText(this, "write data to kasa", Toast.LENGTH_SHORT).show()
            kwCodeChar = '*'
        }
    }

    /**
     * Циклічне вивантаження всієї бази артикулів (PLU) з фіскального реєстратора в Room DB:
     * - Крок 1: Отримання останнього товару -> запит першого товару (kasaFirstCode).
     * - Крок 2: Отримання товару N -> збереження в Room -> запит наступного товару (kasaNext).
     * - Крок 3: Якщо отриманий товар збігається з articleLast -> кінець вивантаження, скидання прапорців.
     */
    private fun readKasaBD(message: String) {
        var tabs = 0
        for (i in message) {
            if (i.toString() == "\t")
                tabs += 1
        }
        Log.d("myLog", "$tabs")

        if ((message[0] == ' ' || message[1] == ' ') && tabs == 18) {
            val commandMsg = message
            if (commandMsg[5].toInt() == lastCodeInt || commandMsg[6].toInt() == lastCodeInt) {
                // Зафіксовано останній товар, запускаємо послідовний витяг з 1-го артикулу
                articleLast = Article(commandMsg)
                btConnection.sendMessage(getString(R.string.kasaFirstCode))
                kpCodeChar = btConnection.com.id.toChar()
                Toast.makeText(this, "$kpCodeChar", Toast.LENGTH_SHORT).show()
                lastCodeInt = 300
            } else if (commandMsg[5] == kpCodeChar || commandMsg[6] == kpCodeChar)
                if (articleLast.article != Article(commandMsg).article) {
                    // Збереження чергового артикулу та запит наступного
                    val article = Article(commandMsg)

                    articleList.add(Article(commandMsg).article)
                    dataModel.articleList.value = Article(commandMsg).article
                    dataModelM.insertArticle(
                        ArticleList(
                            id = article.article.code.toInt(),
                            name = article.article.name,
                            price = article.article.price,
                            TAX = article.article.TAX,
                            section = article.article.section,
                            group = article.article.group,
                            type = article.article.type,
                            salesAmount = article.article.salesAmount,
                            salesCount = article.article.salesCount,
                            remainder = article.article.remainder,
                            barCode1 = article.article.barCode1,
                            barCode2 = article.article.barCode2,
                            barCode3 = article.article.barCode3,
                            barCode4 = article.article.barCode4,
                            productInSet = article.article.productInSet,
                            unit = article.article.unit,
                            UKTZED = article.article.UKTZED
                        )
                    )

                    // Запит наступного товару з таблиці PLU
                    btConnection.sendMessage(getString(R.string.kasaNext))
                    kpCodeChar = btConnection.com.id.toChar()

                } else {
                    // Досягнуто кінець таблиці товарів
                    articleList.add(Article(commandMsg).article)
                    dataModel.articleList.value = Article(commandMsg).article
                    val article = Article(message)
                    dataModelM.insertArticle(
                        ArticleList(
                            id = article.article.code.toInt(),
                            name = article.article.name,
                            price = article.article.price,
                            TAX = article.article.TAX,
                            section = article.article.section,
                            group = article.article.group,
                            type = article.article.type,
                            salesAmount = article.article.salesAmount,
                            salesCount = article.article.salesCount,
                            remainder = article.article.remainder,
                            barCode1 = article.article.barCode1,
                            barCode2 = article.article.barCode2,
                            barCode3 = article.article.barCode3,
                            barCode4 = article.article.barCode4,
                            productInSet = article.article.productInSet,
                            unit = article.article.unit,
                            UKTZED = article.article.UKTZED
                        )
                    )

                    lastCodeInt = 300
                    kpCodeChar = '*'
                    articleList = mutableSetOf()
                }
        } else if (kpCodeChar != '*' && tabs != 18 && message.length > 25) {
            // Пропуск пошкодженого/неіснуючого артикулу переходом до наступного індексу (006;F)
            btConnection.sendMessage("006;F\t${articleList.last().code.toInt() + 1}\t")
            kpCodeChar = btConnection.com.id.toChar()
        }
    }

    fun writeArticle() {
    }

    /**
     * Сповіщення про статус з'єднання (IsConnectListener)
     */
    override fun isConn(isConn: Boolean) {
        isConnect = isConn
        if (!isConn) {
            Toast.makeText(this, "not Bluetooth", Toast.LENGTH_SHORT).show()
            lastCodeInt = 300
        }
    }

    override fun onResume() {
        super.onResume()
    }
}