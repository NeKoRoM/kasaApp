// =================================================================================================
// ДОМЕННИЙ ІНТЕРФЕЙС КАСОВОГО АПАРАТА DATECS / EXELLIO (РРО BLUETOOTH CONTRACT)
//
// Архітектурний шар: Domain Layer (Repository Contract).
// Призначення:
// Визначає контракт високого рівня для зв'язку з касовим апаратом через Bluetooth:
// стан надсилання та отримання команд, зчитування та запис товарів, фіскальні звіти (X/Z),
// запити податкових ставок (ПДВ/акциз), часу та заводського номера.
// =================================================================================================

package com.nekorom.kasaapp.domain.repository

import androidx.lifecycle.MutableLiveData
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.domain.usecase.MessageToArticleUseCase

/**
 * Контракт взаємодії з апаратним РРО через Bluetooth.
 */
interface BtKasaRepository {
    // Спостережуваний стан поточної надісланої команди
    var nowSendCommand : MutableLiveData<String>

    // Спостережуваний стан останньої отриманої відповіді від РРО
    var lastGetAns : MutableLiveData<String>

    // Поточний порядковий номер пакета протоколу
    var actualId : Int

    // Кешований екземпляр останнього прочитаного товару
    var lastArticle : ProductEntity

    // UseCase десеріалізації рядка відповіді РРО в ProductEntity
    val toArticle : MessageToArticleUseCase

    // Запуск читання товарів за початковим ідентифікатором
    fun startReadingForId(idForStart: String)

    // Читання першого товару (тестовий сценарій)
    fun firstArticleReading(message: String)

    // Читання чергового товару з потоку пакетів
    fun nextArticleReading(message: String) : ProductEntity

    // Запит заводського та фіскального номерів РРО (команда 0090)
    fun sendRequestId()

    // Запит найменування торгової точки із заголовка чека
    fun nameOfShopKasaRequest()

    // Відправка довільної протокольної команди
    fun sendMess(message: String)

    // Відправка команди штрих-коду або зважування на ваги
    fun sendCode(message: String)

    // Запит системного часу РРО
    fun timeRequest()

    // Запит денного X-звіту (без обнулення)
    fun xReportRequest()

    // Запит фіскального Z-звіту (з обнуленням оперативних регістрів)
    fun zReportRequest()

    // Синхронізація часу РРО
    fun setTimeRequest()

    // Запит ставок ПДВ та акцизу (групи А-К)
    fun pdvRequest()

    // Асинхронна обробка вхідного пакета від РРО
    suspend fun onReceive(message: String)

    // Запис картки товару в оперативну пам'ять РРО (команда 68)
    fun sendArticleRequest(article: ProductEntity)

    // Базовий метод відправки протокольного кадру
    fun request(request: String)

    // Запит на видалення артикулу з пам'яті РРО
    fun deleteArticleRequest(code: Int, id: Int)
}