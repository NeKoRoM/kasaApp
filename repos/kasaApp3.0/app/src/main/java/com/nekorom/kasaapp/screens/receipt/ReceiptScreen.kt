package com.nekorom.kasaapp.screens.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.domain.models.receipt.entites.ProductForReceipt
import com.nekorom.kasaapp.domain.models.receipt.entites.Receipt
import com.nekorom.kasaapp.screens.tablet.SaleItem
import java.util.Date
import kotlin.time.Duration.Companion.days

/**
 * Екран історії та перегляду фіскальних чеків (Receipt History Screen).
 *
 * Організований у вигляді дворівневого списку (Accordion):
 * 1. Верхній рівень — групування за календарними днями ([DayItem]).
 * 2. Середній рівень — окремі чеки за обраний день із вказанням точного часу та суми ([ReceiptItem]).
 * 3. Нижній рівень — товарні позиції всередині обраного чека через компонент [SaleItem].
 */
@Composable
fun ReceiptScreen() {
    val rVm: ReceiptViewModel = hiltViewModel()
    val receiptList = rVm.getReceipts().observeAsState().value
    val days: Set<Int>? = receiptList?.map { Date(it.createdAt).date }?.toSet()
    val month = receiptList?.map { Date(it.createdAt).month + 1 }
    val year = receiptList?.map { Date(it.createdAt).year }

    Scaffold { values ->
        LazyColumn(modifier = Modifier.padding(values)) {
            receiptList?.let { receipts ->
                days?.let { days ->
                    items(days.toList()) { day ->
                        DayItem(
                            receipts = receipts
                                .map { it.toReceipt() }
                                .filter { Date(it.createdAt).date == day },
                            day = day
                        )
                    }
                }
            }
        }
    }
}

/**
 * Елемент списку за один календарний день.
 *
 * Підраховує та показує загальний грошовий оборот за день ([sum]).
 * При розгортанні виводить перелік усіх проведених чеків цього дня.
 *
 * @param receipts Список чеків [Receipt] за даний день.
 * @param day Номер дня місяця.
 */
@Composable
fun DayItem(receipts: List<Receipt>, day: Int) {
    var sum = 0.0
    receipts.forEach {
        sum += it.getPrice()
    }
    var addInfoState by remember {
        mutableStateOf(false)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "День: $day")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "сума:  ")
                Text(text = String.format("%.2f грн", sum), fontSize = 18.sp)
                IconButton(onClick = { addInfoState = !addInfoState }) {
                    if (addInfoState)
                        Icon(Icons.TwoTone.ArrowDropUp, "")
                    else
                        Icon(Icons.TwoTone.ArrowDropDown, "")
                }
            }
        }
        if (addInfoState) {
            Column {
                receipts.forEach {
                    ReceiptItem(receipt = it)
                }
            }
        }
    }
}

/**
 * Картка конкретного фіскального чека.
 *
 * Показує час створення та суму. При натисканні розгортає детальний список товарів у чеку ([SaleItem]).
 *
 * @param receipt Об'єкт чека [Receipt].
 */
@Composable
fun ReceiptItem(receipt: Receipt) {
    var addInfoState by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = receipt.getDate())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "сума:  ")
                Text(text = receipt.getPrice().toString() + " грн ", fontSize = 18.sp)
                IconButton(onClick = { addInfoState = !addInfoState }) {
                    if (addInfoState)
                        Icon(Icons.TwoTone.ArrowDropUp, "")
                    else
                        Icon(Icons.TwoTone.ArrowDropDown, "")
                }
            }
        }
        if (addInfoState) {
            Column {
                receipt.products.forEach {
                    SaleItem(
                        item = it,
                        onDelete = { },
                        onAmountChange = {},
                        amount = it.amount.toString(),
                        group = listOf(),
                        onExciseChange = {}
                    )
                }
            }
        }
    }
}




@Preview(showSystemUi = true, showBackground = true)
@Composable
private  fun PrevReceiptScreen() {
    ReceiptScreen()

}