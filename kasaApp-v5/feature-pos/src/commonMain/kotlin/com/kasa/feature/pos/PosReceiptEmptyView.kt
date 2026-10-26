package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.ReceiptType

/**
 * Ергономічна мобільна заглушка для порожнього чека:
 * - Адаптується під портретний та альбомний режими (BoxWithConstraints);
 * - Зручні сенсорні кнопки (Thumb Zone) для швидкого старту продажу;
 * - Прямий перехід до каталогу, камери сканера або введення PLU;
 * - Підтримка спрямованого сценарію повернення (швидкий перехід у журнал чеків);
 * - Швидке відновлення відкладених чеків одним дотиком.
 */
@Composable
fun PosReceiptEmptyView(
    heldReceiptsCount: Int = 0,
    receiptType: ReceiptType = ReceiptType.SALE,
    onOpenScanner: () -> Unit,
    onOpenNumpad: () -> Unit,
    onOpenHeldReceipts: (() -> Unit)? = null,
    onOpenCatalog: (() -> Unit)? = null,
    onOpenJournal: (() -> Unit)? = null,
    onCancelReturn: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isSale = receiptType == ReceiptType.SALE

    BoxWithConstraints(modifier = modifier) {
        val isCompactHeight = maxHeight < 450.dp
        val iconSize = if (isCompactHeight) 48.dp else 72.dp
        val iconFontSize = if (isCompactHeight) 24.sp else 34.sp
        val titleFontSize = if (isCompactHeight) 15.sp else 17.sp
        val topSpacing = if (isCompactHeight) 6.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (isCompactHeight) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconBg = if (isSale) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSale) "🛒" else "↩️", fontSize = iconFontSize)
            }

            Spacer(modifier = Modifier.height(topSpacing))

            Text(
                text = if (isSale) "Чек порожній" else "Повернення порожнє",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = if (isSale) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            if (!isCompactHeight) {
                Spacer(modifier = Modifier.height(4.dp))
                val subText = if (isSale) {
                    "Додайте товар із каталогу або відскануйте штрих-код"
                } else {
                    "Виберіть фіскальний чек із журналу або відскануйте товар для повернення"
                }
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

            if (!isSale && onOpenJournal != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenJournal()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("📋 Вибрати з журналу чеків", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 8.dp))
            } else if (onOpenCatalog != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenCatalog()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("📦 Вибрати з каталогу", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenScanner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📷 Сканер", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenNumpad()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔢 Нумпад", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }

            if (heldReceiptsCount > 0 && onOpenHeldReceipts != null) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 12.dp))
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenHeldReceipts()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.92f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("📂 Відновити відкладений ($heldReceiptsCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }

            if (!isSale && onCancelReturn != null) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 10.dp))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCancelReturn()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(0.92f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("✕ Скасувати режим повернення", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
