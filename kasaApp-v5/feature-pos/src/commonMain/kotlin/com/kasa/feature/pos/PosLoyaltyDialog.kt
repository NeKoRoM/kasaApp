package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Customer

/**
 * Модальне вікно пошуку та вибору клієнта програми лояльності.
 * Дозволяє знайти клієнта за ПІБ, номером телефону або штрих-кодом картки лояльності.
 */
@Composable
fun PosLoyaltyDialog(
    customers: List<Customer>,
    currentCustomer: Customer?,
    onSelectCustomer: (Customer) -> Unit,
    onDetachCustomer: () -> Unit,
    onOpenScanner: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val filteredCustomers = remember(customers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            customers
        } else {
            val digitsOnly = q.replace(Regex("[^0-9]"), "")
            customers.filter { c ->
                c.name.contains(q, ignoreCase = true) ||
                    (digitsOnly.isNotBlank() && c.phone.contains(digitsOnly)) ||
                    c.cardBarcode.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 Лояльність",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentCustomer != null) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDetachCustomer()
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Відкріпити", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 360.dp
                val maxListHeight = if (maxHeight < 500.dp) 180.dp else 460.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = if (isNarrow) "Пошук клієнта..." else "Пошук за ім'ям, телефоном або карткою...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            if (filteredCustomers.size == 1) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectCustomer(filteredCustomers.first())
                            }
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(40.dp)) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (onOpenScanner != null) {
                                IconButton(onClick = onOpenScanner, modifier = Modifier.size(40.dp)) {
                                    Text("📷", fontSize = 15.sp)
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👤", fontSize = 28.sp)
                            Text(
                                text = if (searchQuery.isNotBlank()) "Клієнта за запитом '$searchQuery' не знайдено" else "Список клієнтів порожній",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Text("Очистити пошук", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCustomers) { customer ->
                            PosLoyaltyCustomerCard(
                                customer = customer,
                                isSelected = currentCustomer?.id == customer.id,
                                onSelect = { selected -> onSelectCustomer(selected) }
                            )
                        }
                    }
                }
            }
        }
    },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
            ) {
                Text("Закрити", maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
    )
}
