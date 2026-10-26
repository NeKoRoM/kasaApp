package com.kasa.feature.pos

import com.kasa.model.CashierShift
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.protocol.fiscal.FiscalReceiptEngine
import com.kasa.protocol.fiscal.FiscalReceiptResult
import com.kasa.protocol.fiscal.FiscalServiceEngine
import com.kasa.protocol.fiscal.ServiceResult
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Асинхронний координатор фіскалізації чеків, касових змін та операцій з готівкою (Fiscal Receipt & Shift Coordinator).
 */
class PosFiscalCoordinator(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<PosState>,
    private val effect: Channel<PosEffect>,
    private val transport: KasaTransport,
    private val receiptEngine: FiscalReceiptEngine,
    private val serviceEngine: FiscalServiceEngine,
    private val serviceCoordinator: PosServiceCoordinator,
    private val onReceiptFiscalized: (suspend (Receipt) -> Unit)? = null,
    private val onDrawerBalanceUpdated: (suspend (Long) -> Unit)? = null,
    private val onCatalogUpdated: (suspend (List<Product>) -> Unit)? = null,
    private val onShiftUpdated: (suspend (CashierShift?) -> Unit)? = null,
    private val onShiftClosed: (suspend (CashierShift) -> Unit)? = null
) {

    fun ensureShiftOpen(cashierId: Long = 1L, cashierName: String = "Головний касир"): CashierShift {
        val existing = state.value.currentShift
        if (existing != null && existing.isOpen) {
            return existing
        }
        val newShift = PosFiscalLogic.ensureShiftOpen(
            existing = existing,
            cashierId = cashierId,
            cashierName = cashierName,
            currentDrawerBalanceKopecks = state.value.cashDrawerBalanceKopecks
        )
        state.update { it.copy(currentShift = newShift) }
        scope.launch { onShiftUpdated?.invoke(newShift) }
        return newShift
    }

    fun openShift(cashierId: Long, cashierName: String) {
        val shift = ensureShiftOpen(cashierId, cashierName)
        scope.launch {
            effect.send(PosEffect.ShowToast("Зміну №${shift.shiftNumber} відкрито ($cashierName)"))
        }
    }

    fun fiscalizeReceipt() {
        val current = state.value
        if (!current.canPay) return

        val activeShift = ensureShiftOpen()
        if (activeShift.isExpired24h || current.is24HoursShiftExpired) {
            scope.launch {
                effect.send(PosEffect.ShowErrorDialog("Касова зміна", "Зміна триває понад 24 години! Необхідно закрити зміну Z-звітом."))
            }
            return
        }

        val receipt = Receipt(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            cashierId = activeShift.cashierId,
            items = current.items,
            paymentType = current.paymentType,
            splitCashKopecks = current.splitCashKopecks,
            splitCardKopecks = current.splitCardKopecks,
            receiptType = current.receiptType,
            discountPercent = current.receiptDiscountPercent,
            customer = current.selectedCustomer
        )

        state.update { it.copy(isFiscalProcessing = true) }

        scope.launch {
            when (val result = receiptEngine.executeReceipt(receipt)) {
                is FiscalReceiptResult.Success -> {
                    val initialDrawer = state.value.cashDrawerBalanceKopecks
                    val postState = PosFiscalLogic.computePostReceiptState(
                        current = state.value,
                        receipt = receipt,
                        fiscalCheckNumber = result.fiscalCheckNumber,
                        hardwareInfo = state.value.kasaHardwareInfo
                    )
                    state.value = postState.newState
                    onReceiptFiscalized?.invoke(postState.completedReceipt)
                    if (postState.newDrawerBalance != initialDrawer) {
                        onDrawerBalanceUpdated?.invoke(postState.newDrawerBalance)
                    }
                    scope.launch { onCatalogUpdated?.invoke(postState.updatedCatalog) }
                    postState.updatedShift?.let { s -> scope.launch { onShiftUpdated?.invoke(s) } }

                    effect.send(PosEffect.ReceiptPrintedSuccess(result.fiscalCheckNumber ?: "OK"))
                    val checkNum = result.fiscalCheckNumber ?: "OK"
                    serviceCoordinator.displayCustomerText(line1 = "Дякуємо за покупку!", line2 = "Чек №$checkNum видано")
                }
                is FiscalReceiptResult.Failure -> {
                    state.update {
                        it.copy(
                            isFiscalProcessing = false,
                            statusMessage = "Помилка фіскалізації: ${result.message}"
                        )
                    }
                    effect.send(PosEffect.ShowErrorDialog(
                        title = "Помилка [${result.step}]",
                        message = result.message
                    ))
                }
            }
        }
    }

    fun cashInOut(amountKopecks: Long) {
        scope.launch {
            when (val res = serviceEngine.cashInOut(amountKopecks)) {
                is ServiceResult.Success -> {
                    val cashState = PosFiscalLogic.computeCashInOutState(state.value, amountKopecks, res.message)
                    state.value = cashState.newState
                    onDrawerBalanceUpdated?.invoke(cashState.newBalance)
                    cashState.updatedShift?.let { onShiftUpdated?.invoke(it) }
                    effect.send(PosEffect.ShowToast(res.message))
                }
                is ServiceResult.Failure -> {
                    effect.send(PosEffect.ShowErrorDialog("Грошова операція", res.message))
                }
            }
        }
    }

    fun printZReport() {
        scope.launch {
            when (val res = serviceEngine.printZReport()) {
                is ServiceResult.Success -> {
                    val closeState = PosFiscalLogic.computeCloseShiftState(
                        current = state.value,
                        zReportNumber = res.payload.takeIf { it.isNotBlank() } ?: "00001",
                        statusMessage = res.message
                    )
                    state.value = closeState.newState
                    closeState.closedShift?.let { cs ->
                        onShiftClosed?.invoke(cs)
                        onShiftUpdated?.invoke(null)
                    }
                    if (!transport.isScannerRegistrationMode) {
                        serviceCoordinator.checkKasaHealth()
                    }
                    effect.send(PosEffect.ShowToast(res.message))
                }
                is ServiceResult.Failure -> {
                    if (transport.isScannerRegistrationMode) {
                        val closeState = PosFiscalLogic.computeCloseShiftState(
                            current = state.value,
                            zReportNumber = "РРО",
                            statusMessage = "Локальну зміну закрито (Z-звіт на касі виконується в режимі звітів РРО)"
                        )
                        state.value = closeState.newState
                        closeState.closedShift?.let { cs ->
                            onShiftClosed?.invoke(cs)
                            onShiftUpdated?.invoke(null)
                        }
                        effect.send(PosEffect.ShowToast("Локальну зміну закрито"))
                    } else {
                        effect.send(PosEffect.ShowErrorDialog("Z-Звіт", res.message))
                    }
                }
            }
        }
    }
}
