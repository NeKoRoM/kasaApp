package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.FiscalMath
import com.kasa.model.PaymentType
import com.kasa.model.ReceiptType
import kotlin.math.round

/**
 * Сенсорна панель вкладок паралельних чеків (Мульти-чек / Чек 1, Чек 2, Чек 3).
 * Оптимізована під дисплеї смартфонів (Android та iPhone):
 * - Вертикальне компонування назви, бейджа кількості та суми запобігає перекриттю тексту;
 * - Тактильний вібровідгук (Haptic Feedback) при виборі покупця;
 * - Чітка індикація активного, наповненого та порожнього чеків.
 */
@Composable
fun PosCartTabsRow(
    activeCartIndex: Int,
    carts: List<PosCart>,
    onSelectCart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 340.dp
        val isCompactHeight = maxHeight < 500.dp
        val emptyText = if (isNarrow) "—" else "Порожній"
        val tabMinHeight = if (isCompactHeight) 36.dp else 44.dp
        val tabPadVert = if (isCompactHeight) 3.dp else 5.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(if (isNarrow) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 2.dp else 4.dp)
        ) {
            carts.forEachIndexed { index, cart ->
                val isActive = index == activeCartIndex
                val hasItems = cart.items.isNotEmpty()
                val rawTotal = cart.items.sumOf { it.totalKopecks }
                val discountKopecks = if (cart.receiptDiscountPercent > 0.0) {
                    round(rawTotal * (cart.receiptDiscountPercent / 100.0)).toLong()
                } else 0L
                val subtotalAfterDiscount = (rawTotal - discountKopecks).coerceAtLeast(0L)
                val totalKopecks = when (cart.paymentType) {
                    PaymentType.CASH -> FiscalMath.roundCashPayment(subtotalAfterDiscount)
                    else -> subtotalAfterDiscount
                }

                val isReturn = cart.receiptType == ReceiptType.RETURN
                val bgColor = when {
                    isActive && isReturn -> MaterialTheme.colorScheme.error
                    isActive -> MaterialTheme.colorScheme.primary
                    hasItems && isReturn -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    hasItems -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }

                val contentColor = when {
                    isActive -> if (isReturn) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    hasItems && isReturn -> MaterialTheme.colorScheme.onErrorContainer
                    hasItems -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = tabMinHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectCart(index)
                        },
                    color = bgColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = tabPadVert, horizontal = if (isNarrow) 2.dp else 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isReturn) "↩️ ${cart.name}" else cart.name,
                                fontSize = if (isNarrow) 10.sp else 11.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (hasItems) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${cart.items.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (hasItems && totalKopecks > 0L) {
                            val sign = if (isReturn) "-" else ""
                            Text(
                                text = "$sign${PosCurrencyFormatter.formatKopecks(totalKopecks)} ₴",
                                fontSize = if (isNarrow) 9.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = emptyText,
                                fontSize = 9.sp,
                                color = contentColor.copy(alpha = 0.6f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
