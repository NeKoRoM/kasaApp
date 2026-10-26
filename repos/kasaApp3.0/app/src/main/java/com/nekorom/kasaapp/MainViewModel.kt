// =================================================================================================
// ГОЛОВНА В'ЮМОДЕЛЬ POS-ТЕРМІНАЛУ: ШЛЮЗ РРО DATECS, ХМАРИ ТА ROOM DB (MAIN VIEWMODEL)
//
// Архітектурний шар: Presentation Layer (MVVM State Machine + Hilt ViewModel).
// Призначення:
// 1. Координує асинхронний обмін даними між апаратним контролером Bluetooth (BluetoothController),
//    репозиторієм протоколу РРО (BtKasaExellioRepository), локальною базою даних Room (Dao)
//    та хмарним бекендом авторизації / ліцензування (NetworkDataRepositoryImp / Firebase).
// 2. Збирає фрагментовані кадри Datecs (SOH \u0001 ... ETX \u0003), перевіряє порядкові номери пакетів,
//    десеріалізує товари, податкові ставки (ПДВ/акциз), заводські номери та назви магазинів.
// 3. Автоматизує повне вивантаження товарної бази з пам'яті РРО в Room та навпаки.
// 4. Забезпечує безпеку: звіряє заводський номер підключеного РРО з профілем касира (захист від шахрайства).
// =================================================================================================

package com.nekorom.kasaapp

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exellio20.roomDB.entities.PdvItem
import com.nekorom.bt.bluetooth.BluetoothController
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.repository.BtKasaRepository
import com.nekorom.kasaapp.domain.usecase.GetTestUseCase
import com.nekorom.kasaapp.domain.usecase.MessageToArticleUseCase
import com.nekorom.kasaapp.domain.usecase.PdvMessageToListOfPdvUseCase
import com.nekorom.kasaapp.firebase.NetworkDataRepositoryImp
import com.nekorom.kasaapp.keys.USER_ERR
import com.nekorom.kasaapp.utils.Constants.Command.FIRST_AFTER_PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.FIRST_PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.HEADER_3_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.IDnumber_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.LAST_PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.NEXT_PROGUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.PDV_REQUEST
import com.nekorom.kasaapp.utils.USER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Головна ViewModel терміналу kasaApp3.0.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val bluetoothAdapter: BluetoothAdapter,
    val getTestUseCase: GetTestUseCase,
    val networkDataRepository: NetworkDataRepositoryImp,
    database: AppDatabase,
    val bluetoothController: BluetoothController,
    val btKasaExellioRepository: BtKasaRepository
) : ViewModel(), BluetoothController.Listener {

    // Парсер пакету податкових ставок (команда 83)
    private val msgToPdv = PdvMessageToListOfPdvUseCase()

    // Парсер пакету товарної позиції (команда 68)
    private val toArticle = MessageToArticleUseCase()

    // DAO для доступу до локальної бази даних Room
    private val dao = database.dao()

    // Стан з'єднання Bluetooth (CONNECTED, NOT_CONNECTED, CONNECTING)
    val btState: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    // Текстовий статус процесу обміну з РРО (наприклад: "READING", "READING_ENDED")
    val stateForCommunication: MutableLiveData<String> by lazy {
        MutableLiveData<String>("")
    }

    // Прапорець доступності та ліцензійної валідності касира
    val EnabledState: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }

    // Канал виклику системних діалогів (наприклад помилка розбіжності каси USER_ERR)
    val dialogState: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /**
     * Збереження профілю касира у хмарі Firebase.
     */
    fun writeUserToFirebase(){
        networkDataRepository.writeNewUser()
    }

    // Відповідь контролера зв'язку (помилки, таймаути NOT_ANSWER)
    val btAnswer: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    // Прапорець відображення прогресу очікування виконання апаратної операції на РРО
    val btRequestState: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    // Тимчасовий профіль нового користувача/каси при первинному спарюванні
    private val newUserEntity = UserEntity()

    // Прапорець нового первинного з'єднання (потребує читання серійного номера та заголовка)
    private var newConnState = false

    init {
        // Реєстрація ViewModel як слухача вхідних пакетів від апаратного контролера
        bluetoothController.addListener(this)
    }

    /**
     * Запис товару в локальну базу Room та одночасне відправлення кадру запису в пам'ять РРО.
     */
    fun sendArticle(article: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO){
            dao.addProduct(article.copy(isIndependent = true))
        }
        btKasaExellioRepository.sendArticleRequest(article)
    }

    /**
     * Послідовна пакетна відправка списку товарів у РРО з паузою 200мс між командами для буферизації апарата.
     */
    fun sendAllArticle(articles: List<ProductEntity>, endSending: () -> Unit) {
        viewModelScope.launch {
            articles.forEach {
                delay(200)
                sendArticle(it)
            }
            endSending()
        }
    }

    /**
     * Збереження податкової ставки у базу Room.
     */
    fun insertPdv(pdvItem: PdvItem) = viewModelScope.launch {
        dao.insertPdv(pdvItem)
    }

    /**
     * Відправка коду/штрих-коду або команди на торгові ваги.
     */
    fun sendCode(code: String) {
        btKasaExellioRepository.sendCode(code)
    }

    /**
     * Очищення списку податкових ставок для поточного користувача.
     */
    fun deleteAllPdv() = viewModelScope.launch {
        dao.deleteAllPdv(USER?.userId ?: 0)
    }

    /**
     * Підключення до вже відомого РРО за збереженою MAC-адресою.
     */
    fun btConnect(mac: String) {
        bluetoothController.connect(mac, this)
        newConnState = false
        btState.value = keys.CONNECTING
    }

    /**
     * Первинне підключення до нового РРО з подальшою ініціалізацією профілю.
     */
    fun btNewConnect(mac: String) {
        btState.value = keys.CONNECTING
        bluetoothController.connect(mac, this)
        newConnState = true
        newUserEntity.mac = mac
    }

    /**
     * Обробка успішного підключення до РРО та запуск ланцюжка зчитування заводського номера.
     */
    fun btConnected(mac: String){
        newConnState = true
        newUserEntity.mac = mac
        kasaSendId()
    }

    /**
     * Автентифікація касира через хмарний бекенд Firebase.
     */
    fun logIn(onSuccess: () -> Unit) {
        networkDataRepository.connectToDatabase(onSuccess, {
            Log.d("checkData", "Error: ${it}")
        })
    }

    /**
     * Вихід касира з системи.
     */
    fun logOut() {
        networkDataRepository.signOut()
    }

    // Отримання зв'язки касира з його номенклатурою товарів
    fun readAllNotes(userId: Int) = dao.getUsersWithProducts(userId)

    // Читання всіх зареєстрованих локально касирів
    fun readAllUsers() = dao.getAllUsers()

    /**
     * Додавання нового касира/профілю в локальну базу Room.
     */
    fun addUser(userEntity: UserEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.addUser(userEntity = userEntity)
            viewModelScope.launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    /**
     * Повне каскадне видалення профілю касира та всіх пов'язаних сутностей:
     * товарів, ставок ПДВ, товарних груп та кодів УКТЗЕД.
     */
    fun deleteUser(user: UserEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteUser(user.userId)
            dao.deleteAllArticle(user.userId)
            dao.deleteAllPdv(user.userId)
            dao.deleteAllGroup(user.userId)
            dao.deleteAllUktz(user.userId)
            viewModelScope.launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // Накопичувальний буфер фрагментованого пакету протоколу Datecs
    private var fullCommand = ""

    // Локальний LiveData кеш завантажених товарів
    private val _articleList = MutableLiveData<List<ProductEntity>>()
    private val articleList: LiveData<List<ProductEntity>> get() = _articleList

    // Потік товарів для конкретного касира
    fun articleListF(user: UserEntity): LiveData<List<ProductEntity>> =
        dao.getProductsForUser(user.userId)

    private var lastArticle = ProductEntity()
    private var commandToSend = ""
    private var actualUser = UserEntity()

    /**
     * Очищення локальної таблиці товарів касира в Room перед новим повним синхронізаційним зчитуванням з РРО.
     */
    fun clearArticle(userId: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllArticle(userId = userId.userId)
        }
        _articleList.value = mutableListOf<ProductEntity>()
    }

    /**
     * Збереження прочитаного з РРО товару в базу даних та запит наступного товару з пам'яті апарата (NEXT_PROGUCT_REQUEST).
     * Якщо productId == 0 — відновлює читання за останнім збереженим ідентифікатором.
     */
    fun addArticle(productEntity: ProductEntity) {
        if (productEntity.productId != 0) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.addProduct(productEntity)
                viewModelScope.launch(Dispatchers.Main) {
                    btKasaExellioRepository.request(NEXT_PROGUCT_REQUEST)
                }
            }
            val currentList = _articleList.value?.toMutableList() ?: mutableListOf()
            currentList.add(productEntity)
            _articleList.value = currentList
            Log.d("Id", currentList.last().productId.toString())
        } else {
            val readProduct = if (articleList.value?.last()?.productId != null)
                articleList.value?.last()?.productId?.plus(1).toString() else "1"
            btKasaExellioRepository.startReadingForId(readProduct)
        }
    }


    // --- Методи взаємодії з репозиторієм Datecs РРО ---

    /**
     * Відправляє в РРО запит на отримання ідентифікаційного номера (серійного/фіскального номера каси).
     */
    fun kasaSendId() {
        btKasaExellioRepository.sendRequestId()
    }

    /**
     * Відправляє довільне текстове повідомлення/команду в канал зв'язку з касою.
     * @param message Пакет команди Datecs у протокольному форматі.
     */
    fun kasaSendMess(message: String) {
        btKasaExellioRepository.sendMess(message)
    }

    /**
     * Буферизує вхідні фрагменти байтів/рядків від Bluetooth-каналу та виділяє повний протокольний пакет.
     * Протокол Datecs обрамляє пакет символами:
     * - `\u0001` (SOH - Start of Header)
     * - `\u0003` (ETX - End of Text)
     *
     * Метод накопичує дані в [fullCommand] і обрізає зайве до першого SOH та після першого ETX.
     * @param message Черговий отриманий фрагмент рядка.
     */
    fun toFullMess(message: String) {
        fullCommand = (fullCommand + message)
        fullCommand = fullCommand.substring(
            if (fullCommand.indexOf("\u0001") != -1)
                fullCommand.indexOf("\u0001")
            else
                0,
            if (fullCommand.indexOf("\u0003") != -1)
                fullCommand.indexOf("\u0003") + 1
            else
                fullCommand.length
        )
    }

    /**
     * Запускає процедуру повного вичитування бази товарів (PLU/артиклів) із фіскального накопичувача РРО.
     * 1. Запитує ID каси ([kasaSendId]).
     * 2. Встановлює стан виконання команди [keys.START_READING].
     * 3. Фіксує поточного користувача [actualUser].
     * 4. Переводить статус комунікації в [keys.READING_IS] для відображення індикатора прогресу в UI.
     * @param user Поточний касир/користувач, до якого прив'язується завантажувана номенклатура.
     */
    fun readKasaDB(user: UserEntity) {
        kasaSendId()
        commandToSend = keys.START_READING
        actualUser = user
        stateForCommunication.value = keys.READING_IS
    }

    /**
     * Центральний обробник зворотних викликів (Callback) від BluetoothController.
     * Виконує роль диспетчера подій та машини станів фіскального протоколу Datecs/Exellio:
     * - Обробляє системні події підключення/відключення Bluetooth.
     * - Буферизує дані до повного кадру (між SOH і ETX).
     * - Перевіряє байт послідовного лічильника пакетів (sequence number / actualId).
     * - Маршрутизує відповіді залежно від поточної надісланої команди [nowSendCommand].
     * - Здійснює покрокове читання номенклатури, ПДВ-ставок, заголовків чека та перевірку ID каси.
     *
     * @param message Сирий фрагмент рядка або статусний маркер від контролера Bluetooth.
     */
    override fun onReceive(message: String) {
        Log.i("btMess", message)
        viewModelScope.launch(Dispatchers.Main) {

            when (message) {
                // Втрата з'єднання з касовим апаратом
                BluetoothController.BLUETOOTH_NOT_CONNECTED -> {
                    btState.value = keys.NOT_CONNECTED
                    fullCommand = ""
                    btKasaExellioRepository.nowSendCommand.value = ""
                    return@launch
                }

                // Успішне встановлення каналу зв'язку Bluetooth SPP
                BluetoothController.BLUETOOTH_CONNECTED -> {
                    btState.value = keys.CONNECTED
                    btKasaExellioRepository.nowSendCommand.value = ""
                    return@launch
                }

                // РРО не відповів у межах таймауту
                BluetoothController.NOT_ANSWER -> {
                    if (checkReadingDB(BluetoothController.NOT_ANSWER)) {
                        btAnswer.value = BluetoothController.NOT_ANSWER
                        return@launch
                    }
                }

                // Отримано дані від каси
                else -> {
                    toFullMess(message)
                    Log.d("btFull", fullCommand)
                    // Перевіряємо, чи зібрано повний кадр (наявні маркери початку SOH і кінця ETX)
                    if (fullCommand.indexOf("\u0001") != -1 &&
                        fullCommand.indexOf("\u0003") != -1
                    ) {
                        Log.i("btAnswer", fullCommand)
                        // Передаємо повний пакет у репозиторій для внутрішньої обробки черги
                        viewModelScope.launch {
                            btKasaExellioRepository.onReceive(fullCommand)
                        }

                        // Валідація лічильника послідовності пакету (fullCommand[5] містить код інкрементованого символу)
                        if (fullCommand[5].code == btKasaExellioRepository.actualId)

                            when (btKasaExellioRepository.nowSendCommand.value) {
                                // Відповідь на запит останнього товару в базі РРО
                                LAST_PRODUCT_REQUEST -> {
                                    btKasaExellioRepository.firstArticleReading(fullCommand)
                                }

                                // Відповідь на запит першого товару
                                FIRST_PRODUCT_REQUEST -> {
                                    addArticle(
                                        btKasaExellioRepository.nextArticleReading(
                                            fullCommand
                                        )
                                    )
                                }

                                // Відповідь на запит наступного товару при послідовному переборі
                                NEXT_PROGUCT_REQUEST -> {
                                    // Маркер "006;-111004\t" свідчить про те, що товар з таким або більшим кодом відсутній
                                    // (досягнуто кінця бази номенклатури РРО)
                                    if (fullCommand.contains("006;-111004\t")) {
                                        btKasaExellioRepository.nowSendCommand.value = ""
                                        commandToSend = ""
                                        stateForCommunication.value = keys.READING_ENDED
                                    } else {
                                        addArticle(
                                            btKasaExellioRepository.nextArticleReading(
                                                fullCommand
                                            )
                                        )
                                    }
                                }

                                // Відповідь на запит ідентифікаційного номера каси (фіскального/заводського ID)
                                IDnumber_REQUEST -> {
                                    // Виділяємо ID каси між символами табуляції '\t'
                                    val idNum = fullCommand
                                        .substring(fullCommand.indexOfFirst { it.toString() == "\t" } + 1,
                                            fullCommand.indexOfLast { it.toString() == "\t" })
                                    if (newConnState) {
                                        // Первинна реєстрація/прив'язка нової каси в додатку
                                        newUserEntity.kasaId = idNum
                                        btKasaExellioRepository.nameOfShopKasaRequest()
                                    } else if (actualUser.kasaId == idNum) {
                                        // Перевірка відповідності поточної каси зареєстрованому користувачу
                                        when (commandToSend) {
                                            keys.START_READING -> {
                                                // Очищаємо локальну базу артиклів перед повним оновленням з каси
                                                clearArticle(actualUser)
                                                btKasaExellioRepository.startReadingForId("1")
                                            }
                                        }
                                    } else if (USER?.kasaId != idNum) {
                                        // Захист від помилки касира: додаток підключено не до того касового апарата
                                        dialogState.value = USER_ERR
                                    }
                                }

                                // Відповідь з таблицею податкових ставок ПДВ і акцизу (групи А-К)
                                PDV_REQUEST -> {
                                    deleteAllPdv()
                                    for (i in msgToPdv.execute(
                                        message = fullCommand,
                                        USER?.userId ?: 1
                                    )) {
                                        insertPdv(i)
                                    }
                                    if (newConnState) {
                                        newConnState = false
                                    }
                                }

                                // Відповідь із рядком заголовка чека (Header line 3 - назва або адреса торговельної точки)
                                HEADER_3_REQUEST -> {
                                    if (newConnState) {
                                        newUserEntity.locationAddress = fullCommand
                                            .substring(fullCommand.indexOfFirst { it.toString() == "\t" },
                                                fullCommand.indexOfLast { it.toString() == "\t" })
                                        newUserEntity.name = newUserEntity.locationAddress
                                        addUser(newUserEntity) {
                                            btKasaExellioRepository.pdvRequest()
                                        }
                                    }
                                }
                            }

                        // Скидаємо буфер після успішної обробки повного кадру
                        fullCommand = ""
                    } else
                        checkReadingDB("END_TRANSACTION")
                }
            }
        }
    }

    /**
     * Механізм відновлення та повторної синхронізації при збоях зчитування бази номенклатури РРО.
     * Обробляє ситуації таймаутів, розривів або незавершених транзакцій каси:
     * - Якщо пакет містить валідний фрагмент товару з ненульовим [productId], запитує наступний товар після нього.
     * - Якщо товар пошкоджений або відсутній, продовжує зчитування з наступного за порядком номера ([readProduct]).
     *
     * @param messContains Маркер помилки або стан транзакції (наприклад, [BluetoothController.NOT_ANSWER] чи "END_TRANSACTION").
     * @return `true`, якщо помилка не потребує специфічного відновлення черги читання; `false`, якщо виконано відновлювальний запит.
     */
    fun checkReadingDB(messContains: String): Boolean {
        var result = false
        val readProduct = if (articleList.value?.lastOrNull()?.productId != null)
            articleList.value?.last()?.productId?.plus(1).toString() else "1"

        if ((btKasaExellioRepository.nowSendCommand.value == NEXT_PROGUCT_REQUEST &&
                    fullCommand.indexOf("\u0001") != -1 &&
                    fullCommand.contains(messContains))
        ) {
            val partOfArticle = toArticle.execute(fullCommand)
            if (partOfArticle.productId != 0) {
                btKasaExellioRepository.sendMess(FIRST_AFTER_PRODUCT_REQUEST + "${partOfArticle.productId}\t")
            } else {
                btKasaExellioRepository.startReadingForId(readProduct)
                fullCommand = ""
            }

            fullCommand = ""
        } else if (btKasaExellioRepository.nowSendCommand.value == FIRST_PRODUCT_REQUEST &&
            fullCommand.indexOf("\u0001") != -1 &&
            fullCommand.contains(messContains)
        ) {
            btKasaExellioRepository.startReadingForId(readProduct)
            fullCommand = ""
        } else if (btKasaExellioRepository.nowSendCommand.value == LAST_PRODUCT_REQUEST &&
            fullCommand.indexOf("\u0001") != -1 &&
            fullCommand.contains(messContains)
        ) {
            readKasaDB(USER ?: UserEntity())
        } else if (btKasaExellioRepository.nowSendCommand.value == NEXT_PROGUCT_REQUEST &&
            fullCommand.contains(messContains)
        ) {
            btKasaExellioRepository.startReadingForId(readProduct)
            fullCommand = ""
        } else
            result = true

        return result
    }
}

/**
 * Об'єкт констант для ідентифікації станів з'єднання Bluetooth та етапів зчитування бази РРО.
 */
object keys {
    /** Bluetooth підключено та готовий до обміну */
    const val CONNECTED = "CONNECTED"
    /** Bluetooth відключено */
    const val NOT_CONNECTED = "NOT_CONNECTED"
    /** Процес встановлення з'єднання з пристроєм */
    const val CONNECTING = "CONNECTING"
    /** Помилка авторизації: підключений касовий апарат не відповідає активному профілю користувача */
    const val USER_ERR = "USER_ERR"

    /** Команда/стан старту зчитування номенклатури */
    const val START_READING = "START_READING"
    /** Процес зчитування бази товарів у ході виконання */
    const val READING_IS = "READING"
    /** Успішне завершення вичитування всієї бази номенклатури */
    const val READING_ENDED = "READING_ENDED"
}

