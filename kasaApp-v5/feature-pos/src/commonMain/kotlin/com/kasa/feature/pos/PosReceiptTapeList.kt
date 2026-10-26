package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.ReceiptItem

/**
 * Стрічка списку товарних позицій чека:
 * - Плавний свайп-жест для швидкого видалення товару одним пальцем;
 * - Сенсорний степер кількості [-] QTY [+] прямо на рядку;
 * - Швидкий доступ до знижки, зміни ціни, сканування акцизу та редагування картки товару;
 * - Інформативна підказка жесту змахування для нових касирів.
 */
@Composable
fun PosReceiptTapeList(
    items: List<ReceiptItem>,
    catalogProducts: List<Product>,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(items.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Позицій: ${items.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "⇦ Свайп для видалення",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.productId}_${item.exciseStamp ?: ""}_$index" }
            ) { index, item ->
                PosReceiptSwipeableRow(
                    item = item,
                    onQuantityChange = { qty -> onEvent(PosEvent.UpdateQuantity(index, qty)) },
                    onRemove = { onEvent(PosEvent.RemoveItem(index)) },
                    onPromptExcise = { onEvent(PosEvent.PromptExciseStamp(index)) },
                    onPromptDiscount = { onEvent(PosEvent.PromptItemDiscount(index)) },
                    onPromptQuantity = { onEvent(PosEvent.PromptQuantity(index)) },
                    onPromptPrice = { onEvent(PosEvent.PromptItemPrice(index)) },
                    onEditProduct = {
                        catalogProducts.find { p -> p.id == item.productId }?.let { onEvent(PosEvent.PromptProductEdit(it)) }
                    },
                    onOpenScanner = {
                        onEvent(PosEvent.PromptExciseStamp(index))
                        onEvent(PosEvent.ToggleScannerDialog(true))
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
