package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product

/**
 * Список швидких пресетів та зразків штрих-кодів для інтерактивного сканера.
 * Оптимізовано під сенсорні телефони з пальцевими зонами >= 36-40 dp.
 */
@Composable
fun PosBarcodePresetsList(
    catalogProducts: List<Product>,
    barcodeInput: String,
    onSelectCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val presets = listOf(
        "4820000001011" to "🍫 Штучний товар (Батончик)",
        "2100102012503" to "⚖ Ваговий з ваг (1.250 кг)",
        "https://t.gov.ua/AA12345678" to "🍾 Е-акцизна марка (Дія)",
        "4829999999999" to "❓ Невідомий товар (Створення)",
        "LOYALTY-001" to "👤 Картка лояльності (-5%)",
        "3*101" to "⚡ Множник зірочкою (3*101)",
        "5 101" to "⚡ Множник пробілом (5 101)"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (catalogProducts.isNotEmpty()) {
            Text("Товари з поточного каталогу:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items(catalogProducts.take(8)) { prod ->
                    val code = prod.barcodes.firstOrNull() ?: prod.id.toString()
                    FilterChip(
                        selected = barcodeInput == code,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectCode(code)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                        label = { Text(prod.printableName.take(16), fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }

        Text("Швидкі шаблони для тестування:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.forEach { (code, label) ->
                val shortCode = if (code.startsWith("https://")) code.removePrefix("https://") else code
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCode(code)
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f, fill = false),
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = shortCode,
                            fontSize = 10.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
