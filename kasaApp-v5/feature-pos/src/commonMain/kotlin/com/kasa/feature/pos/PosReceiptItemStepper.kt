package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * Сенсорний степер кількості позиції в чеку ([-], Qty, [+]) для швидкого коригування без модальних вікон.
 * Оптимізовано під сенсорні телефони Android та iOS із тактильним відгуком та зручними зонами натискання.
 */
@Composable
fun PosReceiptItemStepper(
    item: ReceiptItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onPromptQuantity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.height(40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Кнопка швидкого зменшення кількості [-] / [🗑]
            val isDeleteStep = if (item.isWeighted) item.quantity <= 0.15 else item.quantity <= 1.0
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .clickable {
                        if (item.isWeighted) {
                            val next = Math.round((item.quantity - 0.1) * 1000.0) / 1000.0
                            if (next > 0.05) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuantityChange(next)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemove()
                            }
                        } else {
                            val next = item.quantity - 1.0
                            if (next > 0.0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuantityChange(next)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemove()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeleteStep) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Відображення поточної кількості (натискання відкриває числовий нумпад)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPromptQuantity()
                    },
                contentAlignment = Alignment.Center
            ) {
                val qtyText = if (item.quantity % 1.0 == 0.0) {
                    "${item.quantity.toInt()} ${item.unitOfMeasurement}"
                } else {
                    "${String.format(java.util.Locale.US, "%.3f", item.quantity).trimEnd('0').trimEnd('.')} ${item.unitOfMeasurement}"
                }
                Text(
                    text = qtyText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Кнопка швидкого збільшення кількості [+]
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (item.isWeighted) {
                            val next = Math.round((item.quantity + 0.1) * 1000.0) / 1000.0
                            onQuantityChange(next)
                        } else {
                            onQuantityChange(item.quantity + 1.0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
