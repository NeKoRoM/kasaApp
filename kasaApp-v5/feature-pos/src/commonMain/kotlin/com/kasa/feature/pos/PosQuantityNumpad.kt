package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Сенсорна цифрова клавіатура (нумпад) для швидкого введення кількості та ваги на смартфонах:
 * - Робота однією рукою у Thumb Zone (висота кнопок >= 44 dp);
 * - Візуальне розмежування цифрових клавіш та дій очищення (C) / видалення (⌫);
 * - Тактильний вібровідгук (Haptic Feedback) на кожне натискання.
 */
@Composable
fun PosQuantityNumpad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDot: () -> Unit,
    onBackspace: (() -> Unit)? = null,
    keyHeight: Dp = 44.dp,
    isCompactHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val rows = if (onBackspace != null) {
        listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("C", "0", ".", "⌫")
        )
    } else {
        listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("C", "0", ".")
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 2.dp else 4.dp)
    ) {
        for (rowKeys in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (key in rowKeys) {
                    val isAction = key == "C" || key == "⌫"
                    val buttonColor = if (isAction) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = if (key == "C") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (key) {
                                "C" -> onClear()
                                "⌫" -> onBackspace?.invoke()
                                "." -> onDot()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight),
                        shape = RoundedCornerShape(6.dp),
                        colors = buttonColor,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = key,
                            fontSize = if (isCompactHeight) 14.sp else 16.sp,
                            fontWeight = if (isAction) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
