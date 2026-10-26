package com.kasa.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

/**
 * Вкладки компактного мобільного інтерфейсу касира (смартфони Android / iPhone).
 */
enum class PosCompactTab {
    CATALOG,
    CART
}

/**
 * Адаптивний компактний інтерфейс робочого місця касира для смартфонів (Android та iOS):
 * - Плавні горизонтальні свайп-переходи між каталогом та кошиком (HorizontalPager);
 * - Липка нижня панель швидкої оплати прямо під великим пальцем (Thumb Zone);
 * - Швидкий виклик сканера через плаваючу кнопку FAB або панель оплати;
 * - Адаптація під мобільні екрани Android та iOS (Thumb Zone);
 * - Тактильний вібровідгук (Haptic Feedback) та сенсорні зони >= 44 dp.
 */
@Composable
fun PosCompactLayout(
    state: PosState,
    onEvent: (PosEvent) -> Unit,
    onOpenConnectionDialog: (() -> Unit)? = null,
    onOpenTaxUrl: ((String) -> Unit)? = null,
    onShareReceipt: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Автоматичний перехід на стрічку чека при завантаженні повернення з журналу
    LaunchedEffect(state.receiptType, state.items.size) {
        if (state.receiptType == com.kasa.model.ReceiptType.RETURN && state.items.isNotEmpty()) {
            pagerState.animateScrollToPage(1)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscapeCompact = maxHeight < 480.dp
        val isNarrow = maxWidth < 360.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Компактний статусний рядок смартфона: стан РРО, касова зміна, клієнт, меню дій
            PosMobileTopBar(
                state = state,
                onEvent = onEvent,
                onOpenConnectionDialog = onOpenConnectionDialog,
                isLandscapeCompact = isLandscapeCompact
            )

            // Верхній рядок навігації між вітриною каталогу та стрічкою чека
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    modifier = Modifier.height(if (isLandscapeCompact) 36.dp else 44.dp),
                    text = {
                        Text(
                            text = if (isNarrow) "📦 (${state.catalogProducts.size})" else "📦 Каталог (${state.catalogProducts.size})",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isLandscapeCompact) 11.sp else 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                val isReturn = state.receiptType == com.kasa.model.ReceiptType.RETURN
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    modifier = Modifier.height(if (isLandscapeCompact) 36.dp else 44.dp),
                    selectedContentColor = if (isReturn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unselectedContentColor = if (isReturn) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isReturn) "↩️ Повернення" else "🛒 Чек",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isLandscapeCompact) 11.sp else 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.items.isNotEmpty()) {
                                Badge(
                                    containerColor = if (isReturn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isReturn) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                ) { Text("${state.items.size}") }
                            }
                        }
                    }
                )
            }

        // Горизонтальний свайп пальцем між вітриною та чеком (нативні жести Android / iOS)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PosCatalogPanel(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isMobile = true
                        )

                        PosCompactCatalogBottomBar(
                            itemsCount = state.items.size,
                            totalPayableKopecks = state.totalPayableKopecks,
                            receiptType = state.receiptType,
                            onOpenScanner = { onEvent(PosEvent.ToggleScannerDialog(true)) },
                            onNavigateToCart = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                            isLandscapeCompact = isLandscapeCompact
                        )
                    }
                }

                1 -> {
                    PosTapePanel(
                        state = state,
                        onEvent = onEvent,
                        onSelectCatalogTab = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        showRroStatus = false,
                        onOpenConnectionDialog = onOpenConnectionDialog,
                        onOpenTaxUrl = onOpenTaxUrl,
                        onShareReceipt = onShareReceipt,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Спливаючий мобільний банер статусів (не перекриває ані нижній бар каталогу, ані панель розрахунку в чеку)
    val bannerBottomPadding = when {
        pagerState.currentPage == 1 && state.items.isNotEmpty() -> 152.dp
        pagerState.currentPage == 0 -> 76.dp
        else -> 24.dp
    }
    PosStatusBanner(
        statusMessage = state.statusMessage,
        onDismiss = { onEvent(PosEvent.DismissStatusMessage) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bannerBottomPadding)
    )
}
}
