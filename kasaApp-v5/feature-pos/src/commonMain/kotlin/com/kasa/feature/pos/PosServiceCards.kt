package com.kasa.feature.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ServiceSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun ServiceHardwareCard(hardwareInfo: String?, onQueryInfo: () -> Unit) {
    ServiceSectionCard(title = "Апаратні дані РРО") {
        Text(text = hardwareInfo ?: "Дані відсутні (натисніть 'Опитати')", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onQueryInfo, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)) {
            Text("Опитати номер та модель", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ServiceRtcCard(rtcTime: String?, onReadTime: () -> Unit, onSyncTime: () -> Unit) {
    ServiceSectionCard(title = "Годинник реального часу (RTC)") {
        Text(text = "Час в РРО: ${rtcTime ?: "Не зчитано"}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 440.dp
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReadTime,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Зчитати", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onSyncTime,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(if (isNarrow) "Синхрон." else "Синхронізувати", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ServiceDisplayCard(
    line1: String,
    line2: String,
    onSendGreeting: () -> Unit,
    onClearDisplay: () -> Unit,
    onSendCustomText: (String, String) -> Unit
) {
    ServiceSectionCard(title = "Дисплей покупця (VFD 2x20)") {
        Text("Рядок 1: ${line1.ifBlank { "(пусто)" }}\nРядок 2: ${line2.ifBlank { "(пусто)" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSendGreeting, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                Text("Вітання", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onClearDisplay, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                Text("Очистити", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ServiceReportsCard(
    onPrintX: () -> Unit,
    onPrintZ: () -> Unit,
    isReceiptOpen: Boolean = false,
    onVoidReceipt: () -> Unit = {}
) {
    ServiceSectionCard(title = "Фіскальні денні звіти та чек") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrintX,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Друк X-звіту", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onPrintZ,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Друк Z-звіту", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onVoidReceipt,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(if (isReceiptOpen) "⚠️ Анулювати відкритий чек (Void)" else "Анулювати чек в РРО (Void)", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}
