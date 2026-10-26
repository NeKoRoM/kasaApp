package com.example.exelioshop.classes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.exelioshop.domain.models.LogInf

/**
 * [VMData] — базова [ViewModel], що акумулює реактивний стан передачі даних між компонентами ExelioShop.
 * 
 * Зберігає LiveData потоки для:
 * - [articleList]: поточної картки товару [Article.ArticleData]
 * - [logList]: списку записів журналу операцій каси/сканування
 * - [logItem]: окремого щойно доданого запису логу
 * - [logmess]: текстових службових сповіщень Bluetooth-драйвера
 */
open class VMData : ViewModel() {
    /** Поточний розпарсений товар з РРО */
    val articleList: MutableLiveData<Article.ArticleData> by lazy {
        MutableLiveData<Article.ArticleData>()
    }

    /** Список усіх повідомлень журналу сесії */
    val logList: MutableLiveData<ArrayList<LogInf>> by lazy {
        MutableLiveData<ArrayList<LogInf>>()
    }

    /** Останній доданий елемент логу */
    val logItem: MutableLiveData<LogInf> by lazy {
        MutableLiveData<LogInf>()
    }

    /** Текстове повідомлення статусу з'єднання або помилки */
    val logmess: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}