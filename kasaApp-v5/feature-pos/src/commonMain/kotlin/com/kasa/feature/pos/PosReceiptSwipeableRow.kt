package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.ReceiptItem

/**
 * Мобільний рядок товару зі змахуванням (Swipe-To-Dismiss) для швидкого видалення.
 * Реалізує нативну поведінку жестів iOS / Android при роботі одним великим пальцем:
 * - Свайп вліво (EndToStart) плавно відкриває червоний фон з іконкою видалення;
 * - Досягнення порогу змахування активує тактильний вібровідгук (Haptic Feedback);
 * - Зберігає традиційну кнопку [✕] для зручності та доступності.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosReceiptSwipeableRow(
    item: ReceiptItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onPromptExcise: () -> Unit,
    onPromptDiscount: () -> Unit,
    onPromptQuantity: () -> Unit = {},
    onPromptPrice: () -> Unit = {},
    onEditProduct: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRemove()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwipingToDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor = if (isSwipingToDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer
            val contentColor = if (isSwipingToDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Видалити",
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "🗑",
                        fontSize = 16.sp,
                        color = contentColor
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PosReceiptItemRow(
                item = item,
                onQuantityChange = onQuantityChange,
                onRemove = onRemove,
                onPromptExcise = onPromptExcise,
                onPromptDiscount = onPromptDiscount,
                onPromptQuantity = onPromptQuantity,
                onPromptPrice = onPromptPrice,
                onEditProduct = onEditProduct,
                onOpenScanner = onOpenScanner
            )
        }
    }
}
