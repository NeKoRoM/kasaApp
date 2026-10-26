# Путівник завантаженими бібліотеками, документацією та прикладами реалізації Datecs / Exellio

> **Призначення:** Огляд завантажених виробничих бібліотек, офіційних мануалів Datecs Ltd та готових еталонних прикладів реалізації для інтеграції в екосистему **KasaApp**.

---

## 1. Завантажена технічна документація (`docs/datecs_exellio/manuals_and_specs/`)

У цій папці зібрано **21 офіційний документ** від виробника Datecs Ltd (англійською та болгарською мовами):

| Назва файлу | Тип пристрою / Протокол | Ключовий зміст |
| :--- | :--- | :--- |
| **`PM_CommunicationProtocol_v2.11_eng.pdf`** | Лінійка X (DP-25X, FP-700X, WP-50X) | Офіційна англомовна специфікація 2021 р. протоколу v2.11 (159 стор.). |
| **`CommunicationProtocol_FiscalDevices_GroupA_EN.pdf`** | Група A (FP-700, FP-2000, FMP-350) | Англомовний протокол зв'язку для швидкісних реєстраторів. |
| **`CommunicationProtocol_FiscalDevices_GroupC_EN.pdf`** | Група C (Портативні каси DP-25, DP-05) | Англомовний протокол для компактних касових апаратів. |
| **`UM_DP25X_-BUL.pdf`** | Exellio / Datecs DP-25X (12 MB) | Повний посібник користувача та сервісне меню касового апарата DP-25X. |
| **`FP 3530-05 Command.pdf`** | Exellio FP-3530T / Datecs FP-3530 | Класичний фіскальний реєстратор, специфікація команд протоколу Krypton. |
| **`FP550_user.pdf` / `KORISNICKO-UPUTSTVO-FP-550...`**| Exellio FPU-550 / Datecs FP-550 | Посібник з експлуатації та команди для FPU-550. |
| **`StornoParams_English.pdf`** | Усі пристрої Datecs | Англомовна інструкція щодо параметрів сторнування та повернень чеків. |
| **`Possible_ErrorCodes.xls`** | Усі пристрої Datecs | Офіційна таблиця Excel усіх кодів помилок апаратів Datecs від вендора. |
| **`IU_D500PL-BUL-ECRServerProtocol_v3_TCP-IP.pdf`** | DP-500, WP-500, DP-150 | Протокол зв'язку ECRServer через TCP-IP порт. |
| **`RoundingInFiscalDevices_Bulgaria_v1.0.4.pdf`** | Алгоритми округлення сум | Математика фіскального округлення чека при множенні ціни на вагу. |

---

## 2. Завантажені бібліотеки та відкриті шлюзи (`docs/datecs_exellio/implementations/`)

### 2.1. `ErpNet.FP` (C# / .NET Core, Cross-Platform)
* **Розташування:** [`docs/datecs_exellio/implementations/ErpNet.FP/`](file:///Users/neko/Documents/PlatformIO/Projects/kasaApp/docs/datecs_exellio/implementations/ErpNet.FP/)
* **Ключові драйвери:**
  * `ErpNet.FP.Core/Drivers/BgDatecs/BgDatecsXIslFiscalPrinter.cs` — **еталонна реалізація Datecs Protocol v2 (X-line: DP-25X, FP-700X)**.
  * `ErpNet.FP.Core/Drivers/BgDatecs/BgDatecsXIslFiscalPrinter.Frame.cs` — низькорівневе пакування кадру: SOH, нібли, BCC, обробка квитанцій SYN (`0x16`).
  * `ErpNet.FP.Core/Drivers/BgDatecs/BgDatecsCIslFiscalPrinter.cs` — підтримка класичного протоколу Datecs v1 (DP-05, DP-25, DP-50).
* **Чому це важливо для KasaApp:**
  * Реалізовано складні сценарії: комбіновані оплати (готівка + картка), розбиття оплат, робота з підакцизними товарами та перевірка 24-годинного таймера зміни.

### 2.2. `fpgate` (Java / Spring / Application Server)
* **Розташування:** [`docs/datecs_exellio/implementations/fpgate/`](file:///Users/neko/Documents/PlatformIO/Projects/kasaApp/docs/datecs_exellio/implementations/fpgate/)
* **Ключові класи:**
  * `FPGateSrv/src/main/java/org/eda/protocol/DeviceDatecsXV1.java` — чиста Java-реалізація кодування кадру Datecs X.
  * `FPGateSrv/src/main/java/org/eda/fdevice/FPCDatecsX.java` — бізнес-логіка касових операцій.
  * `FPGateSrv/src/main/java/org/eda/protocol/FDExceptionDatecsX.java` — обробка числових помилок реєстратора.
* **Чому це важливо:**
  * Код написаний на Java і практично 1-в-1 переноситься в Kotlin / Android для модуля `:bt` або цільового `kasaApp-v5`.

### 2.3. `UcsFiscalPrinters` (REST-шлюз та класи протоколів)
* **Розташування:** [`docs/datecs_exellio/implementations/UcsFiscalPrinters/`](file:///Users/neko/Documents/PlatformIO/Projects/kasaApp/docs/datecs_exellio/implementations/UcsFiscalPrinters/)
* **Ключові файли:**
  * `src/UcsFP20/Protocols/cDatecsProtocol.cls` — парсинг протоколу Datecs.
  * Містить архів оригінальної документації вендора в `doc/Datecs/`.

---

## 3. Готові перевірені приклади для швидкої розробки (`docs/datecs_exellio/examples/`)

1. **[`KotlinDatecsClientExample.kt`](file:///Users/neko/Documents/PlatformIO/Projects/kasaApp/docs/datecs_exellio/examples/KotlinDatecsClientExample.kt):**
   * Чистий Kotlin (Android-незалежний) клієнт.
   * Працює з будь-якими `InputStream` та `OutputStream` (Bluetooth SPP сокет або TCP-сокет).
   * Повністю реалізовано:
     * Datecs Nibble Encoding (`nibble + 0x30`).
     * Генерація SEQ (`0x40..0x7C`).
     * Розрахунок контрольної суми BCC.
     * Реактивне очікування байтів `SYN (0x16)` кожні 60 мс.
     * Розбір 8 статусних байтів (`hasPaperEnd`, `isCoverOpen`, `isBlocked24Hours`, `isFiscalized`).
2. **[`PythonDatecsClientExample.py`](file:///Users/neko/Documents/PlatformIO/Projects/kasaApp/docs/datecs_exellio/examples/PythonDatecsClientExample.py):**
   * Скрипт для діагностики та тестування реального апарата з консолі через TCP сокет 4999 або Bluetooth RFCOMM.
