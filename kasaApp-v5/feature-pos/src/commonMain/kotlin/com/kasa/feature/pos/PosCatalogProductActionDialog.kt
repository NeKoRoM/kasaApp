package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import java.util.Locale

/**
 * Контекстне меню дій над карткою товару (Android та iOS):
 * - Відображає детальну картку товару (ціна, залишок, штрихкод);
 * - Швидкий виклик редагування, зміни залишку (Quick Stock), цінника та дублювання;
 * - Працює як для візуальної сітки (Grid), так і для списку (List);
 * - Touch-target >= 44 dp з тактильним вібровідгуком.
 */
@Composable
fun PosCatalogProductActionDialog(
    product: Product,
    onDismiss: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onQuickStock: ((Product) -> Unit)? = null,
    onPriceTag: ((Product) -> Unit)? = null,
    onDuplicateProduct: ((Product) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val remFormatted = if (product.remainder % 1.0 == 0.0) "${product.remainder.toInt()}" else String.format(Locale.US, "%.2f", product.remainder)
    val priceKop = Math.round(product.price * 100.0)
    val barcodeText = product.barcodes.firstOrNull() ?: "PLU #${product.id}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.printableName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${PosCurrencyFormatter.formatKopecks(priceKop)} ₴/${product.unitOfMeasurement}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Зал: $remFormatted",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = barcodeText,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PosProductActionRow("✏", "Редагувати картку") {
                    onDismiss()
                    onEditProduct(product)
                }
                if (onQuickStock != null) {
                    PosProductActionRow("📦", "Змінити залишок (Quick Stock)") {
                        onDismiss()
                        onQuickStock(product)
                    }
                }
                if (onPriceTag != null) {
                    PosProductActionRow("🖨", "Друк поличкового цінника") {
                        onDismiss()
                        onPriceTag(product)
                    }
                }
                if (onDuplicateProduct != null) {
                    PosProductActionRow("📋", "Дублювати товар") {
                        onDismiss()
                        onDuplicateProduct(product)
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Закрити", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 480.dp)
    )
}

@Composable
private fun PosProductActionRow(
    icon: String,
    title: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
