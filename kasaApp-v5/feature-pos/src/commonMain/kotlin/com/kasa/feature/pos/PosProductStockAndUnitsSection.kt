package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Секція налаштування одиниць виміру, залишку та вагових властивостей товару.
 * Оптимізована під мобільні екрани з числовими клавіатурами та пальцевими зонами >= 36 dp.
 */
@Composable
fun PosProductStockAndUnitsSection(
    isWeighted: Boolean,
    onIsWeightedChange: (Boolean) -> Unit,
    unitOfMeasurement: String,
    onUnitChange: (String) -> Unit,
    remainderStr: String,
    onRemainderChange: (String) -> Unit,
    minRemainderStr: String,
    onMinRemainderChange: (String) -> Unit,
    sectionStr: String,
    onSectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 380.dp

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Рядок 1: Перемикач Ваговий / Штучний товар
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isWeighted) "⚖ Ваговий товар (кг / г)" else "📦 Штучний товар ($unitOfMeasurement)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isWeighted,
                    onCheckedChange = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onIsWeightedChange(it)
                        onUnitChange(if (it) "кг" else "шт")
                    }
                )
            }

            // Рядок 2: Вибір одиниці вимірювання
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                val units = if (isWeighted) listOf("кг", "г") else listOf("шт", "л", "м", "пак")
                units.forEach { u ->
                    FilterChip(
                        selected = unitOfMeasurement == u,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onUnitChange(u)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = { Text(u, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false) }
                    )
                }
            }

            // Рядок 3+: Залишок, мінімальний залишок та номер відділу
            if (isNarrow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = remainderStr,
                        onValueChange = { onRemainderChange(it.replace(',', '.')) },
                        label = { Text("Залишок ($unitOfMeasurement)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minRemainderStr,
                        onValueChange = { onMinRemainderChange(it.replace(',', '.')) },
                        label = { Text("Мін. залишок", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = sectionStr,
                    onValueChange = { onSectionChange(it.filter { ch -> ch.isDigit() }) },
                    label = { Text("Відділ (номер секції)", maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = remainderStr,
                        onValueChange = { onRemainderChange(it.replace(',', '.')) },
                        label = { Text("Залишок ($unitOfMeasurement)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minRemainderStr,
                        onValueChange = { onMinRemainderChange(it.replace(',', '.')) },
                        label = { Text("Мін. залишок", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sectionStr,
                        onValueChange = { onSectionChange(it.filter { ch -> ch.isDigit() }) },
                        label = { Text("Відділ", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }
}
