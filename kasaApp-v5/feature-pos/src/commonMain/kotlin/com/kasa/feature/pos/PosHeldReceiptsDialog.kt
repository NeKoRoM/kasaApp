package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.HeldReceipt

/**
 * Діалогове вікно перегляду, відновлення та видалення відкладених чеків касира (Android та iOS).
 */
@Composable
fun PosHeldReceiptsDialog(
    heldReceipts: List<HeldReceipt>,
    onRestore: (heldReceiptId: String) -> Unit,
    onDelete: (heldReceiptId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 Відкладені чеки (${heldReceipts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxListHeight = if (maxHeight < 500.dp) 180.dp else 460.dp
                val minListHeight = if (maxHeight < 500.dp) 100.dp else 200.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minListHeight, max = maxListHeight)
                ) {
                    if (heldReceipts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📦", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Немає відкладених чеків",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Щоб відкласти поточне замовлення, відкрийте меню дій каси та виберіть 'Відкласти чек'.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(heldReceipts, key = { it.id }) { held ->
                                PosHeldReceiptCard(
                                    heldReceipt = held,
                                    onRestore = { onRestore(held.id) },
                                    onDelete = { onDelete(held.id) }
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
                Text("Закрити", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 540.dp)
    )
}
