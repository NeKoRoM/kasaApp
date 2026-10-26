package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.BarcodeGenerator
import com.kasa.model.BarcodeParser
import com.kasa.model.Product

/**
 * Допоміжні UI-компоненти редагування товару:
 * - Розрахунок маржі та націнки в реальному часі
 * - Поле штрих-коду з валідацією контрольної суми EAN-13 та детекцією колізій
 * - Швидкі підказки кодів УКТЗЕД для підакцизних товарів
 */

@Composable
fun PosProductMarginSummary(
    priceStr: String,
    costPriceStr: String,
    modifier: Modifier = Modifier
) {
    val price = priceStr.toDoubleOrNull() ?: 0.0
    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
    if (price > 0.0 && cost > 0.0) {
        val profit = price - cost
        val markupPercent = (profit / cost) * 100.0
        val isNegative = profit < 0.0
        Surface(
            color = if (isNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNegative) "⚠️ Збиток: ${String.format("%.2f", profit)} грн"
                    else "✓ Прибуток: ${String.format("%.2f", profit)} грн/од",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Націнка: ${String.format("%.1f", markupPercent)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun PosProductBarcodeSection(
    barcodeStr: String,
    onBarcodeChange: (String) -> Unit,
    isWeighted: Boolean,
    plu: Long,
    existingProducts: List<Product>,
    currentProductId: Long?,
    modifier: Modifier = Modifier,
    onOpenScanner: (() -> Unit)? = null
) {
    val parsedBarcodes = barcodeStr.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
    val collidingProduct = parsedBarcodes.firstNotNullOfOrNull { code ->
        existingProducts.find { it.id != currentProductId && it.barcodes.contains(code) }
    }

    val primaryBarcode = parsedBarcodes.firstOrNull() ?: ""
    val is13Digits = primaryBarcode.length == 13 && primaryBarcode.all { it.isDigit() }
    val isInvalidChecksum = is13Digits && !BarcodeParser.isValidEan13Checksum(primaryBarcode)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = barcodeStr,
            onValueChange = onBarcodeChange,
            label = { Text("Штрих-код(і)", maxLines = 1) },
            placeholder = { Text("через кому...") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenScanner != null) {
                        IconButton(onClick = onOpenScanner, modifier = Modifier.size(40.dp)) {
                            Text("📷", fontSize = 15.sp)
                        }
                    }
                    if (barcodeStr.isNotEmpty()) {
                        IconButton(onClick = { onBarcodeChange("") }, modifier = Modifier.size(40.dp)) {
                            Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
            isError = isInvalidChecksum || collidingProduct != null,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenScanner != null) {
                OutlinedButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("📷 Сканер", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (isInvalidChecksum) {
                OutlinedButton(
                    onClick = {
                        val correct = BarcodeParser.calculateEan13CheckDigit(primaryBarcode.take(12))
                        val fixed = "${primaryBarcode.take(12)}$correct"
                        val remaining = parsedBarcodes.drop(1)
                        onBarcodeChange((listOf(fixed) + remaining).joinToString(", "))
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("⚡ КС (Виправити)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            }
            OutlinedButton(
                onClick = {
                    val gen = BarcodeGenerator.generateRetailEan13(plu)
                    onBarcodeChange(if (barcodeStr.isBlank()) gen else "$barcodeStr, $gen")
                },
                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("⚡ 482 (Укр)", fontSize = 11.sp) }
            OutlinedButton(
                onClick = {
                    val generated = if (isWeighted) BarcodeGenerator.generateWeightedEan13(21, plu, 0)
                    else BarcodeGenerator.generateInternalPieceEan13(plu)
                    onBarcodeChange(if (barcodeStr.isBlank()) generated else "$barcodeStr, $generated")
                },
                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("⚡ Внутр. EAN", fontSize = 11.sp, maxLines = 1, softWrap = false) }
        }

        if (collidingProduct != null) {
            Text("⚠️ Штрих-код вже належить: #${collidingProduct.id} '${collidingProduct.name}'", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        } else if (isInvalidChecksum) {
            Text("⚠️ Невірна контрольна сума EAN-13. Натисніть '⚡ КС (Виправити)'", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
        }
    }
}

@Composable
fun PosProductUktzedSection(
    uktzed: String,
    onUktzedChange: (String) -> Unit,
    hasExcise: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = uktzed,
            onValueChange = { onUktzedChange(it.trim()) },
            label = { Text(if (hasExcise) "Код УКТЗЕД (акциз) *" else "Код УКТЗЕД", maxLines = 1) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        if (hasExcise) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val uktzedPresets = listOf("2204" to "🍷 Вино", "2208" to "🥃 Алкоголь", "2203" to "🍺 Пиво", "2402" to "🚬 Тютюн")
                uktzedPresets.forEach { (code, label) ->
                    FilterChip(
                        selected = uktzed.startsWith(code),
                        onClick = { onUktzedChange(code) },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = { Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }
    }
}

@Composable
fun PosProductDescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Опис / примітки") },
        placeholder = { Text("Склад, фасування, постачальник...") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

