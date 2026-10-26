package com.example.exelioshop.classes

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.exelioshop.MainApp
import com.example.exelioshop.db.MainDataBase
import com.example.exelioshop.entities.ArticleList
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

/**
 * =================================================================================================
 * MainViewModel — Головна бізнес-модель стану POS-терміналу та синхронізації з Room.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Зберігає реактивний потік усіх артикулів з локальної бази даних Room (allList).
 * 2. Координує обмін подіями між UI, Bluetooth-драйвером та фіскальним реєстратором:
 *    - barCode: отриманий від сканера штрихкод для пошуку або продажу;
 *    - settingToKasa: службова команда налаштування, яку слід передати у РРО;
 *    - timeToSmart: синхронізований час годинника реального часу (RTC) каси;
 *    - articleNew: модель для запису або оновлення артикулу в РРО;
 *    - articleToScan: товар, відправлений для передачі в режим оптичного сканування/продажу;
 *    - listHeading / listHeadingToSet: читання та запис рядків заголовка (кліше) чека.
 * =================================================================================================
 */
class MainViewModel(dataBase: MainDataBase) : ViewModel() {
    /** DAO інтерфейс доступу до бази даних товарів */
    val dao = dataBase.getDao()

    /** Повний реактивний список усіх збережених артикулів (Room -> Flow -> LiveData) */
    val allList: LiveData<List<ArticleList>> = dao.getAllArticle().asLiveData()

    /** Останній розпізнаний штрихкод */
    val barCode: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /** Службова команда конфігурації для відправки в РРО (наприклад, зміна швидкості або звіт) */
    val settingToKasa: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /** Час і дата, прочитані з фіскального реєстратора (команда 0x3E) */
    val timeToSmart: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /** Дані нового або відредагованого товару для запису в пам'ять РРО */
    val articleNew: MutableLiveData<Article.ArticleData> by lazy {
        MutableLiveData<Article.ArticleData>()
    }

    /** Товар, обраний для сканування або додавання в чек продажу */
    val articleToScan: MutableLiveData<ArticleList> by lazy {
        MutableLiveData<ArticleList>()
    }

    /** Список рядків заголовка (кліше) чека, прочитаних з каси */
    val listHeading: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>()
    }

    /** Список нових рядків кліше для запису в постійну пам'ять РРО */
    val listHeadingToSet: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>()
    }

    /**
     * Асинхронне додавання або оновлення товару в базі даних Room через CoroutineScope
     */
    fun insertArticle(articleList: ArticleList) = viewModelScope.launch {
        dao.insertArticle(articleList)
    }

    /** Відфільтрований список товарів для пошуку */
    val filteredItems = MutableLiveData<MutableList<ArticleList>>().apply {
        value = allList.value as MutableList<ArticleList>?
    }

    /**
     * Фабрика для створення екземпляра MainViewModel з обов'язковою передачею бази даних
     */
    class MainViewModelFactory(val database: MainDataBase) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModelClass")
        }
    }
}