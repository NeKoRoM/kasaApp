package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Сенсорний калькулятор решти та швидкі купюри готівки для смартфонів (Android та iOS):
 * - Швидкий вибір номіналу (Рівно, 50, 100, 200, 500, 1000 грн)
 * - Автоматичний розрахунок здачі в реальному часі
 * - Тактильний вібровідгук на натискання купюр
 */
@Composable
fun PosCashChangeCalculatorView(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 440.dp
        val isVeryNarrow = maxWidth < 330.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(if (isNarrow) 4.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Швидкі купюри та кнопка "Рівно" (плавний горизонтальний скрол без спотворення тексту)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isExact = state.receivedCashKopecks == state.totalPayableKopecks
                FilterChip(
                    selected = isExact,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(PosEvent.SetReceivedCash(state.totalPayableKopecks))
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    label = {
                        Text(
                            text = "Рівно",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )

                val bills = remember(state.totalPayableKopecks) {
                    calculateSuggestedBills(state.totalPayableKopecks)
                }
                for (billUah in bills) {
                    val billKop = billUah * 100L
                    val isSelected = state.receivedCashKopecks == billKop
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(PosEvent.SetReceivedCash(billKop))
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        label = {
                            Text(
                                text = "$billUah ₴",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            // Рядок відображення отриманих грошей та суми решти
            state.receivedCashKopecks?.let { received ->
                val receivedRow = @Composable {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Отримано: ${PosCurrencyFormatter.formatKopecks(received)} грн",
                            fontSize = if (isNarrow) 10.sp else 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isNarrow) 32.dp else 40.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(PosEvent.SetReceivedCash(null))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val changeBadge = @Composable {
                    when {
                        state.changeKopecks > 0L -> {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Решта: ${PosCurrencyFormatter.formatKopecks(state.changeKopecks)} грн",
                                    fontSize = if (isNarrow) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        received < state.totalPayableKopecks -> {
                            val diff = state.totalPayableKopecks - received
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Не вистачає: ${PosCurrencyFormatter.formatKopecks(diff)} грн",
                                    fontSize = if (isNarrow) 10.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "✓ Без решти",
                                fontSize = if (isNarrow) 10.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (isVeryNarrow) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        receivedRow()
                        changeBadge()
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        receivedRow()
                        changeBadge()
                    }
                }
            }
        }
    }
}

/**
 * Розраховує розумні рекомендовані купюри готівки:
 * Спочатку найближчі округлені суми >= суми чека (найбільш ймовірні купюри покупця),
 * потім інші стандартні номінали готівки НБУ.
 */
internal fun calculateSuggestedBills(totalKopecks: Long): List<Long> {
    val totalUah = if (totalKopecks > 0L) (totalKopecks + 99L) / 100L else 0L
    val standardBills = listOf(20L, 50L, 100L, 200L, 500L, 1000L)
    if (totalUah <= 0L) return standardBills

    val set = linkedSetOf<Long>()
    for (step in listOf(50L, 100L, 200L, 500L)) {
        val nextMultiple = ((totalUah + step - 1) / step) * step
        if (nextMultiple >= totalUah && nextMultiple <= 2000L) {
            set.add(nextMultiple)
        }
    }
    for (bill in standardBills) {
        set.add(bill)
    }
    val geTotal = set.filter { it >= totalUah }.sorted()
    return geTotal.take(5)
}
