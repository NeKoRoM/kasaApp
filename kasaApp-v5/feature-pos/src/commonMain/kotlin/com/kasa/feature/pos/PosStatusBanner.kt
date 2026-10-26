package com.kasa.feature.pos

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

/**
 * Нативний спливаючий банер статусів та сповіщень касира (Android / iPhone Toast & Snackbar).
 * Забезпечує миттєвий візуальний та тактильний зворотний зв'язок на мобільних пристроях:
 * - Успішний друк чека, відновлення/відкладання кошика, прикріплення клієнта лояльності;
 * - Попередження про дублікати акцизних марок чи невідомий штрих-код;
 * - Автоматичне плавне приховування через 3.5 секунди з можливістю закриття в 1 дотик.
 */
@Composable
fun PosStatusBanner(
    statusMessage: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(3500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = statusMessage != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (statusMessage != null) {
            val isWarningOrError = statusMessage.contains("Помилка", ignoreCase = true) ||
                statusMessage.contains("УВАГА", ignoreCase = true) ||
                statusMessage.contains("не знайдено", ignoreCase = true)
            val isSuccess = statusMessage.contains("успішно", ignoreCase = true) ||
                statusMessage.contains("Відновлено", ignoreCase = true) ||
                statusMessage.contains("Додано", ignoreCase = true) ||
                statusMessage.contains("Збережено", ignoreCase = true)

            val bgColor = when {
                isWarningOrError -> MaterialTheme.colorScheme.errorContainer
                isSuccess -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val contentColor = when {
                isWarningOrError -> MaterialTheme.colorScheme.onErrorContainer
                isSuccess -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            val icon = when {
                isWarningOrError -> "⚠️"
                isSuccess -> "✓"
                else -> "ℹ️"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 500.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(icon, fontSize = 16.sp)
                        Text(
                            text = statusMessage,
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
