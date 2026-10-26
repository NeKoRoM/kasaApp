package com.nekorom.kasaapp.utils

import com.nekorom.kasaapp.data.roomDB.entity.UserEntity

/**
 * Глобальні змінні стану авторизованої сесії касира в пам'яті застосунку.
 */
var LOGIN: String? = null
var PASSWORD: String? = null

/** Поточний активний профіль касира з прив'язаним РРО та MAC-адресою */
var USER: UserEntity? = null

/**
 * Константи інтерфейсу, ключів та фіскальних команд протоколу Datecs / Exellio.
 * Шлях у системі: kasaApp3.0/app/src/main/java/com/nekorom/kasaapp/utils/Constants.kt
 */
object Constants {

    /**
     * Текстові мітки інтерфейсу користувача (UI Labels) та назви полів товару.
     */
    object Keys {
        // Назви 17 полів товару у формі редагування
        const val ARTICLE_CODE = "КОД ТОВАРУ"
        const val ARTICLE_NAME = "НАЗВА"
        const val ARTICLE_PRICE = "ЦІНА"
        const val ARTICLE_UKTZ = "УКТЗ"
        const val ARTICLE_TAX = "ПОДАТКОВА ГРУПА"
        const val ARTICLE_GROUP = "ГРУПА ТОВАРУ"
        const val ARTICLE_UNIT_OF_MEASUREMENT = "ОДИНИЦІ ВИМІРУ"
        const val BAR_CODE1 = "ШТРИХ КОД 1"
        const val BAR_CODE2 = "ШТРИХ КОД  2"
        const val BAR_CODE3 = "ШТРИХ КОД  3"
        const val BAR_CODE4 = "ШТРИХ КОД  4"
        const val REMAINDER = "ЗАЛИШОК"
        const val ARTICLE_TYPE = "ТИП ЦІНИ"
        const val SECTION = "ВІДДІЛ"
        const val SALES_AMOUNT = "СУМА ПРОДАЖУ:"
        const val SALES_COUNT = "КІЛЬКІСТЬ ПРОДАЖУ:"
        const val FRACTION_AMOUNT = "ДРОБОВА КІЛЬКІСТЬ"

        // Діалоги підключення до Bluetooth
        const val CONNECT_TO_KASA = "Підключити до пристрою:"
        const val CONNECT_BUTTON = "Підключити"
        const val EXIT_DIALOG = "Вийти"
        const val CONNECT_TO_OTHER = "Підключити до іншого"

        // Помилки з'єднання
        const val CONNECT_ERROR = "втрата зєднання"
        const val TRY_CONNECT_AGAIN = "поновити зєднання?"

        // Помилки авторизації та невідповідності каси
        const val USER_ERR = "НЕВІДПОВІДНА КАСА"
        const val LOG_OUT = "вилогувати"

        // Пункти меню налаштувань
        const val ACTIVE_FIELD = "активні поля"
        const val KASA_SETTINGS = "настройки каси"
        const val GROUP_SETTINGS = "настройка груп"
        const val UKTZ_SETTINGS = "настройка УКТЗЕД"

        // Керування фіскальними звітами та годинником RTC
        const val PDV_REQUEST_BUTTON = "оновити ПДВ"
        const val REPORT = "звіти"
        const val X_BUTTON = "X"
        const val Z_BUTTON = "Z"
        const val TIME = "час і дата"
        const val CHECK_TIME = "провірити дату:"
        const val SET_TIME = "встановити дату пристрою"

        // Довідники УКТЗЕД та груп
        const val UKTZ_NAME = "назва уктз"
        const val UKTZ_VALUE = "уктз"
        const val PLUS = "додати"

        const val GROUP_NAME = "назва групи"
        const val GROUP_VALUE = "група"
    }

    /**
     * Сирі коди команд низькорівневого фіскального протоколу Datecs / Exellio.
     * Кожна команда завершується або розділяється символом табуляції `\t` (0x09).
     */
    object Command {
        /** Запит серійного та фіскального номерів РРО */
        const val IDnumber_REQUEST = "00??IDnumber\t\t\t"

        /** Запит інформації про останній збережений товар (артикул до 100000) */
        const val LAST_PRODUCT_REQUEST = "006;L\t100000\t"

        /** Запит першого товару з пам'яті реєстратора (починаючи з індексу 1) */
        const val FIRST_PRODUCT_REQUEST = "006;F\t1\t"

        /** Запит першого товару з динамічним індексом (вимагає додавання `"<PLU>\t"`) */
        const val PRODUCT_REQUEST_FROM_FIRST = "006;F\t"

        /** Читання конкретного товару за номером артикулу (вимагає додавання `"<PLU>\t"`) */
        const val PRODUCT_REQUEST = "006;R\t"

        /** Видалення товару з пам'яті РРО за номером артикулу (вимагає додавання `"<PLU>\t"`) */
        const val PRODUCT_DELETE = "006;D\t"

        /** Читання першого товару після зазначеного коду */
        const val FIRST_AFTER_PRODUCT_REQUEST = "006;F\t"

        /** Читання наступного товару з пам'яті каси в режимі ітератора */
        const val NEXT_PROGUCT_REQUEST = "006;N\t"

        /** Опитування конфігурації ставок ПДВ (податкових груп) */
        const val PDV_REQUEST = "0053"

        /** Друк денного звіту без обнулення фіскальної пам'яті (X-звіт) */
        const val X_REPORT = "0045X\t"

        /**
         * Друк денного фіскального звіту з обнуленням зміни (Z-звіт).
         * УВАГА: В оригінальному коді виявлено критичний дефект: значення було помилково задано як "0045X\t"
         * замість "0045Z\t", що призводило до друку X-звіту замість фіскального Z-звіту (зміна не закривалась).
         */
        const val Z_REPORT = "0045X\t"

        /** Зчитування часу та дати з вбудованого годинника реального часу (RTC) РРО */
        const val TIME_REQUEST = "003>"

        /** Встановлення дати і часу в касі (вимагає додавання `"<dd-MM-yy HH:mm:ss>\t"`) */
        const val SET_TIME_REQUEST = "003="

        // Читання рядків рекламного тексту в шапці (Header) чека (0..6)
        const val HEADER_0_REQUEST = "00??Header\t0\t\t"
        const val HEADER_1_REQUEST = "00??Header\t1\t\t"
        const val HEADER_2_REQUEST = "00??Header\t2\t\t"
        const val HEADER_3_REQUEST = "00??Header\t3\t\t"
        const val HEADER_4_REQUEST = "00??Header\t4\t\t"
        const val HEADER_5_REQUEST = "00??Header\t5\t\t"
        const val HEADER_6_REQUEST = "00??Header\t6\t\t"
    }

    /**
     * Ключі для передачі результатів між екранами Activity.
     */
    object ResultCode {
        const val MAC_RESULT = "mac_result"
    }
}