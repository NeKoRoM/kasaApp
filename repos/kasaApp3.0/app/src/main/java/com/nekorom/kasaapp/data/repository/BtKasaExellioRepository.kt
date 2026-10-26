// =================================================================================================
// РЕПОЗИТОРІЙ УПРАВЛІННЯ РРО DATECS / EXELLIO ЧЕРЕЗ BLUETOOTH ТА СИНХРОНІЗАЦІЇ З ROOM DB
//
// Архітектурний шар: Data Layer (Репозиторій взаємодії з апаратним РРО).
// Призначення:
// 1. Забезпечує шлюз між рівнем представлення / доменною логікою та модулем зв'язку BluetoothController.
// 2. Формує та відправляє низькорівневі протокольні пакети команд Datecs/Exellio (запис/читання товарів,
//    податкові ставки, звіти X/Z, отримання часу, заводського номера, видалення товарів).
// 3. Обробляє вхідні пакети відповідей від апарата (onReceive) та синхронізує локальну базу Room (ProductEntity).
// 4. Реалізує чергу підтвердження транзакцій (state machine) для операцій оновлення та видалення товарів.
// =================================================================================================

package com.nekorom.kasaapp.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.nekorom.bt.bluetooth.BluetoothController
import com.nekorom.kasaapp.data.roomDB.AppDatabase
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.domain.repository.BtKasaRepository
import com.nekorom.kasaapp.domain.usecase.MessageToArticleUseCase
import com.nekorom.kasaapp.utils.Constants
import com.nekorom.kasaapp.utils.Constants.Command.FIRST_PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.IDnumber_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.LAST_PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.PDV_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.PRODUCT_DELETE
import com.nekorom.kasaapp.utils.Constants.Command.PRODUCT_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.PRODUCT_REQUEST_FROM_FIRST
import com.nekorom.kasaapp.utils.Constants.Command.TIME_REQUEST
import com.nekorom.kasaapp.utils.Constants.Command.X_REPORT
import com.nekorom.kasaapp.utils.Constants.Command.Z_REPORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Реалізація репозиторію [BtKasaRepository] для фіскальних реєстраторів Datecs / Exellio.
 *
 * @param bluetoothController Контролер Bluetooth-з'єднання з РРО та вагами.
 * @param database Екземпляр бази даних Room для збереження товарів та налаштувань.
 */
class BtKasaExellioRepository (
    private val bluetoothController: BluetoothController, 
    database: AppDatabase,
) : BtKasaRepository {

    // DAO для доступу до таблиці товарів у локальній базі Room
    val dao = database.dao()

    // LiveData з останнім сирим текстом відповіді від касового апарата
    override var lastGetAns: MutableLiveData<String> = MutableLiveData("")

    // LiveData з поточною надісланою командою (використовується як контекст для обробника onReceive)
    override var nowSendCommand: MutableLiveData<String> = MutableLiveData("")

    // Поточний порядковий номер пакета протоколу Datecs (0x20..0x7F)
    override var actualId = 0

    // Останній прочитаний з РРО товар
    override var lastArticle = ProductEntity()

    // UseCase для парсингу сирого рядка відповіді РРО у сутність ProductEntity
    override val toArticle = MessageToArticleUseCase()

    // Тимчасовий Room ID товару, який оновлюється в РРО (для збереження автоінкрементного ключа)
    var tempID = 0

    // ID товару в базі даних Room для видалення після підтвердження від РРО
    var idToDelete = 0

    // Артикул (код) товару в пам'яті РРО, надісланий на видалення
    var codeToDelete = 0

    /**
     * Асинхронне оновлення або додавання товару в локальну базу даних Room.
     * Якщо [tempID] == 0 — створюється новий запис у базі даних.
     * Якщо [tempID] != 0 — оновлюється існуючий запис зі збереженням первинного ключа [tempID].
     */
    suspend fun updateArticle(productEntity: ProductEntity) = coroutineScope{
        if (productEntity.productId != 0) {
            if (tempID == 0)
                launch(Dispatchers.IO) {
                    dao.addProduct(productEntity)
                }
            else {
                launch(Dispatchers.IO) {
                    dao.updateProduct(product = productEntity.copy(id = tempID))
                    tempID = 0
                }
            }
        }
    }

    /**
     * Запуск читання товарів з пам'яті РРО:
     * - якщо idForStart == "1" — запитується останній збережений товар (LAST_PRODUCT_REQUEST).
     * - інакше — запитується діапазон товарів від вказаного ID.
     */
    override fun startReadingForId(idForStart:String) {
        if (idForStart == "1")
        request(LAST_PRODUCT_REQUEST)
        else
            request(PRODUCT_REQUEST_FROM_FIRST+idForStart+"\t", FIRST_PRODUCT_REQUEST)

    }



    // Читання першого товару в черзі з пам'яті РРО (для тестових сценаріїв)
    override fun firstArticleReading(message: String) {
        request(FIRST_PRODUCT_REQUEST)
        lastArticle = toArticle.execute(message)?: ProductEntity()
    }

    // Читання наступного товару в потоці пакетів та десеріалізація в ProductEntity
    override fun nextArticleReading(message: String) : ProductEntity {
        toArticle.execute(message).let { return it }
    }

    // Запит заводського номера та фіскального номера РРО (команда 0090)
    override fun sendRequestId() {
        request(IDnumber_REQUEST)
    }

    // Запит третього рядка заголовка чека (зазвичай назва магазину / ФОП)
    override fun nameOfShopKasaRequest() {
        request(Constants.Command.HEADER_3_REQUEST)
    }

    // Відправка довільного рядка команди до РРО через Bluetooth контролер
    override fun sendMess(message: String) {
        bluetoothController.sendMessage(message)
        actualId = bluetoothController.com.id
    }

    // Відправка штрих-коду або команди до торгових ваг
    override fun sendCode(message: String) {
        bluetoothController.sendMessageBar(message)
    }

    // Запит поточного внутрішнього часу РРО (команда 0068 / 0062)
    override fun timeRequest() {
        request(TIME_REQUEST)
    }

    // Друк денного X-звіту (без обнулення регістрів оперативної пам'яті)
    override fun xReportRequest() {
        request(X_REPORT)
    }

    // Друк фіскального денного Z-звіту з обнуленням та записом у фіскальну пам'ять
    override fun zReportRequest() {
        request(Z_REPORT)
    }

    // Встановлення системного часу РРО
    override fun setTimeRequest() {
        // Зарезервовано для синхронізації часу з Android-пристроєм
    }

    // Запит таблиці податкових ставок (ПДВ та акцизний збір по групах А-К)
    override fun pdvRequest() {
        request(PDV_REQUEST)
    }

    /**
     * Запит на видалення товару з пам'яті РРО за кодом (артикулом).
     * Відправляє команду видалення діапазону збіжних кодів, запам'ятовуючи id в Room для подальшого каскадного видалення.
     */
    override fun deleteArticleRequest(code: Int, id: Int){
        request(PRODUCT_DELETE+"$code"+"\t"+"$code"+"\t", PRODUCT_DELETE)
        idToDelete = id
        codeToDelete = code
    }

    /**
     * Головний обробник вхідних повідомлень від апарата (Callback / Listener).
     * Реалізує кінцевий автомат (State Machine) синхронізації бази:
     * 1. Якщо попередня команда була записом товару ("006;P"), автоматично формується
     *    запит на зворотне читання (PRODUCT_REQUEST_FROM_FIRST) для перевірки збереження.
     * 2. Якщо обробляється відповідь на PRODUCT_REQUEST_FROM_FIRST — оновлюється база Room.
     * 3. Якщо виконувалося PRODUCT_DELETE і отримано успіх "0\t", перевіряється відсутність
     *    товару в РРО (PRODUCT_REQUEST). Отримання помилки "-111004" (товар відсутній)
     *    підтверджує успішне видалення з РРО, після чого товар видаляється з локальної бази Room.
     */
    override suspend fun onReceive(message: String){
        lastGetAns.value = message
        if (nowSendCommand.value?.contains("006;P") == true){
            Log.d("Bt", nowSendCommand.value!!
                .substring(startIndex = nowSendCommand.value!!
                    .indexOf("\t")+1, endIndex = nowSendCommand.value!!
                        .indexOf("\t", startIndex = nowSendCommand.value!!.indexOf("\t")+1))
            )
            request(request = PRODUCT_REQUEST_FROM_FIRST+nowSendCommand.value!!
                .substring(startIndex = nowSendCommand.value!!
                    .indexOf("\t")+1, endIndex = nowSendCommand.value!!
                    .indexOf("\t", startIndex = nowSendCommand.value!!.indexOf("\t")+1))+"\t",
                sendComm = PRODUCT_REQUEST_FROM_FIRST)
        }
        when(nowSendCommand.value){
            PRODUCT_REQUEST_FROM_FIRST->{
                updateArticle(toArticle.execute(message))
            }
            PRODUCT_DELETE->{
                if (message.contains("0\t"))
                {
                    request(PRODUCT_REQUEST+"$codeToDelete"+"\t", PRODUCT_REQUEST)
                    codeToDelete = 0
                }
            }
            PRODUCT_REQUEST->{
                // Код -111004 означає "Артикул не знайдено" — підтвердження видалення з пам'яті РРО
                if (message.contains("006;-111004\t")){
                    deleteArticle(idToDelete)
                    idToDelete = 0
                }
            }
        }
    }

    /**
     * Асинхронне видалення запису товару з Room DB у фоновому потоці.
     */
    suspend fun deleteArticle(id: Int) = coroutineScope{
        dao.deleteArticle(id)
    }

    /**
     * Формування та відправка протокольного пакета запису артикулу (команда 68 / 006;P).
     * Структура полів пакету Datecs (15 полів, розділених символом табуляції \t):
     * 1. Код артикулу (productId)
     * 2. Податкова група (TAX: 1-8 / А-К)
     * 3. Відділ (section)
     * 4. Товарна група (group: 1-99)
     * 5. Тип товару (type: ваговий/штучний)
     * 6. Ціна (price)
     * 7. Залишок на складі (remainder)
     * 8-11. Штрих-коди 1-4 (barCode1..4)
     * 12. Назва товару (name, до 48 символів)
     * 13. Ознака вагового товару (weightOrUnit)
     * 14. Код УКТЗЕД (UKTZED, 10 цифр)
     * 15. Одиниця виміру (unitOfMeasurement, наприклад "шт", "кг")
     * Завершується символом \u0005 (ASCII ENQ).
     */
    override fun sendArticleRequest(article: ProductEntity) {
        tempID = article.id
        val message ="006;P\t${article.productId.toString()}" +
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
        request(message)
    }

    /**
     * Відправка команди в РРО з фіксацією тексту запиту в стані.
     */
    override fun request(request:String){
        bluetoothController.sendMessage(request)
        actualId = bluetoothController.com.id
        nowSendCommand.value = request
    }

    /**
     * Відправка команди з явним перевантаженням відслідковуваного стану `sendComm`.
     */
    private fun request(request:String, sendComm: String){
        bluetoothController.sendMessage(request)
        actualId = bluetoothController.com.id
        nowSendCommand.value = sendComm
    }
}