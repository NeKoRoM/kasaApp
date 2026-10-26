package com.example.exellio20.presentation

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.*
import com.example.exelioshop.bluetooth.ReceiveThread
import com.example.exelioshop20.bluetooth.BtConnection
import com.example.exellio20.domain.usecases.IdKasaToStringUseCase
import com.example.exellio20.domain.usecases.MessageToArticleUseCase
import com.example.exellio20.domain.usecases.PdvMessageToListOfPdvUseCase
import com.example.exellio20.roomDB.MainDataBase
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import com.example.exellio20.roomDB.entities.PdvItem
import com.example.exellio20.roomDB.entities.UktzItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ====================================================================================================
 * ГОЛОВНА VIEWMODEL КЕРУВАННЯ РРО ТА БАЗОЮ ДАНИХ (BLUETOOTH & DB VIEWMODEL - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Центральний координатор бізнес-логіки та обміну даними між Android UI, базою даних Room
 * та касовим апаратом Datecs/Exellio:
 *
 * ОСНОВНІ ОБОВ'ЯЗКИ:
 * 1. Взаємодія з базою даних Room:
 *    - Реактивні потоки [LiveData] для товарів, груп, ставок ПДВ та кодів УКТЗЕД.
 *    - Асинхронні CRUD-операції в [viewModelScope].
 * 2. Управління апаратним з'єднанням Bluetooth:
 *    - Ініціалізація зв'язку [btConnect] та обробка стану сокета [BtConnection.ListenerCS].
 *    - Прийом відповідей від РРО через [ReceiveThread.Listener].
 * 3. Автомат станів протоколу Datecs (State Machine):
 *    - Покрокове читання бази товарів із пам'яті РРО (команди "006;L", "006;F", "006;N").
 *    - Програмування товарів у РРО ("006;P").
 *    - Збирання фрагментованих пакетів (понад 256 байт) за символом завершення `\u0004`.
 *    - Автоматичне відновлення зв'язку при збоях (сторожовий таймер "try_to_fix").
 *    - Обробка фіскальної помилки "006;-1030" (товар в обігу активної зміни).
 */
@HiltViewModel
class BluetoothDBViewModel @Inject constructor(
    val btConnection: BtConnection,
    database: MainDataBase
) : ViewModel(), ReceiveThread.Listener, BtConnection.ListenerCS {

    // ------------------------------------------------------------------------------------------------
    // РОБОТА З БАЗОЮ ДАНИХ ROOM (LIVEDATA ТА АСИНХРОННІ ОПЕРАЦІЇ)
    // ------------------------------------------------------------------------------------------------

    val dao = database.getDao()
    val allArticle: LiveData<List<ArticleItem>> = dao.getAllArticle().asLiveData()
    val withoutPrice0Article: LiveData<List<ArticleItem>> = dao.getWithoutNullArticle().asLiveData()
    val allUktz: LiveData<List<UktzItem>> = dao.getAllUKTZ().asLiveData()
    val allGroup: LiveData<List<GroupItem>> = dao.getAllGroup().asLiveData()
    val allPdv: LiveData<List<PdvItem>> = dao.getAllPdv().asLiveData()

    fun insertPdv(pdvItem: PdvItem) = viewModelScope.launch {
        dao.insertPdv(pdvItem)
    }

    fun deleteAllPdv() = viewModelScope.launch {
        dao.deleteAllPdv()
    }

    fun insertArticle(article: ArticleItem) = viewModelScope.launch {
        dao.insert(article)
    }

    fun deleteAllArticle() = viewModelScope.launch {
        dao.deleteAllArticle()
    }

    fun insertUktz(uktzItem: UktzItem) = viewModelScope.launch {
        dao.insertUktz(uktzItem)
    }

    fun deleteUktz(id: Int) = viewModelScope.launch {
        dao.deleteUktz(id)
    }

    fun deleteUktzFromVal(uktzValue: String) = viewModelScope.launch {
        dao.deleteUktzFromVal(uktzValue)
    }

    fun insertGroup(groupItem: GroupItem) = viewModelScope.launch {
        dao.deleteGroupFromVal(groupItem.groupValue)
        dao.insertGroup(groupItem)
    }

    fun deleteGroupFromVal(groupVal: String) = viewModelScope.launch {
        dao.deleteGroupFromVal(groupVal)
    }

    fun deleteGroup(id: Int) = viewModelScope.launch {
        dao.deleteGroup(id)
    }

    // ------------------------------------------------------------------------------------------------
    // СТАНИ BLUETOOTH ТА ЮЗКЕЙСИ ОБРОБКИ ПОВІДОМЛЕНЬ
    // ------------------------------------------------------------------------------------------------

    var articleList = mutableSetOf<ArticleItem>()

    init {
        btConnection.addLisener(this)
    }

    var bluetoothDevice: BluetoothDevice? = null
    val bluetoothAdapter = btConnection.adapter

    private val handler = Handler(Looper.getMainLooper())

    private val msgToPdv = PdvMessageToListOfPdvUseCase()
    private val msgToArt = MessageToArticleUseCase()
    private val msgToId = IdKasaToStringUseCase()

    private val _errorMsg = MutableLiveData<String>()
    val errorMsg: LiveData<String> = _errorMsg

    val _dateKasa = MutableLiveData<String>()
    val dateKasa: LiveData<String> = _dateKasa

    val startBtActivity: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val startScanFrag: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val startListFrag: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val _btProblemMSG = MutableLiveData<String>()
    val btProblemMSG: LiveData<String> = _btProblemMSG

    private val _btNotAnswer = MutableLiveData<String>()
    val btNotAnswer: LiveData<String> = _btNotAnswer

    private val _btMac = MutableLiveData<String>()
    val btMac: LiveData<String> = _btMac

    private val _articleSold = MutableLiveData<String>()
    val articleSold: LiveData<String> = _articleSold

    val _idKasa = MutableLiveData<String>()
    val idKasa: LiveData<String> = _idKasa

    private var lastReadArticle: ArticleItem? = null










    /**
     * Ініціює підключення до Bluetooth-пристрою за MAC-адресою [mac].
     */
    fun btConnect(mac: String) {
        btConnection.connect(mac, this)
        bluetoothDevice = btConnection.bluetoothDevice
        btConnection.btConnCheck()
        Log.d("MyLog", "btconn serv: ${btConnection.cThread.isConnect()}, ")
    }

    /**
     * Запускає процес вивантаження всієї номенклатури товарів із пам'яті РРО в локальну базу даних Room.
     * Відправляє команду "006;L\t100000\t" для запиту діапазону PLU від 1 до 100 000.
     * Очищує локальну таблицю [deleteAllArticle], готуючись до повного оновлення.
     */
    fun startReadKasaDB() {
        if (btConnection.connectState) {
            if (firstCodeASCISymbol == 0) {
                btConnection.sendMessage("006;L\t100000\t")
                lastCode = btConnection.com.id
                deleteAllArticle()
            }
        } else {
            Log.d("MyLog", "vm: btNotFound")
            _btProblemMSG.postValue("not_bluetooth")
        }
    }

    /**
     * Відправляє довільну текстову команду в касовий апарат через Bluetooth-з'єднання.
     */
    @Synchronized
    fun sendCommand(message: String) {
        if (btConnection.connectState) {
            btConnection.sendMessage(message)
        } else {
            Log.d("MyLog", "vm: btNotFound")
            _btProblemMSG.postValue("not_bluetooth")
            _errorMsg.value = message
        }
    }

    /**
     * Програмує/оновлює параметри товару в пам'яті РРО через протокольну команду "006;P" (Command 68 sub-op P).
     *
     * ФОРМАТ ПОЛІВ КОМАНДИ:
     * `006;P \t <PLU> \t <ПДВ> \t <Секція> \t <Група> \t <Тип> \t <Ціна> \t\t <Залишок> \t <ШК1> \t <ШК2> \t <ШК3> \t <ШК4> \t <Назва> \t <ОзнакаВаги> \t <УКТЗЕД> \t <ОдВиміру> \t <0x05>`
     */
    fun sendArticleCode(article: ArticleItem) {
        if (btConnection.connectState) {
            val message = "006;P\t${article.id}" +
                    "\t${article.TAX}" +
                    "\t${article.section}" +
                    "\t${article.group}" +
                    "\t${article.type}" +
                    "\t${article.price}" +
                    "\t\t${article.remainder}" +
                    "\t${article.barCode1}" +
                    "\t${article.barCode2}" +
                    "\t${article.barCode3}" +
                    "\t${article.barCode4}" +
                    "\t${article.name}" +
                    "\t${article.weightOrUnit}" +
                    "\t${article.UKTZED}" +
                    "\t${article.unitOfMeasurement}" +
                    "\t\u0005"
            btConnection.sendMessage(message)
            firstCodeASCISymbol = btConnection.com.id
            lastArticle = article
            readId = article.id.toString()
            Log.d("MyLogRead", "vm: $readId")
        } else {
            Log.d("MyLog", "vm: btNotFound")
            _btProblemMSG.postValue("not_bluetooth")
        }
    }

    /**
     * Відправляє штрихкод у форматі ручного введення зі сканера (з додаванням CR).
     */
    @Synchronized
    fun codeSend(message: String) {
        if (btConnection.connectState) {
            btConnection.sendMessageBar(message)
        } else {
            Log.d("MyLog", "vm: btNotFound")
            handler.post {
                _btProblemMSG.postValue("not_bluetooth")
            }
        }
    }

    private var tempMSG = ""

    fun setArticleSold(valStr: String) {
        _articleSold.value = valStr
    }

    /**
     * ГОЛОВНИЙ ОБРОБНИК ВХІДНИХ ПОВІДОМЛЕНЬ ВІД КАСОВОГО АПАРАТА (RECEIVE LISTENER)
     *
     * Обробляє фрагментацію пакетів, парсить табуляторні відповіді за кількістю розділювачів `\t`:
     * - 18 табуляцій: структура товару PLU ([readKasaDB]).
     * - 2 табуляції + "0040": фіскальний заводський номер РРО ([setKasaId]).
     * - 25 або 26 табуляцій: звіт про ставки ПДВ та акцизу (парсинг через [msgToPdv] та збереження в Room).
     * - Помилка "006;-1030": товар заблокований, оскільки був проданий у відкритій зміні.
     * - "try_to_fix": сторожове відновлення циклу читання при втраті пакету.
     */
    @Synchronized
    override fun onReceive(message: String) {
        handler.post {
            val subStr = "003>0"

            val byteArrayUtf8 = message.toByteArray(Charsets.UTF_8)
            val strUtf8 = String(byteArrayUtf8, Charsets.UTF_8)

            val pattern = Regex("(?i)${Regex.escape(subStr)}")
            val containsSubstring = pattern.containsMatchIn(message)

            Log.d("MyLog1", "vm: $message")
            var twoMess = ""

            var tabs = 0
            for (i in message) {
                if (i.toString() == "\t")
                    tabs += 1
            }

            if (message.length > 4) {
                if ((message.first() == ' ' || message[1] == ' ') && message.contains("\u0004")) {
                    if (tabs == 18) {
                        readKasaDB(message)
                    } else if (tabs == 2 && message.contains("0040", ignoreCase = true) && message.indexOf("00??0\tКР") != -1) {
                        Log.d("MyLog2", "vm: setKasaId ")
                        setKasaId(message)
                    } else if (tabs == 25) {
                        for (i in msgToPdv.execute(message = message)) {
                            insertPdv(i)
                        }
                    }
                } else if (message.first() == ' ' || message[1] == ' ') {
                    // Збереження першої половини фрагментованого пакету
                    tempMSG = message
                } else if (message.contains("\u0004") && tempMSG != "") {
                    // Збирання двох частин повідомлення разом
                    twoMess = tempMSG + message
                    var tabs2 = 0
                    for (i in twoMess) {
                        if (i.toString() == "\t")
                            tabs2 += 1
                    }
                    Log.d("MyLog1", "vm: 2 $tabs2")
                    Log.d("MyLog", "vm: 2msg=$twoMess")
                    if (tabs2 == 18) {
                        readKasaDB(twoMess)
                    } else if (tabs2 == 26) {
                        deleteAllPdv()
                        for (i in msgToPdv.execute(message = twoMess)) {
                            insertPdv(i)
                        }
                    }
                    tempMSG = ""
                } else if (message == "try_to_fix" && firstCodeASCISymbol != 0) {
                    // Сторожове відновлення: якщо пакет загубився, запитуємо наступний PLU
                    if (articleList.isNotEmpty()) {
                        btConnection.sendMessage("006;F\t${articleList.last().id + 1}\t")
                        firstCodeASCISymbol = btConnection.com.id
                        Log.i("MyLog_try_to_fix", "try_to_fix")
                    } else {
                        Log.i("MyLogContinue", "continue...false")
                    }
                } else if (message == "!!!!!") {
                    _btNotAnswer.postValue("No_Answer")
                    readId = "0"
                    lastCode = 0
                    firstCodeASCISymbol = 0
                } else if (message == "try_to_fix") {
                    // Порожній обробник
                } else if (message == "Broken pipe") {
                    _btProblemMSG.postValue("not_bluetooth")
                } else if (message == "not_bluetooth") {
                    _btProblemMSG.postValue("not_bluetooth")
                } else if (message == "btNotFound") {
                    _btProblemMSG.value = ""
                    _btProblemMSG.postValue("not_bluetooth")
                }

                // Вилучення дати та часу касового апарата
                if (message.indexOf(subStr) != -1) {
                    setKasaData(message)
                    Log.d("MyLog", dateKasa.value.toString())
                }

                // Помилка Datecs: товар заблокований через продаж у поточній зміні
                if (message.indexOf("006;-1030") != -1) {
                    Log.d("MyLogSold", "товар в обігу")
                    _articleSold.postValue("sold")
                }

                // Перевірка щойно записаного товару шляхом зворотного запиту ("006;F\t<id>\t")
                if (readId != "0") {
                    Log.d("MyLogRead", "vm: $readId !=0")
                    val toWrite = readId

                    viewModelScope.launch {
                        delay(100) // Пауза 100 мс перед підтвердженням
                        Log.i("MyLog", "100ms")
                        btConnection.sendMessage("006;F\t${toWrite}\t")
                        firstCodeASCISymbol = btConnection.com.id
                    }
                    readId = "0"
                    lastCode = 0
                    firstCodeASCISymbol = 0
                }
            }
        }
    }

    private fun setKasaData(message: String) {
        _dateKasa.value = msgToId.execute(message)
    }

    private fun setKasaId(message: String) {
        _idKasa.value = msgToId.execute(message)
    }

    /**
     * Автомат покрокового вивантаження товарів із пам'яті РРО:
     * - Крок 1: Запит першого товару ("006;F\t1\t").
     * - Крок 2: Запит кожного наступного товару ("006;N\t") та збереження в Room [insertArticle].
     * - Крок 3: При отриманні кінця каталогу (articleItem == lastArticle) завершує цикл та надсилає сигнал "0053".
     */
    private fun readKasaDB(message: String) {
        val articleItem = msgToArt.execute(message)

        if (lastCode != 0) {
            lastArticle = articleItem
            btConnection.sendMessage("006;F\t1\t")
            lastCode = 0
            firstCodeASCISymbol = btConnection.com.id
        } else if (firstCodeASCISymbol != 0 &&
            (message[5] == firstCodeASCISymbol.toChar() || message[6] == firstCodeASCISymbol.toChar())
            && articleItem.id != lastArticle?.id
        ) {
            btConnection.sendMessage("006;N\t")
            firstCodeASCISymbol = btConnection.com.id

            insertArticle(articleItem)
            articleList.add(articleItem)
        } else if (articleItem == lastArticle) {
            insertArticle(articleItem)
            articleList.add(articleItem)
            Log.i("MyLog", "last_Article")
            articleList.clear()
            firstCodeASCISymbol = 0
            lastArticle = null

            sendCommand("0053") // Звуковий сигнал РРО про завершення синхронізації
        }
    }

    override fun onConnect(boolean: Boolean, mac: String) {
        _btMac.postValue(mac)
        Log.d("MyLog", "vm: $mac")
    }

    companion object {
        var readId = "0"
        var lastCode = 0
        var firstCodeASCISymbol = 0
        var lastArticle: ArticleItem? = null
    }

    override fun onCheckBtState(boolean: Boolean) {
        if (!boolean) {
            _btProblemMSG.postValue("not_bluetooth_onCheckBtState")
        }
    }
}






    //end bt



