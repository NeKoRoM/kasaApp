package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Базові поля картки товару:
 * - PLU код з перевіркою конфліктів у каталозі;
 * - Назва товару з індикацією обов'язковості;
 * - Роздрібна ціна реалізації та собівартість.
 */
@Composable
fun PosProductCoreFields(
    pluStr: String,
    onPluChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    isNameError: Boolean,
    conflictingProductName: String?,
    priceStr: String,
    onPriceChange: (String) -> Unit,
    isPriceError: Boolean,
    costPriceStr: String,
    onCostPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = pluStr,
                onValueChange = { onPluChange(it.filter { ch -> ch.isDigit() }.take(5)) },
                label = { Text("PLU *", maxLines = 1) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = conflictingProductName != null,
                modifier = Modifier.width(76.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Назва товару *", maxLines = 1) },
                singleLine = true,
                isError = isNameError,
                modifier = Modifier.weight(1f)
            )
        }

        if (conflictingProductName != null) {
            Text(
                text = "⚠️ PLU #$pluStr вже використовується: '$conflictingProductName'",
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = priceStr,
                onValueChange = onPriceChange,
                label = { Text("Ціна ₴ *", maxLines = 1) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = isPriceError,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = costPriceStr,
                onValueChange = onCostPriceChange,
                label = { Text("Собів. ₴", maxLines = 1) },
                placeholder = { Text("0.00 ₴") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
