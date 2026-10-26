package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.TaxGroup

/**
 * Кнопки підтвердження та керування життєвим циклом товару в модальному діалозі редагування:
 * - Збереження («Зберегти» / «Створити товар»)
 * - Пакетне введення («Зберегти та +Новий») для швидкого оприбуткування багатьох товарів
 * - Безпечне двоетапне видалення з підтвердженням від випадкового натискання
 * - Дублювання картки товару з авто-інкрементом коду PLU
 */

/**
 * Повнорозмірний блок дій для діалогу редагування товару (оптимізовано для смартфонів).
 * Розміщує головну кнопку "Створити товар" на всю ширину, що запобігає вертикальному сплющуванню тексту.
 */
@Composable
fun PosProductEditActionButtons(
    isNew: Boolean,
    hasConflict: Boolean,
    productExists: Boolean,
    onSave: () -> Unit,
    onSaveAndNew: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isConfirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Головна дія (Створити / Зберегти) на всю ширину екрана
        Button(
            enabled = !hasConflict,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text(if (isNew) "Створити товар" else "Зберегти зміни", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        // Рядок допоміжних дій з рівномірним розподілом ширини
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isNew && onSaveAndNew != null) {
                OutlinedButton(
                    enabled = !hasConflict,
                    onClick = onSaveAndNew,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("+ Новий", maxLines = 1, softWrap = false)
                }
            }
            if (productExists && onDuplicate != null) {
                OutlinedButton(
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("📋 Копія", maxLines = 1, softWrap = false)
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("Скасувати", maxLines = 1, softWrap = false)
            }
        }

        // Блок безпечного видалення товару
        if (productExists && onDelete != null) {
            if (isConfirmingDelete) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Підтвердити видалення товару", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else {
                TextButton(
                    onClick = { isConfirmingDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("🗑 Видалити товар з каталогу", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}



/**
 * Селектор податкової ставки ПДВ / Акцизу для мобільних екранів та планшетів без Skiko-залежностей.
 */
@Composable
fun PosTaxGroupSelector(
    taxGroup: com.kasa.model.TaxGroup,
    onTaxGroupSelected: (com.kasa.model.TaxGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { isDialogOpen = true },
        modifier = modifier.defaultMinSize(minHeight = 56.dp).height(56.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ПДВ: ${taxGroup.codeLetter} (${taxGroup.vatRatePercent}%) ▾",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Оберіть ставку ПДВ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isDialogOpen = false }, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.kasa.model.TaxGroup.entries.forEach { group ->
                        Surface(
                            onClick = {
                                onTaxGroupSelected(group)
                                isDialogOpen = false
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (taxGroup == group) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${group.codeLetter} (${group.vatRatePercent}%)",
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = group.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isDialogOpen = false },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Закрити", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            },
            modifier = Modifier.fillMaxWidth(0.96f).widthIn(max = 480.dp)
        )
    }
}
