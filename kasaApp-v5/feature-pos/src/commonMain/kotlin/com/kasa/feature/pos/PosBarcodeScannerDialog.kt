package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.BarcodeParser
import com.kasa.model.Product

/**
 * Інтерактивний діалог мобільного сканера штрих-кодів для смартфонів (Android та iOS):
 * - Пакетний режим (Continuous Multi-scan) для швидкого сканування декількох товарів поспіль;
 * - Керування спалахом/ліхтариком камери (Flashlight/Torch toggle);
 * - Звуковий біп роздрібного сканера та тактильний вібровідгук (Haptic Feedback);
 * - Підтримка префіксів ваг, карток лояльності, е-акцизу та швидких шорткатів '3*101'.
 */
@Composable
fun PosBarcodeScannerDialog(
    catalogProducts: List<Product> = emptyList(),
    onScan: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var barcodeInput by remember { mutableStateOf("") }
    var isBatchMode by remember { mutableStateOf(false) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isSoundOn by remember { mutableStateOf(PosAudioFeedback.isSoundEnabled) }
    var batchScannedCount by remember { mutableStateOf(0) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val matchedProduct = remember(lastScannedCode, catalogProducts) {
        val code = lastScannedCode?.trim().orEmpty()
        if (code.isBlank()) null else catalogProducts.firstOrNull { it.barcodes.contains(code) || it.id.toString() == code }
    }

    val clean = barcodeInput.trim()
    val isEan13 = clean.length == 13 && clean.all { it.isDigit() }
    val isValidEan13 = isEan13 && BarcodeParser.isValidEan13Checksum(clean)
    val isWeighted = clean.length == 13 && clean.take(2).toIntOrNull() in 20..24
    val isExcise = clean.contains("t.gov.ua/") || (clean.length in 10..16 && clean.all { it.isLetterOrDigit() })

    fun performScan(code: String) {
        if (code.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        PosAudioFeedback.playScanBeep()
        onScan(code)
        if (isBatchMode) {
            batchScannedCount++
            lastScannedCode = code
            barcodeInput = ""
        } else {
            onDismiss()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isCompactHeight = maxHeight < 500.dp
        val cameraHeight = if (isCompactHeight) 130.dp else 180.dp

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "📷 Сканер кодів",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (batchScannedCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("+$batchScannedCount") }
                        }
                    }
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
                    PosBarcodeScannerControls(
                        isBatchMode = isBatchMode,
                        onToggleBatchMode = { isBatchMode = !isBatchMode },
                        isFlashlightOn = isFlashlightOn,
                        onToggleFlashlight = { isFlashlightOn = !isFlashlightOn },
                        isSoundOn = isSoundOn,
                        onToggleSound = {
                            isSoundOn = !isSoundOn
                            PosAudioFeedback.isSoundEnabled = isSoundOn
                        }
                    )

                    // Вбудований видошукач камери
                    PosCameraScannerView(
                        modifier = Modifier.fillMaxWidth().height(cameraHeight),
                        isFlashlightOn = isFlashlightOn,
                        isScanningEnabled = true,
                        onBarcodeDetected = { scannedCode -> performScan(scannedCode) }
                    )

                    PosBarcodeLastScannedBanner(
                        batchScannedCount = batchScannedCount,
                        matchedProduct = matchedProduct,
                        lastScannedCode = lastScannedCode
                    )

                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("Штрих-код або команда '3*101'") },
                    placeholder = { Text("Наприклад 4820000001011...") },
                    singleLine = true,
                    supportingText = {
                        when {
                            clean.isBlank() -> Text("Введіть код або виберіть зі списку нижче")
                            isExcise -> Text("✓ Електронна акцизна марка", color = MaterialTheme.colorScheme.secondary)
                            isWeighted && isValidEan13 -> Text("✓ Ваговий штрих-код (${clean.take(2)})", color = MaterialTheme.colorScheme.tertiary)
                            isValidEan13 -> Text("✓ Валідний EAN-13", color = MaterialTheme.colorScheme.tertiary)
                            isEan13 -> Text("⚠ Помилка контрольного розряду EAN-13", color = MaterialTheme.colorScheme.error)
                            clean.contains('*') || clean.contains(' ') -> Text("⚡ Шорткат кількості", color = MaterialTheme.colorScheme.primary)
                            else -> Text("Штрих-код або картка")
                        }
                    },
                    trailingIcon = {
                        if (barcodeInput.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(onClick = { barcodeInput = "" }, modifier = Modifier.size(32.dp)) {
                                    Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { performScan(clean) }, modifier = Modifier.size(32.dp)) {
                                    Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { performScan(clean) }),
                    modifier = Modifier.fillMaxWidth()
                )

                PosBarcodePresetsList(
                    catalogProducts = catalogProducts,
                    barcodeInput = barcodeInput,
                    onSelectCode = { code ->
                        if (isBatchMode) performScan(code) else barcodeInput = code
                    }
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val label = if (batchScannedCount > 0) "Готово ($batchScannedCount)" else "Скасувати"
                    Text(label, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { performScan(clean) },
                    enabled = clean.isNotBlank(),
                    modifier = Modifier.weight(1.2f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val label = if (isBatchMode) "Додати (+)" else "Зісканувати ↵"
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        dismissButton = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 500.dp)
        )
    }
}
