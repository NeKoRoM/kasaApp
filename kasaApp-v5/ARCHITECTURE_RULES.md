# 🏛️ Архітектурний кодекс KasaApp v5 (Gemini 3.8 Flash Optimized POS)

> **Статус:** Офіційний керівний інженерний стандарт та технічна онтологія проекту `kasaApp-v5`.  
> **Ключова мета:** Побудова промислового касового терміналу (Android-First POS) з чистим KMP-ядром, де **кожен архітектурний шар, інтерфейс та файл спроектовані під когнітивний профіль моделі Gemini 3.8 Flash**.  
> **Дата оновлення:** 2026 рік.

---

## 1. Фундаментальна філософія: Архітектура через призму Gemini 3.8 Flash

Шість місяців тому в правилах домінував монолітний Vertical Slice (*«помістити все в один файл billing-bundle.ts: БД, ендпоінти, UI та логіку»*).  
Для **Gemini 3.8 Flash** цей старий підхід є згубним:
* **Diff Bloat & Скорочення:** У файлах > 350 рядків висока швидкість виведення Flash-моделі провокує скорочення коду (`// ... залишок без змін ...`), що руйнує автоматичні дифи.
* **Контекстний шум:** Змішування Android Compose UI та SQL-запитів в одному файлі розмиває фокус уваги моделі, збільшуючи ризик помилок у податковій математиці.

### Наскрізний принцип KasaApp v5:
> **Кожне архітектурне рішення в системі (від розміру файлів до типізації) слугує єдиній меті: зробити так, щоб Gemini 3.8 Flash міг згенерувати або відредагувати будь-який компонент з математичною точністю 100%, перевірити його за 2 секунди через компілятор та не мати фізичної змоги сгалюцинувати.**

---

## 2. Когнітивний профіль Gemini 3.8 Flash у розрізі системи

| Характеристика Gemini 3.8 Flash | Що ламає модель | Як це вирішено наскрізно через усю архітектуру |
| :--- | :--- | :--- |
| **Блискавична швидкість виведення (High Throughput)** | Файли понад 300–400 рядків; спроба змінити 5 рядків посеред 2000 рядків коду. | **Жорсткий ліміт розміру файлів: 80–220 рядків.** Жоден файл проекту не перевищує 250 рядків. |
| **Миттєве сприйняття компактних онтологій** | Неявні типи (`Any?`), динамічні структури (`Map<String, Any>`), нерозмежовані інтерфейси. | **Файли-онтології (`Contract.kt`):** 50–70 рядків чистого опису стану, дій та ефектів для кожної фічі. |
| **Детермінізм лінійного потоку** | Вкладені конструкції `if / else` на 3–4 рівні, приховані мутації зовнішнього стану. | **Early Returns & No Else:** усі перевірки на початку, 100% лінійний потік зверху вниз. |
| **Компілятор як зовнішній мозок** | Відсутність швидких тестів, залежність бізнес-логіки від емулятора Android. | **Pure Kotlin Core + Flash-TDD:** ядро не має залежностей від Android SDK; тести проходять на JVM за 0.3 секунди. |
| **Схильність наслідувати скорочення** | Псевдо-коментарі `// TODO`, `// ...` у кодовій базі спонукають модель писати неповний код. | **Повна заборона заглушок:** кожен клас, метод та тест пишеться повністю, на 100% завершеним. |

---

## 3. Модульна анатомія: Ізоляція шуму для Gemini 3.8 Flash

Щоб Gemini 3.8 Flash ніколи не плутав нативні виклики Android із бізнес-правилами фіскалізації, система розділена на три чіткі рівні:

```
                                kasaApp-v5 (Root)
                                        │
        ┌───────────────────────────────┴───────────────────────────────┐
        ▼                                                               ▼
┌───────────────────────────────────────────┐       ┌───────────────────────────────────────────┐
│        PURE KOTLIN / KMP ЯДРО             │       │            ANDROID-FIRST ШАР              │
│    (:core-model, :core-protocol)          │       │                 (:app)                    │
├───────────────────────────────────────────┤       ├───────────────────────────────────────────┤
│ • 0% залежностей від Android SDK          │       │ • Jetpack Compose POS (планшет 10-12")    │
│ • Канонічні дата-класи (Product, Receipt) │       │ • Bluetooth Classic SPP сокети (RFCOMM)   │
│ • Кадрування Datecs, розрахунок BCC       │       │ • Bluetooth-ваги з live-дебаунсом         │
│ • Декодер 6 статусних байтів каси         │       │ • USB-HID сканер акцизних марок           │
│ • Фіскальна математика (ПДВ 20%, Акциз 5%)│       │ • Room KMP SQLite база даних              │
│ • Юніт-тести на JVM: 0.3 сек.             │       │ • Ktor HTTP клієнт до FastApi             │
└───────────────────────────────────────────┘       └───────────────────────────────────────────┘
```

### Чому цей поділ критичний для Gemini 3.8 Flash:
1. Коли модель працює над протоколом або чеком у `core-protocol`, вона фізично не бачить класів Android (`Context`, `BluetoothSocket`, `Toast`). Вона не може сгалюцинувати або заплутатися в життєвому циклі Activity.
2. Коли модель пише UI у модулі `app`, вона взаємодіє з протоколом тільки через типізований інтерфейс `KasaTransport`.

---

## 4. Специфіка Android-First: Апаратний POS-термінал через призму Flash

Додаток проектується під **Android-планшети 10–12 дюймів (Landscape, 1920x1200 / 2560x1600)** касира.  
Для моделі Gemini 3.8 Flash апаратний шар організований за принципом **«Чорної скриньки з типізованими подіями»**:

### 4.1. Драйвер РРО Exellio (Bluetooth Classic SPP)
* **Проблема для ШІ:** Bluetooth-сокети в Android нестабільні, виникають обриви зв'язку (`IOException`), фрагментація пакетів та підвисання.
* **Рішення для Flash:** Замість складних колбеків створюється простий скінченний автомат станів (FSM):
  $$\text{Disconnected} \longrightarrow \text{Connecting} \longrightarrow \text{Connected} \rightleftharpoons \text{Reconnecting}$$
* Gemini 3.8 Flash оперує виключно `StateFlow<ConnectionState>`.
* **Keep-Alive:** Кожні 45 секунд команда зчитування часу `003>` підтримує сокет живим.
* **Кодування:** Усі назви товарів конвертуються в `Windows-1251` (CP1251) байтовим маппером, що гарантує друк літер `І`, `Ї`, `Є`, `Ґ`.

### 4.2. Торгові ваги по Bluetooth
* Потік байтів з ваг парситься у фоновій корутині та видається моделі у вигляді строгого дата-класу:
  `data class ScaleWeight(val weightGrams: Int, val isStable: Boolean)`
* Моделі заборонено додавати ваговий товар у чек, якщо `isStable == false`.

### 4.3. Апаратний 2D-сканер та акцизні марки
* Сканер працює як апаратна клавіатура USB-HID (перехоплення `KeyEvent.KEYCODE_ENTER`).
* Товари з прапорцем `requiresExciseStamp == true` блокують оплату чека доти, доки касир не зсканує валідну марку (10-значний серійний номер або цифровий код DataMatrix).

### 4.4. Crash-Proof транзакційність у Room KMP
* Щоб уникнути втрати чека при раптовому вимкненні живлення планшета, кожна операція над відкритим чеком виконується в атомній `@Transaction` Room.
* Стан чека (`isFiscalCheckOpenInRro: Boolean`) завжди зберігається в БД.

---

## 5. Анатомія кадрового протоколу Datecs / Exellio (Чисте ядро)

Формат кадру:
$$\langle 0x01 \rangle + \text{Len} + \text{Seq} + \text{Cmd} + \langle 0x05 \rangle + \text{BCC} + \langle 0x03 \rangle$$

### 5.1. Алгоритм розрахунку BCC (Чистий Kotlin без сторонніх бібліотек)
```kotlin
package com.kasa.protocol.frame

/**
 * Розрахунок контрольної суми кадру Datecs для чистих тестів на JVM.
 * Оптимізовано для Gemini 3.8 Flash: відсутність сторонніх залежностей, явна побайтова математика.
 */
object DatecsBccCalculator {
    fun calculate(bytesFromLenToEnq: ByteArray): ByteArray {
        var sum = 0
        for (b in bytesFromLenToEnq) {
            sum += (b.toInt() and 0xFF)
        }
        val bcc = sum and 0xFFFF
        val bccBytes = ByteArray(4)
        bccBytes[0] = (((bcc ushr 12) and 0x0F) + 0x30).toByte()
        bccBytes[1] = (((bcc ushr 8) and 0x0F) + 0x30).toByte()
        bccBytes[2] = (((bcc ushr 4) and 0x0F) + 0x30).toByte()
        bccBytes[3] = ((bcc and 0x0F) + 0x30).toByte()
        return bccBytes
    }
}
```

### 5.2. Вичерпний DSL команд каси (Sealed Interface замість сирих рядків)
Gemini 3.8 Flash зобов'язана використовувати **тільки строгі класи команд**:

```kotlin
package com.kasa.protocol.command

sealed interface DatecsCommand {
    val rawCommandCode: String
    fun buildPayload(): String

    data object ReadDateTime : DatecsCommand {
        override val rawCommandCode: String = "003>"
        override fun buildPayload(): String = ""
    }

    data class SetDateTime(val formattedDateTime: String) : DatecsCommand {
        override val rawCommandCode: String = "003="
        override fun buildPayload(): String = "$formattedDateTime\t"
    }

    data object PrintXReport : DatecsCommand {
        override val rawCommandCode: String = "0045X"
        override fun buildPayload(): String = "\t"
    }

    data object PrintZReport : DatecsCommand {
        override val rawCommandCode: String = "0045Z"
        override fun buildPayload(): String = "\t"
    }

    data class OpenReceipt(val operatorPassword: Int = 1) : DatecsCommand {
        override val rawCommandCode: String = "0048"
        override fun buildPayload(): String = "1\t$operatorPassword\t"
    }

    data class RegisterSale(
        val name: String,
        val priceKopecks: Long,
        val quantity: Double,
        val taxGroupLetter: String
    ) : DatecsCommand {
        override val rawCommandCode: String = "0049"
        override fun buildPayload(): String {
            val priceStr = String.format(java.util.Locale.US, "%.2f", priceKopecks / 100.0)
            val qtyStr = String.format(java.util.Locale.US, "%.3f", quantity)
            return "$name\t$priceStr\t$qtyStr\t$taxGroupLetter\t"
        }
    }

    data class Total(val paymentType: Int, val amountKopecks: Long) : DatecsCommand {
        override val rawCommandCode: String = "0054"
        override fun buildPayload(): String {
            val amountStr = String.format(java.util.Locale.US, "%.2f", amountKopecks / 100.0)
            return "$paymentType\t$amountStr\t"
        }
    }

    data object CloseReceipt : DatecsCommand {
        override val rawCommandCode: String = "0056"
        override fun buildPayload(): String = ""
    }

    data object VoidReceipt : DatecsCommand {
        override val rawCommandCode: String = "0060"
        override fun buildPayload(): String = ""
    }
}
```

### 5.3. Повна онтологія 6 статусних байтів каси (`KasaStatus`)
```kotlin
package com.kasa.protocol.model

/**
 * Повний розбір 6 байтів стану Datecs.
 * Усі прапорці типізовані, що унеможливлює побічні галюцинації Gemini 3.8 Flash.
 */
data class KasaStatus(
    // Байт 0: Синтаксис і критичні помилки
    val hasSyntaxError: Boolean,
    val isInvalidCommand: Boolean,
    val isRtcError: Boolean,
    val isCommandNotAllowed: Boolean,
    val hasBccError: Boolean,
    // Байт 1: Термопринтер і механіка
    val isControlPaperOut: Boolean,
    val isReceiptPaperOut: Boolean,
    val isCoverOpen: Boolean,
    val isHeadOverheated: Boolean,
    // Байт 2: Фіскальний стан
    val isFiscalized: Boolean,
    val isFiscalMemoryFull: Boolean,
    val is24HoursShiftExpired: Boolean,
    // Байт 4: Режим чека
    val isNonFiscalReceiptOpen: Boolean,
    val isFiscalReceiptOpen: Boolean
) {
    val canPrintReceipt: Boolean
        get() = !isReceiptPaperOut && !isCoverOpen && !isHeadOverheated && !is24HoursShiftExpired
}
```

---

## 6. Канонічний шаблон фічі (Тріада Фічі для Gemini 3.8 Flash)

Кожна фіча у додатку займає **рівно одну папку** і розбивається на 3 компактні файли (кожен до 150 рядків).

### 6.1. `PosContract.kt` (Онтологія фічі — до 70 рядків)
```kotlin
package com.kasa.feature.pos

import com.kasa.model.Product
import com.kasa.model.ReceiptItem

data class PosState(
    val items: List<ReceiptItem> = emptyList(),
    val totalKopecks: Long = 0L,
    val isKasaOnline: Boolean = false,
    val scaleWeightGrams: Int = 0,
    val isScaleStable: Boolean = false,
    val isReceiptOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val isPaymentProcessing: Boolean = false,
    val errorBanner: String? = null
)

sealed interface PosEvent {
    data class ScanBarcode(val barcode: String) : PosEvent
    data class AddProduct(val product: Product, val quantity: Double = 1.0) : PosEvent
    data class UpdateQuantity(val itemIndex: Int, val quantity: Double) : PosEvent
    data class RemoveItem(val itemIndex: Int) : PosEvent
    data class EnterExciseStamp(val itemIndex: Int, val stamp: String) : PosEvent
    data object ClearReceipt : PosEvent
    data class PayCash(val cashTenderedKopecks: Long) : PosEvent
    data object PayCard : PosEvent
    data object PrintZReport : PosEvent
    data object PrintXReport : PosEvent
}

sealed interface PosEffect {
    data class ShowToast(val text: String) : PosEffect
    data class ShowErrorDialog(val title: String, val message: String) : PosEffect
    data class PromptExciseInput(val itemIndex: Int, val productName: String) : PosEffect
    data object OpenCashDrawer : PosEffect
}
```

### 6.2. `PosViewModel.kt` (Чиста машина станів — до 150 рядків)
```kotlin
package com.kasa.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PosViewModel : ViewModel() {

    private val _state = MutableStateFlow(PosState())
    val state: StateFlow<PosState> = _state.asStateFlow()

    private val _effect = Channel<PosEffect>(Channel.BUFFERED)
    val effect: Flow<PosEffect> = _effect.receiveAsFlow()

    // ВИЧЕРПНИЙ when без гілки else — компілятор Kotlin контролює Gemini 3.8 Flash
    fun onEvent(event: PosEvent) {
        when (event) {
            is PosEvent.ScanBarcode -> handleScanBarcode(event.barcode)
            is PosEvent.AddProduct -> handleAddProduct(event)
            is PosEvent.UpdateQuantity -> handleUpdateQuantity(event)
            is PosEvent.RemoveItem -> handleRemoveItem(event.itemIndex)
            is PosEvent.EnterExciseStamp -> handleEnterExcise(event)
            is PosEvent.ClearReceipt -> handleClearReceipt()
            is PosEvent.PayCash -> handlePayCash(event.cashTenderedKopecks)
            is PosEvent.PayCard -> handlePayCard()
            is PosEvent.PrintZReport -> handlePrintZ()
            is PosEvent.PrintXReport -> handlePrintX()
        }
    }

    private fun handleAddProduct(event: PosEvent.AddProduct) {
        if (event.quantity <= 0.0) {
            _state.update { it.copy(errorBanner = "Кількість має бути більшою за 0") }
            return
        }

        if (event.product.requiresExciseStamp) {
            viewModelScope.launch {
                _effect.send(PosEffect.PromptExciseInput(
                    itemIndex = _state.value.items.size,
                    productName = event.product.name
                ))
            }
        }
        // Додавання товару та лінійний перерахунок суми в копійках
    }

    private fun handleScanBarcode(barcode: String) { /* Пошук товару в Room */ }
    private fun handleUpdateQuantity(event: PosEvent.UpdateQuantity) { /* Оновлення кількості */ }
    private fun handleRemoveItem(index: Int) { /* Видалення позиції */ }
    private fun handleEnterExcise(event: PosEvent.EnterExciseStamp) { /* Прив'язка марки */ }
    private fun handleClearReceipt() { _state.update { it.copy(items = emptyList(), totalKopecks = 0L) } }
    private fun handlePayCash(kopecks: Long) { /* Фіскалізація готівкою */ }
    private fun handlePayCard() { /* Фіскалізація карткою */ }
    private fun handlePrintZ() { /* Друк Z-звіту */ }
    private fun handlePrintX() { /* Друк X-звіту */ }
}
```

---

## 7. Фіскальна математика України (Захист від Floating-Point галюцинацій)

Gemini 3.8 Flash суворо заборонено використовувати тип `Double` для зберігання вартості у копійках або підсумків чека.  
Усі розрахунки виконуються через строго типізований об'єкт `FiscalMath`:

```kotlin
package com.kasa.model.fiscal

/**
 * Нормативна фіскальна математика (ПКУ та Постанова НБУ № 25).
 * Усі суми передаються та повертаються в цілих копійках (Long).
 */
object FiscalMath {
    /**
     * Роздрібний акцизний податок 5% (включений у вартість підакцизного товару):
     * Акциз = Вартість * 5 / 105
     */
    fun calculateExciseKopecks(totalKopecks: Long): Long {
        return Math.round(totalKopecks * 5.0 / 105.0)
    }

    /**
     * ПДВ 20% від бази без акцизу:
     * База = Вартість - Акциз
     * ПДВ = База * 20 / 120 = База / 6
     */
    fun calculateVat20Kopecks(totalKopecks: Long, hasExcise: Boolean): Long {
        val baseWithoutExcise = if (hasExcise) {
            totalKopecks - calculateExciseKopecks(totalKopecks)
        } else {
            totalKopecks
        }
        return Math.round(baseWithoutExcise * 20.0 / 120.0)
    }

    /**
     * Округлення готівкового розрахунку за правилами НБУ:
     * 1..4 коп -> зменшення до 0 коп.
     * 5..9 коп -> збільшення до 10 коп.
     * Для безготівкової оплати (банківська картка) округлення суворо заборонено!
     */
    fun roundCashPayment(totalKopecks: Long): Long {
        if (totalKopecks == 0L) return 0L
        val sign = if (totalKopecks >= 0) 1 else -1
        val abs = Math.abs(totalKopecks)
        val lastDigit = (abs % 10).toInt()
        val base = abs - lastDigit
        val rounded = when {
            lastDigit in 1..4 -> base
            lastDigit in 5..9 -> base + 10
            else -> abs
        }
        return rounded * sign
    }
}
```

---

## 8. Bluetooth SPP: Дефрагментація чанків та буферизація

Для уникнення поломки сесії зв'язку при фрагментованій передачі через Bluetooth Classic впроваджується `DatecsByteFramer`:

```kotlin
package com.kasa.protocol.frame

import java.io.ByteArrayOutputStream

/**
 * Потоковий накопичувач байтів кадру Datecs.
 * Вирішує проблему фрагментації Bluetooth-пакетів.
 */
class DatecsByteFramer {
    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun appendAndExtractFrames(incomingBytes: ByteArray): List<ByteArray> {
        buffer.write(incomingBytes)
        val allBytes = buffer.toByteArray()
        val frames = mutableListOf<ByteArray>()
        var startIndex = 0

        while (startIndex < allBytes.size) {
            // Пошук SOH (0x01)
            val sohIndex = allBytes.indexOfByte(0x01.toByte(), startIndex)
            if (sohIndex == -1) {
                buffer.reset()
                break
            }

            // Пошук ETX (0x03) після SOH
            val etxIndex = allBytes.indexOfByte(0x03.toByte(), sohIndex + 1)
            if (etxIndex == -1) {
                // Кадр ще не до кінця отримано з сокету
                buffer.reset()
                buffer.write(allBytes, sohIndex, allBytes.size - sohIndex)
                break
            }

            // Повний кадр знайдено
            val frameLength = (etxIndex - sohIndex) + 1
            val completeFrame = ByteArray(frameLength)
            System.arraycopy(allBytes, sohIndex, completeFrame, 0, frameLength)
            frames.add(completeFrame)

            startIndex = etxIndex + 1
            if (startIndex >= allBytes.size) {
                buffer.reset()
            }
        }
        return frames
    }

    private fun ByteArray.indexOfByte(target: Byte, fromIndex: Int): Int {
        for (i in fromIndex until this.size) {
            if (this[i] == target) return i
        }
        return -1
    }
}
```

---

## 9. Схема бази даних Room KMP (Offline-First)

Заборонено зберігати чек як нетипізований JSON-рядок у колонці SQLite.  
Схема строго нормалізована:

```
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│         receipts                │   1 : N   │        receipt_items            │
├─────────────────────────────────┤           ├─────────────────────────────────┤
│ id: String (UUID, PK)           │──────────<│ id: Long (PK, Autoincrement)    │
│ createdAt: Long                 │           │ receiptId: String (FK)          │
│ cashierId: Long                 │           │ productId: Long                 │
│ totalKopecks: Long              │           │ productName: String             │
│ isFiscalized: Boolean           │           │ priceKopecks: Long              │
│ fiscalCheckNumber: String?      │           │ quantity: Double                │
│ paymentType: Int                │           │ exciseStamp: String?            │
│ isSyncedWithServer: Boolean     │           │ taxGroupLetter: String          │
└─────────────────────────────────┘           └─────────────────────────────────┘
```

* **Індекси:** Створюються обов'язкові індекси на `receipt_items.receiptId` та `receipts.isSyncedWithServer`.
* **Чому це важливо для Gemini 3.8 Flash:** Модель генерує бездоганні SQL-запити до типізованих DAO, тоді як десеріалізація JSON-блобів всередині запитів Room призводить до помилок.

---

## 10. Синхронізація з FastAPI бекендом

1. **Авторизація:** RS256 JWT (`POST /jwt/login/`), збереження токена у безпечному сховищі.
2. **Завантаження товарів:** `GET /products/?store_id={id}` (оновлення каталогу).
3. **Офлайн-черга фіскалізованих чеків:**
   * Коли чек роздруковано на касі Exellio, він позначається `isFiscalized = true`.
   * Якщо є інтернет — чек негайно відправляється на сервер (`POST /orders/`).
   * Якщо інтернету немає — чек потрапляє в чергу невідправлених чеків і синхронізується у фоні при відновленні зв'язку.

---

## 11. Наскрізний протокол роботи Gemini 3.8 Flash (Execution Protocol)

Перед кожною зміною чи додаванням коду Gemini 3.8 Flash зобов'язана слідувати цьому алгоритму:

1. **[ ] Перевірка ліміту рядків:** Чи файл, який редагується або створюється, не перевищує **220 рядків**? Якщо перевищує — розділити на окремі файли за Тріадою Фічі.
2. **[ ] Перевірка вичерпності:** Чи у виразах `when` для `sealed interface` відсутня гілка `else`?
3. **[ ] Чистота ядра:** Чи у модулях `core-protocol` та `core-model` **повністю відсутні** імпорти `android.*`?
4. **[ ] Цілочисельна математика:** Чи всі грошові величини представлені у копійках (`Long`) згідно з `FiscalMath`?
5. **[ ] Flash-TDD валідація:** Запустити `./gradlew test` у терміналі та переконатися, що всі тести проходять із кодом 0 за лічені секунди.
