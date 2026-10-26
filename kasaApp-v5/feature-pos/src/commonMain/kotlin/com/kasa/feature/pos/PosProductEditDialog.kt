package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import com.kasa.model.TaxGroup

/**
 * Діалог створення, редагування та пакетного додавання карток товарів:
 * - Фіскальні реквізити, PLU, націнка, собівартість, штрих-коди
 * - Режим швидкого пакетного додавання «Зберегти та +Новий»
 */
@Composable
fun PosProductEditDialog(
    product: Product? = null,
    prefilledBarcode: String? = null,
    suggestedPlu: Long = 1L,
    availableCategories: List<String> = emptyList(),
    existingProducts: List<Product> = emptyList(),
    onSave: (Product) -> Unit,
    onSaveAndNew: ((Product) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    onDuplicate: ((Product) -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = product == null
    var pluStr by remember { mutableStateOf(product?.id?.toString() ?: suggestedPlu.toString()) }
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var priceStr by remember { mutableStateOf(product?.let { String.format(java.util.Locale.US, "%.2f", it.price) }.orEmpty()) }
    var costPriceStr by remember { mutableStateOf(product?.let { String.format(java.util.Locale.US, "%.2f", it.costPrice) }.orEmpty()) }
    var barcodeStr by remember { mutableStateOf(product?.barcodes?.joinToString(", ") ?: prefilledBarcode.orEmpty()) }
    var categoryName by remember { mutableStateOf(product?.categoryName.orEmpty()) }
    var taxGroup by remember { mutableStateOf(product?.taxGroup ?: TaxGroup.GROUP_A) }
    var uktzed by remember { mutableStateOf(product?.uktzed.orEmpty()) }
    var isWeighted by remember { mutableStateOf(product?.isWeighted ?: false) }
    var unitOfMeasurement by remember { mutableStateOf(product?.unitOfMeasurement ?: if (isWeighted) "кг" else "шт") }
    var remainderStr by remember { mutableStateOf(product?.remainder?.toString() ?: "0.0") }
    var minRemainderStr by remember { mutableStateOf(product?.minRemainder?.toString() ?: "0.0") }
    var sectionStr by remember { mutableStateOf(product?.section?.toString() ?: "1") }
    var isFavorite by remember { mutableStateOf(product?.isFavorite ?: false) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    val currentPluNum = pluStr.toLongOrNull() ?: 0L
    val conflictingPluProduct = if (isNew) existingProducts.find { it.id == currentPluNum } else null

    LaunchedEffect(prefilledBarcode) {
        if (!prefilledBarcode.isNullOrBlank()) {
            val current = barcodeStr.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
            if (!current.contains(prefilledBarcode)) {
                barcodeStr = if (barcodeStr.isBlank()) prefilledBarcode else "$barcodeStr, $prefilledBarcode"
            }
        }
    }

    fun buildProductFromFields(): Product? {
        val parsedPrice = priceStr.toDoubleOrNull()
        nameError = name.isBlank()
        priceError = parsedPrice == null || parsedPrice <= 0.0
        if (nameError || priceError || parsedPrice == null) return null
        val parsedPlu = pluStr.toLongOrNull() ?: suggestedPlu
        val barcodesList = barcodeStr.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }

        return (product ?: Product(id = parsedPlu, name = name.trim(), price = parsedPrice)).copy(
            id = parsedPlu, name = name.trim(), price = parsedPrice,
            costPrice = costPriceStr.toDoubleOrNull() ?: 0.0,
            barcodes = barcodesList, categoryName = categoryName.trim(), taxGroup = taxGroup,
            uktzed = uktzed, isWeighted = isWeighted, unitOfMeasurement = unitOfMeasurement,
            remainder = remainderStr.toDoubleOrNull() ?: 0.0,
            minRemainder = minRemainderStr.toDoubleOrNull() ?: 0.0,
            isFavorite = isFavorite, section = sectionStr.toIntOrNull() ?: 1,
            description = description.trim()
        )
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
                    text = if (isNew) "➕ Новий товар" else "✏️ Редагування #$pluStr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PosProductCoreFields(
                    pluStr = pluStr,
                    onPluChange = { pluStr = it },
                    name = name,
                    onNameChange = {
                        name = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    isNameError = nameError,
                    conflictingProductName = conflictingPluProduct?.name,
                    priceStr = priceStr,
                    onPriceChange = {
                        priceStr = it.replace(',', '.')
                        if (priceError && it.toDoubleOrNull() != null) priceError = false
                    },
                    isPriceError = priceError,
                    costPriceStr = costPriceStr,
                    onCostPriceChange = { costPriceStr = it.replace(',', '.') }
                )

                PosProductMarginSummary(priceStr = priceStr, costPriceStr = costPriceStr)

                PosProductBarcodeSection(
                    barcodeStr = barcodeStr,
                    onBarcodeChange = { barcodeStr = it },
                    isWeighted = isWeighted,
                    plu = currentPluNum.coerceAtLeast(1L),
                    existingProducts = existingProducts,
                    currentProductId = product?.id,
                    onOpenScanner = onOpenScanner
                )

                if (availableCategories.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = categoryName == cat,
                                onClick = { categoryName = cat },
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                label = { Text(cat, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Категорія", maxLines = 1) },
                        placeholder = { Text("Хліб, Напої...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    PosTaxGroupSelector(
                        taxGroup = taxGroup,
                        onTaxGroupSelected = { taxGroup = it },
                        modifier = Modifier.weight(1.1f)
                    )
                }

                PosProductUktzedSection(uktzed = uktzed, onUktzedChange = { uktzed = it }, hasExcise = taxGroup.hasExcise)

                PosProductStockAndUnitsSection(
                    isWeighted = isWeighted,
                    onIsWeightedChange = { isWeighted = it },
                    unitOfMeasurement = unitOfMeasurement,
                    onUnitChange = { unitOfMeasurement = it },
                    remainderStr = remainderStr,
                    onRemainderChange = { remainderStr = it },
                    minRemainderStr = minRemainderStr,
                    onMinRemainderChange = { minRemainderStr = it },
                    sectionStr = sectionStr,
                    onSectionChange = { sectionStr = it }
                )

                PosProductDescriptionField(description = description, onDescriptionChange = { description = it })

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isFavorite,
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = { Text(if (isFavorite) "⭐ Швидкий товар (Обране)" else "☆ Додати в Обране", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        },
        confirmButton = {
            PosProductEditActionButtons(
                isNew = isNew,
                hasConflict = conflictingPluProduct != null,
                productExists = product != null,
                onSave = { buildProductFromFields()?.let(onSave) },
                onSaveAndNew = if (onSaveAndNew != null) {
                    {
                        buildProductFromFields()?.let { built ->
                            onSaveAndNew(built)
                            pluStr = (built.id + 1).toString()
                            name = ""; priceStr = ""; costPriceStr = ""; barcodeStr = ""; description = ""
                        }
                    }
                } else null,
                onDuplicate = if (product != null && onDuplicate != null) { { onDuplicate(product) } } else null,
                onDelete = if (product != null && onDelete != null) { { onDelete(product.id) } } else null,
                onDismiss = onDismiss
            )
        },
        dismissButton = null,
        modifier = modifier.fillMaxWidth(0.96f).widthIn(max = 580.dp).imePadding()
    )
}
