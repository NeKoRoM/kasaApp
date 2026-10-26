package nekorom.com

import ProductRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import database.Dao
import database.Department
import database.Product
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import nekorom.com.uikit.KasaAppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import nekorom.com.network.*
import nekorom.com.repo.DepartmentRepository

/**
 * ====================================================================================================
 * ГОЛОВНИЙ ЕКРАН СКЛАДСЬКОГО ОБЛІКУ ТА ПЕРЕМІЩЕННЯ ТОВАРІВ (COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Даний компонент є центральним робочим місцем оператора/комірника в додатку "Magazyn".
 * Працює на платформах Android, Desktop (JVM) та WasmJS завдяки технології Compose Multiplatform.
 *
 * БІЗНЕС-ПРИЗНАЧЕННЯ:
 * - Фіксація видачі запчастин, інструментів та матеріалів співробітникам або цехам.
 * - Локальний польськомовний інтерфейс цехового обліку (dział, przedmiot, kod, ilosc, użytkownik).
 * - Автономне кешування номенклатури в локальній базі Room з реактивними потоками (Flow).
 * - Фонова синхронізація довідників відділів і номенклатури з Google Sheets через Google Apps Script API.
 * - Відправка фактичних проводок списання/видачі безпосередньо в онлайн-таблицю Google Sheets.
 *
 * АРХІТЕКТУРА:
 * - [settings]: мультиплатформні налаштування (запам'ятовування імені користувача).
 * - [dao]: доступ до таблиць Room (Product, Department, Person).
 * - [ProductRepository], [DepartmentRepository]: репозиторії, що транслюють Flow із бази даних.
 * - [LazyVerticalStaggeredGrid]: інтерактивна сітка номенклатури зі швидким пошуком за назвою або артикулом.
 */
@OptIn(InternalAPI::class)
@Composable
@Preview
fun KasaApp(
    settings: List<SettingConfig<*>>,
    onClearSettings: () -> Unit,
    dao: Dao
) {
    // Корутиновий скоуп для запуску асинхронних мережевих дій (відправка запитів postPart)
    val scope = rememberCoroutineScope()

    // Ініціалізація доменних репозиторіїв із кешуванням у пам'яті
    val productRepository = remember { ProductRepository(dao) }
    val departmentRepository = remember { DepartmentRepository(dao) }

    // Локальні реактивні стани для довідників та списку деталей
    var department by remember { mutableStateOf("") }
    var departmentList by remember { mutableStateOf(listOf("ff")) }
    var products by remember { mutableStateOf(emptyList<Product>()) }

    // Підписка на реактивний потік товарів із локальної БД Room
    LaunchedEffect(Unit) {
        productRepository.allProducts.collect { updatedProductList ->
            products = updatedProductList
        }
    }

    // Фонова одноразова синхронізація каталогів із віддаленим Google Apps Script
    LaunchedEffect(Unit) {
        updateDepartmentsInDb(dao)
        updatePartsInDb(dao)
    }

    // Підписка на реактивний потік відділів/цехів із локальної БД Room
    LaunchedEffect(Unit) {
        departmentRepository.allDepartment.collect { updatedDepartmentList ->
            departmentList = updatedDepartmentList.map { it.name }
        }
    }

    KasaAppTheme {
        Scaffold {
            // Отримання поточної конфігурації користувача
            var settingConfig by remember { mutableStateOf(settings.first()) }
            var outputText by remember { mutableStateOf("200 OK") }

            // Поля форми видачі матеріалів зі збереженням стану при реконфігурації (rememberSaveable)
            var user by rememberSaveable { mutableStateOf(settingConfig.get()) }
            var departmentLocal by rememberSaveable { mutableStateOf("-||-") }
            var expanded by rememberSaveable { mutableStateOf(true) } // Стан видимості випадаючого списку відділів
            var item by rememberSaveable { mutableStateOf("") }
            var itemCode by rememberSaveable { mutableStateOf("") }
            var amount by rememberSaveable { mutableStateOf("1") }

            Column {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 35.dp)) {

                    // Рядок вибору підрозділу (dział) та скидання форми (zeruj)
                    Row {
                        Box(Modifier.weight(2f)) {
                            TextField(
                                readOnly = true,
                                value = departmentLocal,
                                modifier = Modifier.clickable(onClick = { expanded = true }),
                                onValueChange = {},
                                label = { Text("dział") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                        contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                                        modifier = Modifier.clickable {
                                            expanded = !expanded
                                        }
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                departmentList.forEach { label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            departmentLocal = label
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Кнопка очищення ("zeruj") - скидає поля введеного товару та повертає кількість в "1"
                        Button(
                            onClick = { itemCode = ""; item = ""; amount = "1" },
                            Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCD1B1B)
                            )
                        ) {
                            Text("zeruj")
                        }
                    }

                    // Поле введення назви товару (przedmiot)
                    TextField(
                        value = item,
                        onValueChange = { item = it },
                        singleLine = true,
                        label = { Text("przedmiot") }
                    )

                    // Рядок введення коду деталі та кількості
                    Row {
                        TextField(
                            value = itemCode,
                            onValueChange = { itemCode = it },
                            singleLine = true,
                            label = { Text("kod przedmiotu") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        TextField(
                            value = amount,
                            onValueChange = { amount = it },
                            singleLine = true,
                            label = { Text("ilosc") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Рядок ідентифікації користувача та відправки проводки
                    Row {
                        TextField(
                            modifier = Modifier.fillMaxWidth().weight(2.5f),
                            value = user,
                            onValueChange = {
                                user = it
                                settingConfig.set(user) // Персистентне збереження користувача в налаштуваннях
                            },
                            singleLine = true,
                            label = { Text("użytkownik") }
                        )

                        // Якщо немає активного мережевого запиту — показуємо кнопку відправки "Wyślij"
                        if (outputText == "200 OK") {
                            Button(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                onClick = {
                                    outputText = ""
                                    scope.launch {
                                        outputText = postPart(
                                            itemCode,
                                            item,
                                            amount,
                                            departmentLocal,
                                            user
                                        )
                                    }
                                }
                            ) {
                                Text(modifier = Modifier.fillMaxWidth(), text = "Wyślij")
                            }
                        } else {
                            // Індикатор прогресу під час відправки POST-запиту в Google Apps Script
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxWidth().weight(2.5f),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }

                // Динамічна фільтрація каталогу товарів за введеним кодом або назвою
                val filteredProducts = if (itemCode.isNotBlank() || item.isNotBlank()) {
                    products.filter {
                        (itemCode.isBlank() || it.code.contains(itemCode, ignoreCase = true)) &&
                                (item.isBlank() || it.name.contains(item, ignoreCase = true))
                    }
                } else {
                    emptyList()
                }

                // Адаптивна сітка знайдених товарів: клік на картку підставляє код та назву у форму
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(100.dp),
                    verticalItemSpacing = 1.dp,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    filteredProducts.forEach {
                        item {
                            ItemCard(it) {
                                itemCode = it.code
                                item = it.name
                            }
                        }
                    }
                }

                // Відображення статусу відповіді сервера внизу екрана
                Text(text = outputText, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}


