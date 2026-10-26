package nekorom.com.network

import database.Dao
import database.Department
import database.Product
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ====================================================================================================
 * МОДУЛЬ СИНХРОНІЗАЦІЇ З ХМАРОЮ: СЕРВІС GOOGLE APPS SCRIPT / GOOGLE SHEETS
 * ====================================================================================================
 *
 * Даний файл реалізує HTTP-клієнт на базі бібліотеки Ktor Client (Kotlin Multiplatform)
 * для обміну даними між складським додатком "Magazyn" та віддаленою Google-таблицею обліку залишків.
 *
 * АРХІТЕКТУРНІ ОСОБЛИВОСТІ ТА ІНТЕГРАЦІЯ:
 * 1. Google Apps Script Web App як безсерверний (serverless) бекенд:
 *    - Запити надсилаються на опубліковані точки доступу `/exec`.
 *    - Apps Script повертає JSON-масиви номенклатури або статус виконання операцій.
 * 2. Обробка редиректів у Ktor:
 *    - Google Apps Script при прийомі POST або деяких GET запитів видає HTTP 302 Redirect
 *      на внутрішній CDN Google. Плагін [HttpRedirect] з налаштуванням `checkHttpMethod = false`
 *      гарантує збереження методу POST та тіла запиту при перенаправленні.
 * 3. Атомарна синхронізація з локальною базою даних Room:
 *    - Отримані DTO трансформуються в сутності бази даних [Department] та [Product].
 *    - Оновлення локального кешу виконується транзакційно через [Dao.clearAndInsertDepartments]
 *      та [Dao.clearAndInsert].
 */

/**
 * DTO (Data Transfer Object) для отримання позиції товару/номенклатури з Google Sheets.
 *
 * @property id Унікальний ідентифікатор товару (артикул або номер рядка).
 * @property name Назва запчастини або товару.
 */
@Serializable
data class ProductDto(
    val id: String,
    val name: String
)

/**
 * DTO для отримання підрозділу/відділу з Google Sheets.
 *
 * @property name Назва підрозділу (цеху, складу, точки видачі).
 */
@Serializable
data class DepartmentDto(
    val name: String
)

/**
 * Налаштування парсера JSON:
 * - [ignoreUnknownKeys = true]: гарантує стабільність, якщо Google Apps Script поверне
 *   додаткові системні поля або метадані, які не оголошені в DTO.
 */
val json = Json { ignoreUnknownKeys = true }

/**
 * Глобальний екземпляр Ktor HTTP-клієнта.
 * - [HttpRedirect]: обробляє перенаправлення Google Apps Script без скидання POST-методу.
 */
val client = HttpClient() {
    install(HttpRedirect) {
        checkHttpMethod = false // Дозволяє автоматичний редирект зі збереженням POST-запиту
    }
}

/**
 * Виконує прямий GET-запит до Google Apps Script для отримання списку підрозділів у форматі JSON-рядка.
 * Параметр `action = "getData"` вказує сценарію повернути довідник підрозділів.
 *
 * @return Сирий рядок відповіді від сервера.
 */
suspend fun getDepartment(): String {
    val response: HttpResponse = client.get("https://script.google.com/macros/s/AKfycbz1HuonSWyMiDUTHtzsW7hJF5uA8YahBYlODoK34OlMmDIYZTDuFWV8Jy72PZ_VeYQ-/exec") {
        url {
            parameters.append("action", "getData")
        }
    }

    return response.bodyAsText()
}

/**
 * Завантажує актуальний довідник підрозділів з Google Sheets, парсить JSON і транзакційно оновлює БД.
 *
 * КРОКИ ВИКОНАННЯ:
 * 1. Відправка GET-запиту з параметром `action=getData`.
 * 2. Десеріалізація JSON у список об'єктів [DepartmentDto].
 * 3. Мапінг [DepartmentDto] у доменні Room-сутності [Department].
 * 4. Очищення застарілих записів та пакетна вставка в базу через [Dao.clearAndInsertDepartments].
 *
 * @param departmentDao Об'єкт доступу до даних Room.
 * @return [Result.success] у разі успіху, або [Result.failure] з винятком при збої мережі чи парсингу.
 */
suspend fun updateDepartmentsInDb(departmentDao: Dao): Result<Unit> {
    return try {
        // 1. Виконуємо GET-запит до Google Apps Script
        val response: HttpResponse = client.get("https://script.google.com/macros/s/AKfycbz1HuonSWyMiDUTHtzsW7hJF5uA8YahBYlODoK34OlMmDIYZTDuFWV8Jy72PZ_VeYQ-/exec") {
            url {
                parameters.append("action", "getData")
            }
        }

        // 2. Парсимо JSON-відповідь у список DTO
        val departmentsDto = json.decodeFromString<List<DepartmentDto>>(response.bodyAsText())

        // 3. Конвертуємо список DTO у список сутностей для бази даних
        val departments = departmentsDto.map { dto ->
            Department(
                // id генерується автоматично базою даних (autoGenerate = true)
                name = dto.name
            )
        }

        // 4. Очищуємо старі дані та вставляємо нові в одній транзакції
        departmentDao.clearAndInsertDepartments(departments)

        // Повертаємо успішний результат
        Result.success(Unit)

    } catch (e: Exception) {
        // Фіксація помилки в стек-трейсі та повернення типізованої помилки
        e.printStackTrace()
        Result.failure(e)
    }
}

/**
 * Отримує статус погодження (approve) або глобальний стан доступу з Google Apps Script.
 *
 * @return Рядок відповіді скрипта (наприклад, "approved", "pending" або прапорець дозволу).
 */
suspend fun getApprove(): String {
    val response: HttpResponse = client.get("https://script.google.com/macros/s/AKfycbz1HuonSWyMiDUTHtzsW7hJF5uA8YahBYlODoK34OlMmDIYZTDuFWV8Jy72PZ_VeYQ-/exec") {
        url {
            parameters.append("action", "getApprove")
        }
    }

    return response.bodyAsText()
}

/**
 * Реєструє факт переміщення, списання або видачі товару/запчастини в Google-таблиці.
 *
 * Відправляє POST-запит із JSON-корисним навантаженням на виділену точку прийому проводок.
 *
 * @param itemCode Код/артикул номенклатури.
 * @param item Назва товару/деталі.
 * @param amount Кількість одиниць.
 * @param departmentLocal Цільовий підрозділ (отримувач).
 * @param user ПІБ або табельний номер співробітника, який зафіксував операцію.
 * @return HTTP статус або відповідь сервера у текстовому форматі (наприклад, "OK").
 */
suspend fun postPart(itemCode: String, item: String, amount: String, departmentLocal: String, user: String): String {
    val response: HttpResponse = client.post("https://script.google.com/macros/s/AKfycbwGKWcS2c9q-BGJli0jOsnNa6heXAGbPM5dqFP6udyjARD-bd7CDroPnhMHwJOYhk7s/exec") {
        contentType(ContentType.Application.Json)
        setBody(
            """
            {
              "code": "$itemCode",
              "name": "$item",
              "amount": "$amount",
              "department": "$departmentLocal",
              "user": "$user"
            }
            """.trimIndent()
        )
    }
    val outputText = response.status.toString() // Повертає статус, наприклад "200 OK"
    return outputText
}

/**
 * Завантажує актуальний каталог запчастин/товарів із Google Sheets і транзакційно оновлює Room.
 *
 * КРОКИ ВИКОНАННЯ:
 * 1. Відправка GET-запиту з параметром `action=getParts`.
 * 2. Десеріалізація JSON у список [ProductDto].
 * 3. Конвертація у список сутностей [Product] (з прив'язкою id = code).
 * 4. Атомарне очищення та збереження нового списку в таблицю бази даних.
 *
 * @param productDao Об'єкт доступу до даних номенклатури Room.
 * @return [Result.success] у разі успішного завершення, або [Result.failure] при виникненні помилок.
 */
suspend fun updatePartsInDb(productDao: Dao): Result<Unit> {
    return try {
        // 1. Виконуємо GET-запит до Google Apps Script для отримання списку деталей
        val response: HttpResponse = client.get("https://script.google.com/macros/s/AKfycbz1HuonSWyMiDUTHtzsW7hJF5uA8YahBYlODoK34OlMmDIYZTDuFWV8Jy72PZ_VeYQ-/exec") {
            url {
                parameters.append("action", "getParts")
            }
        }

        // 2. Парсимо JSON-відповідь у список DTO
        val productsDto = json.decodeFromString<List<ProductDto>>(response.bodyAsText())

        // 3. Конвертуємо список DTO у список сутностей бази даних
        val products = productsDto.map { dto ->
            Product(
                id = dto.id,
                code = dto.id, // Використовуємо id як код номенклатури
                name = dto.name,
                amountInStore = "0" // За замовчуванням кількість встановлюється в 0 до перерахунку
            )
        }

        // 4. Очищуємо старі записи та вставляємо оновлені товари в межах однієї транзакції
        productDao.clearAndInsert(products)

        // Повертаємо успішний результат
        Result.success(Unit)

    } catch (e: Exception) {
        // Логування помилки та повернення відмови
        e.printStackTrace()
        Result.failure(e)
    }
}






