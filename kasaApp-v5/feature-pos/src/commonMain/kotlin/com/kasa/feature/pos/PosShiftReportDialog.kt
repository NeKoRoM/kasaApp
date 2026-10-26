package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kasa.model.CashierShift

/**
 * Діалог денного звіту касової зміни та виконання фіскального Z-звіту.
 * Відображає розрахункові суми, тривалість зміни, рух коштів у скриньці та захист від випадкового закриття.
 */
@Composable
fun PosShiftReportDialog(
    shift: CashierShift?,
    onPrintZReport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPrintXReport: (() -> Unit)? = null,
    onOpenShift: (() -> Unit)? = null,
    onPerformCashInOut: ((Long) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isConfirmingZReport by remember { mutableStateOf(false) }
    var activeCashInOutMode by remember { mutableStateOf<CashInOutMode?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (shift?.isClosed == true) "Z-Звіт зміни #${shift.shiftNumber}" else "Касова зміна #${shift?.shiftNumber ?: 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    shift?.let {
                        val (bg, fg, title) = when {
                            it.isClosed -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "ЗАКРИТА")
                            it.isExpired24h -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "> 24 ГОД!")
                            else -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "ВІДКРИТА")
                        }
                        Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = title,
                                color = fg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            if (shift == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Касова зміна наразі не відкрита.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (shift.isExpired24h && !shift.isClosed) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ УВАГА: Зміна триває понад 24 години! Згідно із Законом України про РРО, фіскальні продажі заблоковані до виконання Z-звіту.",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Блок інформації про зміну
                    ShiftInfoRow("Касир:", shift.cashierName)
                    ShiftInfoRow("Зміна №:", "${shift.shiftNumber}")
                    ShiftInfoRow("Відкрито:", formatShiftTimestamp(shift.openedAt))
                    shift.closedAt?.let { closedTime ->
                        ShiftInfoRow("Закрито:", formatShiftTimestamp(closedTime))
                    }
                    ShiftInfoRow("Тривалість:", shift.formattedDuration)
                    shift.zReportNumber?.let { zNum ->
                        ShiftInfoRow("Номер Z-звіту:", zNum)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Фінансові показники
                    ShiftMoneyRow("Початкова сума в скриньці:", shift.initialDrawerBalanceKopecks)
                    ShiftMoneyRow("Продажі (готівка):", shift.totalSalesCashKopecks)
                    ShiftMoneyRow("Продажі (картка):", shift.totalSalesCardKopecks)
                    ShiftMoneyRow("Загальний виторг:", shift.totalSalesKopecks, isBold = true)
                    ShiftMoneyRow("Повернення коштів:", shift.totalReturnsKopecks, isNegative = true)
                    ShiftMoneyRow("Службові внесення (+):", shift.totalCashInKopecks)
                    ShiftMoneyRow("Службові видачі (-):", shift.totalCashOutKopecks, isNegative = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ShiftMoneyRow("Очікуваний залишок у скриньці:", shift.expectedDrawerCashKopecks, isBold = true, isHighlighted = true)

                    if (!shift.isClosed && onPerformCashInOut != null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeCashInOutMode = CashInOutMode.CASH_IN
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("💵 +Внесення", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeCashInOutMode = CashInOutMode.CASH_OUT
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = shift.expectedDrawerCashKopecks > 0L
                            ) { Text("📤 −Видача", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                        }
                    }

                    ShiftInfoRow("Кількість чеків:", "${shift.receiptsCount}")

                    if (!shift.isClosed && onPrintXReport != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrintXReport()
                            },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🖨 Надрукувати X-звіт (денний)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (shift != null && !shift.isClosed) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isConfirmingZReport = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Z-звіт", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else if (shift == null && onOpenShift != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenShift()
                        onDismiss()
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Відкрити зміну", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Зрозуміло", maxLines = 1, softWrap = false)
                }
            }
        },
        dismissButton = if ((shift != null && !shift.isClosed) || (shift == null && onOpenShift != null)) {
            {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) { Text("Скасувати", maxLines = 1, softWrap = false) }
            }
        } else null,
        modifier = modifier.imePadding().fillMaxWidth(0.96f).widthIn(max = 560.dp)
    )

    if (isConfirmingZReport) {
        PosZReportConfirmDialog(
            expectedCashKopecks = shift?.expectedDrawerCashKopecks,
            onConfirm = {
                isConfirmingZReport = false
                onPrintZReport()
            },
            onDismiss = { isConfirmingZReport = false }
        )
    }

    activeCashInOutMode?.let { mode ->
        PosCashInOutDialog(
            mode = mode,
            currentBalanceKopecks = shift?.expectedDrawerCashKopecks ?: 0L,
            onConfirm = { kopecks ->
                onPerformCashInOut?.invoke(kopecks)
                activeCashInOutMode = null
            },
            onDismiss = { activeCashInOutMode = null }
        )
    }
}
