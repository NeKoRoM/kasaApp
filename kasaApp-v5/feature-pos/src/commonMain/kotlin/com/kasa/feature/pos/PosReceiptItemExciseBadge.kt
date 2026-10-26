package com.kasa.feature.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * Сенсорний бейдж обов'язкової акцизної марки для алкоголю та тютюну в рядку чека:
 * - Попереджає контрастним кольором, якщо марка ще не відсканована/введена;
 * - Відображає зелений бейдж зі штрихкодом марки, якщо вже введено;
 * - Дозволяє відкрити сканер камери або діалог введення в 1 дотик;
 * - Оптимізований для роботи пальцем на смартфонах із висотою touch-target >= 38 dp.
 */
@Composable
fun PosReceiptItemExciseBadge(
    exciseStamp: String?,
    onPromptExcise: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenScanner: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val stampMissing = exciseStamp.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (stampMissing) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
        },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 38.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPromptExcise()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = if (stampMissing) "⚠️" else "✓",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (stampMissing) {
                        "Потрібна акцизна марка"
                    } else {
                        "Акциз: $exciseStamp"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stampMissing) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (stampMissing && onOpenScanner != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenScanner()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("📷", fontSize = 14.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (stampMissing) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = if (stampMissing) "Ввести ✎" else "Змінити ✎",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stampMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
