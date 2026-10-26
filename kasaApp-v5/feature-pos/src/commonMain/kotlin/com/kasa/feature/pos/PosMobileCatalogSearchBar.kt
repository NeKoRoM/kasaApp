package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Компактний мобільний рядок пошуку товарів зі вбудованими швидкими діями касира (Android та iOS):
 * - Інтегровані в 1 рядок: пошук за текстом/кодом, запуск камери-сканера, нумпад та кнопка додавання товару;
 * - Звільняє до 80-120 dp вертикального простору екрана смартфона.
 */
@Composable
fun PosMobileCatalogSearchBar(
    searchQuery: String,
    onUpdateSearchQuery: (String) -> Unit,
    onScanBarcode: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenNumpad: () -> Unit,
    onAddProduct: () -> Unit,
    focusManager: FocusManager,
    isCompactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val barHeight = if (isCompactHeight) 40.dp else 46.dp
    val iconBtnSize = if (isCompactHeight) 40.dp else 46.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onUpdateSearchQuery,
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = barHeight),
            placeholder = {
                Text(
                    text = "Пошук товару, коду...",
                    fontSize = if (isCompactHeight) 11.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    val q = searchQuery.trim()
                    if (q.isNotBlank()) {
                        val isBarcodeOrCommand = q.all { it.isDigit() } || q.contains('*') || q.contains(' ') || q.contains("t.gov.ua")
                        if (isBarcodeOrCommand) {
                            onScanBarcode(q)
                            onUpdateSearchQuery("")
                        }
                    }
                }
            ),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateSearchQuery("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                focusManager.clearFocus()
                                onScanBarcode(searchQuery)
                                onUpdateSearchQuery("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("↵", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenScanner()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("📷", fontSize = 15.sp)
                    }
                }
            },
            shape = RoundedCornerShape(8.dp)
        )

        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenNumpad()
            },
            modifier = Modifier.size(iconBtnSize),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("🔢", fontSize = if (isCompactHeight) 14.sp else 16.sp)
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAddProduct()
            },
            modifier = Modifier.size(iconBtnSize)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", fontSize = if (isCompactHeight) 18.sp else 21.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
