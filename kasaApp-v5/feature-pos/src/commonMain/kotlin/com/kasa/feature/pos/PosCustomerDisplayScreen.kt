package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.ReceiptItem

/**
 * Екран покупця (Customer-Facing Second Display / Pole Display).
 * Для виведення на другий монітор POS-термінала, VFD-дисплей або планшет покупця.
 */
@Composable
fun PosCustomerDisplayScreen(
    state: PosState,
    modifier: Modifier = Modifier,
    storeName: String = "KasaApp Retail"
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // ВЕРХНІЙ БАР: Логотип магазину та статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(text = storeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Чек #${state.activeCartIndex + 1} • Каса працює", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                if (state.isFiscalProcessing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("⏳ Друкується чек...", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ЦЕНТР: Вітання або чекова стрічка
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.items.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = state.customerDisplayLine1.ifBlank { "👋 Ласкаво просимо!" }, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = state.customerDisplayLine2.ifBlank { "Каса готова до обслуговування" }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                state.currentShift?.cashierName?.let {
                                    Text(text = "Вас обслуговує касир: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                } else {
                    CustomerReceiptList(items = state.items, modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // НИЖНІЙ ПІДСУМОК ДО ОПЛАТИ
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompactWidth = maxWidth < 480.dp
                    val priceFontSize = if (isCompactWidth) 20.sp else 26.sp
                    val innerPadding = if (isCompactWidth) PaddingValues(horizontal = 10.dp, vertical = 8.dp) else PaddingValues(horizontal = 16.dp, vertical = 12.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(innerPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(text = "ДО СПЛАТИ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, softWrap = false)
                            if (state.receiptDiscountPercent > 0.0) {
                                val savedKopecks = state.rawTotalKopecks - state.totalPayableKopecks
                                val discPct = if (state.receiptDiscountPercent % 1.0 == 0.0) state.receiptDiscountPercent.toInt().toString() else state.receiptDiscountPercent.toString()
                                Text("Знижка $discPct% (економія ${PosCurrencyFormatter.formatKopecks(savedKopecks)} грн)", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                            if (state.changeKopecks > 0L) {
                                Text("💵 Отримано: ${PosCurrencyFormatter.formatKopecks(state.receivedCashKopecks ?: 0L)} грн | Здача: ${PosCurrencyFormatter.formatKopecks(state.changeKopecks)} грн", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                            state.lastFiscalCheckNumber?.let {
                                Text("✅ Чек № $it фіскалізовано", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${PosCurrencyFormatter.formatKopecks(state.totalPayableKopecks)} грн",
                            fontSize = priceFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerReceiptList(items: List<ReceiptItem>, modifier: Modifier = Modifier) {
    val lastItem = items.lastOrNull()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lastItem?.let { item ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("Остання позиція:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        Text(item.productName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${item.quantity} ${item.unitOfMeasurement} × ${PosCurrencyFormatter.formatKopecks(item.priceKopecks)} грн", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${PosCurrencyFormatter.formatKopecks(item.totalKopecks)} грн", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(items) { itm ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(itm.productName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${itm.quantity} × ${PosCurrencyFormatter.formatKopecks(itm.priceKopecks)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${PosCurrencyFormatter.formatKopecks(itm.totalKopecks)} грн", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}
