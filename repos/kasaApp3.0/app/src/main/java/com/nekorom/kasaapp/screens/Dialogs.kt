package com.nekorom.kasaapp.screens

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp

/**
 * Універсальне модальне вікно сповіщення / підтвердження (Material3 [AlertDialog]).
 *
 * Використовується в додатку для:
 * - Попередження про неактивну ліцензію касира або блокування адміністратором.
 * - Помилки невідповідності каси (`USER_ERR`).
 * - Системних сповіщень Bluetooth та зв'язку з Datecs РРО.
 *
 * @param onDismissRequest Дія при закритті діалогу або натисканні за його межами.
 * @param onConfirmation Дія при натисканні кнопки підтвердження (ОК).
 * @param dialogTitle Заголовок повідомлення.
 * @param dialogText Детальний текст повідомлення.
 * @param icon Піктограма діалогу (за замовчуванням [Icons.Default.Message]).
 * @param confirmButtonText Текст кнопки підтвердження ("ОК").
 * @param dismissButtonText Текст кнопки відхилення ("Вийти").
 * @param titleColor Колір заголовка.
 */
@Composable
fun MyDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: Painter = rememberVectorPainter(image = Icons.Default.Message),
    confirmButtonText: String ="ОК",
    dismissButtonText: String = "Вийти",
    titleColor:  androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Dialog Icon", modifier = Modifier.size(40.dp))
        },
        title = {
            Text(text = dialogTitle, color = titleColor)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(dismissButtonText)
            }
        }
    )
    
}

@Preview(showBackground = true, showSystemUi = false,)
@Composable
private fun DFRP() {
    DialogForReceipt(
        300.0,
        {},
        {},
        {}
    )
}

/**
 * Модальний касовий діалог оплати та розрахунку решти (Payment & Change Calculator).
 *
 * Призначений для швидкого обслуговування клієнта на касі:
 * 1. Показує підсумкову суму до сплати ([sum]).
 * 2. Надає швидкі кнопки вибору внесеної клієнтом купюри (100, 200, 500, 1000 грн).
 * 3. Автоматично розраховує та виводить суму решти: `banknote - sum`.
 * 4. Запитує підтвердження друку фіскального чека на РРО:
 *    - "Так": надсилає пакет чека в РРО з фіскалізацією ([onConfirmation]).
 *    - "Ні": зберігає операцію локально без фізичного друку чека ([onNoConfirmation]).
 *    - "Назад": скасування оплати та повернення до кошика ([onDismissRequest]).
 *
 * @param sum Загальна сума чека до сплати в гривнях.
 * @param onDismissRequest Відхилення та повернення назад.
 * @param onConfirmation Підтвердження друку фіскального чека в РРО.
 * @param onNoConfirmation Завершення чека без фізичного друку.
 */
@Composable
fun DialogForReceipt(
    sum: Double,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    onNoConfirmation: () -> Unit
) {
    var banknote by remember {
        mutableStateOf(100.0)
    }

    Dialog(onDismissRequest = {onDismissRequest() }, properties = DialogProperties()) {
        Card {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "До оплати")
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onPrimary),
                    text = sum.toString() + " грн", fontSize = 30.sp
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Готівка")
                        Text(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onPrimary),
                            text = banknote.toString() + " грн", fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(text = "решта")
                        Text(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onPrimary),
                            text = String.format("%.2f",banknote - sum) + " грн", fontSize = 30.sp
                        )
                    }
                }
                Text(text = "Номінали купюр")
                Row {
                    Button(onClick = { banknote = 100.0 }) {
                        Text(text = "100")
                    }
                    Spacer(modifier = Modifier.size(5.dp))
                    Button(onClick = { banknote = 200.0 }) {
                        Text(text = "200")
                    }
                    Spacer(modifier = Modifier.size(5.dp))

                    Button(onClick = { banknote = 500.0 }) {
                        Text(text = "500")
                    }
                    Spacer(modifier = Modifier.size(5.dp))
                    Button(onClick = { banknote = 1000.0 }) {
                        Text(text = "1000")
                    }
                }
                Text(text = "Видрукувати чек?")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { onDismissRequest() }) {
                        Text(text = "Назад")
                    }
                    TextButton(onClick = { onNoConfirmation() }) {
                        Text(text = "Ні")
                    }
                    TextButton(onClick = { onConfirmation() }) {
                        Text(text = "Так")
                    }
                }
            }
        }
    }
}