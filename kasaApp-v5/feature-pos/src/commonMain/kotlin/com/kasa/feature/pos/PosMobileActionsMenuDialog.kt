package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Мобільне діалогове вікно швидких дій касира (Android та iOS):
 * - Швидкий виклик X-звіту та закриття зміни (Z-звіт);
 * - Доступ до журналу фіскальних чеків, складу, PLU та сервісу РРО;
 * - Тактильний вібровідгук (Haptic Feedback) на кожну дію;
 * - Ергономічні сенсорні плашки з висотою >= 44 dp для роботи однією рукою.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMobileActionsMenuDialog(
    onDismiss: () -> Unit,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Дії касира",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            PosMobileMenuRow("📊", "Денний X-звіт", "Перевірка виручки без закриття") {
                onDismiss()
                onEvent(PosEvent.PrintXReport)
            }
            PosMobileMenuRow("🔒", "Закриття зміни (Z-звіт)", "Фіскалізація та підсумок доби") {
                onDismiss()
                onEvent(PosEvent.PromptShiftReportDialog(true))
            }
            PosMobileMenuRow("📋", "Журнал чеків", "Історія операцій та повернення") {
                onDismiss()
                onEvent(PosEvent.ToggleJournalDialog)
            }
            PosMobileMenuRow("⏸", "Відкладені чеки", "Збережені незавершені замовлення") {
                onDismiss()
                onEvent(PosEvent.ToggleHeldReceiptsDialog)
            }
            PosMobileMenuRow("👥", "Клієнти та лояльність", "Пошук клієнта за телефоном чи карткою") {
                onDismiss()
                onEvent(PosEvent.PromptLoyaltyDialog(true))
            }
            PosMobileMenuRow("📦", "Склад та залишки", "Облік номенклатури та цін") {
                onDismiss()
                onEvent(PosEvent.ToggleCatalogManagementDialog(true))
            }
            PosMobileMenuRow("🔢", "Сенсорний нумпад", "Швидке введення коду / кількості") {
                onDismiss()
                onEvent(PosEvent.ToggleNumpadDialog(true))
            }
            PosMobileMenuRow("🏷", "Пам'ять артикулів (PLU)", "Синхронізація товарів з РРО") {
                onDismiss()
                onEvent(PosEvent.TogglePluDialog)
            }
            if (onOpenConnectionDialog != null) {
                PosMobileMenuRow("📡", "Підключення РРО", "Bluetooth / COM-порт зв'язок") {
                    onDismiss()
                    onOpenConnectionDialog()
                }
            }
            PosMobileMenuRow("⚙", "Сервіс та скринька", "Внесення, видача, діагностика РРО") {
                onDismiss()
                onEvent(PosEvent.ToggleServiceDialog)
            }
        }
    }
}

@Composable
private fun PosMobileMenuRow(
    icon: String,
    title: String,
    subtitle: String,
    isCompactHeight: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = if (isCompactHeight) 38.dp else 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isCompactHeight) 5.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, fontSize = if (isCompactHeight) 17.sp else 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = if (isCompactHeight) 12.sp else 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = if (isCompactHeight) 9.sp else 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
