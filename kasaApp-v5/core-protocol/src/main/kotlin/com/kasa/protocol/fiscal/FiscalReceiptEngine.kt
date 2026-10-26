package com.kasa.protocol.fiscal

import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import com.kasa.protocol.command.DatecsCommand
import com.kasa.protocol.frame.DatecsFrameBuilder
import com.kasa.protocol.status.KasaStatus
import com.kasa.protocol.status.KasaStatusDecoder
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Скінченний автомат (FSM) та рушій фіскалізації чека на реєстраторі Datecs/Exellio.
 */
class FiscalReceiptEngine(
    private val transport: KasaTransport,
    private val frameBuilder: DatecsFrameBuilder = DatecsFrameBuilder()
) {

    private val _executionState = MutableStateFlow<FiscalExecutionState>(FiscalExecutionState.Idle)
    val executionState: StateFlow<FiscalExecutionState> = _executionState.asStateFlow()

    /**
     * Покроково виконує фіскальний друк чека [receipt].
     */
    suspend fun executeReceipt(receipt: Receipt): FiscalReceiptResult {
        // Крок 1: Валідація моделі чека
        if (!receipt.canBeFiscalized) {
            val failure = FiscalReceiptResult.Failure(
                error = FiscalError.INVALID_RECEIPT,
                step = FiscalStep.VALIDATION,
                message = "Чек порожній, містить некоректну суму або непідписані акцизні марки"
            )
            _executionState.value = FiscalExecutionState.Completed(failure)
            return failure
        }

        // Крок 2: Перевірка зв'язку
        if (!transport.isConnected) {
            val failure = FiscalReceiptResult.Failure(
                error = FiscalError.TRANSPORT_DISCONNECTED,
                step = FiscalStep.HARDWARE_CHECK,
                message = "Відсутній активний канал зв'язку з РРО"
            )
            _executionState.value = FiscalExecutionState.Completed(failure)
            return failure
        }

        // Режим реєстрації Exellio (передача товарів чека як штрих-кодів '23<PLU:5><QTY:5><CS>\r')
        if (transport.isScannerRegistrationMode) {
            return executeScannerRegistrationReceipt(receipt)
        }

        // Крок 3: Перевірка готовності апарата (RTC & Статуси)
        _executionState.value = FiscalExecutionState.Executing(FiscalStep.HARDWARE_CHECK)
        val hwStatus = sendAndCheckStatus(DatecsCommand.ReadDateTime) ?: return fail(
            FiscalError.TIMEOUT, FiscalStep.HARDWARE_CHECK, "Каса не відповідає на опитування статусу"
        )

        if (hwStatus.isReceiptPaperOut) {
            return fail(FiscalError.HARDWARE_ERROR, FiscalStep.HARDWARE_CHECK, "Закінчився чековий папір")
        }
        if (hwStatus.isCoverOpen) {
            return fail(FiscalError.HARDWARE_ERROR, FiscalStep.HARDWARE_CHECK, "Відкрита кришка принтера")
        }
        if (hwStatus.is24HoursShiftExpired) {
            return fail(FiscalError.SHIFT_EXPIRED_24H, FiscalStep.HARDWARE_CHECK, "Минуло понад 24 години зміни. Зніміть Z-звіт")
        }

        // Крок 4: Відкриття фіскального чека (0048)
        _executionState.value = FiscalExecutionState.Executing(FiscalStep.OPEN_RECEIPT)
        val openStatus = sendAndCheckStatus(DatecsCommand.OpenReceipt(receiptType = receipt.receiptType.code)) ?: return fail(
            FiscalError.TIMEOUT, FiscalStep.OPEN_RECEIPT, "Таймаут відкриття чека"
        )
        if (openStatus.hasSyntaxError || openStatus.isCommandNotAllowed) {
            return fail(FiscalError.COMMAND_REJECTED, FiscalStep.OPEN_RECEIPT, "Команда відкриття чека відхилена РРО")
        }

        // Крок 5: Позиційна реєстрація товарів (0049)
        val totalItems = receipt.items.size
        for ((index, item) in receipt.items.withIndex()) {
            _executionState.value = FiscalExecutionState.Executing(
                step = FiscalStep.REGISTER_ITEMS,
                currentItemIndex = index + 1,
                totalItems = totalItems
            )

            val saleCommand = DatecsCommand.RegisterSale(
                name = item.productName,
                priceKopecks = item.priceKopecks,
                quantity = item.quantity,
                taxGroupLetter = item.taxGroup.codeLetter,
                discountPercent = item.discountPercent
            )

            val saleStatus = sendAndCheckStatus(saleCommand)
            if (saleStatus == null || saleStatus.hasSyntaxError || saleStatus.isCommandNotAllowed) {
                voidReceipt()
                return fail(
                    FiscalError.COMMAND_REJECTED,
                    FiscalStep.REGISTER_ITEMS,
                    "Помилка реєстрації позиції '${item.productName}'. Чек анульовано"
                )
            }
        }

        // Загальна знижка на чек (0051 Subtotal), якщо задано
        if (receipt.discountPercent > 0.0) {
            val subtotalStatus = sendAndCheckStatus(DatecsCommand.Subtotal(discountPercent = receipt.discountPercent))
            if (subtotalStatus == null || subtotalStatus.hasSyntaxError) {
                voidReceipt()
                return fail(
                    FiscalError.COMMAND_REJECTED,
                    FiscalStep.REGISTER_ITEMS,
                    "Помилка застосування загальної знижки чека. Чек анульовано"
                )
            }
        }

        // Крок 6: Оплата чека (0054)
        _executionState.value = FiscalExecutionState.Executing(FiscalStep.TOTAL_PAYMENT)
        when (receipt.paymentType) {
            PaymentType.CASH -> {
                if (!executePayment(0, receipt.totalPayableKopecks)) {
                    return fail(FiscalError.COMMAND_REJECTED, FiscalStep.TOTAL_PAYMENT, "Помилка оплати готівкою. Чек анульовано")
                }
            }
            PaymentType.CARD -> {
                if (!executePayment(1, receipt.totalPayableKopecks)) {
                    return fail(FiscalError.COMMAND_REJECTED, FiscalStep.TOTAL_PAYMENT, "Помилка оплати карткою. Чек анульовано")
                }
            }
            PaymentType.COMBINED -> {
                val cardAmount = receipt.splitCardKopecks ?: 0L
                val cashAmount = receipt.splitCashKopecks ?: 0L
                if (cardAmount > 0L && !executePayment(1, cardAmount)) {
                    return fail(FiscalError.COMMAND_REJECTED, FiscalStep.TOTAL_PAYMENT, "Помилка оплати карткою (комбінована). Чек анульовано")
                }
                if (cashAmount > 0L && !executePayment(0, cashAmount)) {
                    return fail(FiscalError.COMMAND_REJECTED, FiscalStep.TOTAL_PAYMENT, "Помилка оплати готівкою (комбінована). Чек анульовано")
                }
            }
        }

        // Крок 7: Закриття та завершення друку (0056)
        _executionState.value = FiscalExecutionState.Executing(FiscalStep.CLOSE_RECEIPT)
        val closeFrame = sendAndReceive(DatecsCommand.CloseReceipt) ?: return fail(
            FiscalError.TIMEOUT, FiscalStep.CLOSE_RECEIPT, "Таймаут закриття чека"
        )

        val checkNumber = extractCheckNumber(closeFrame.payloadText)
        val success = FiscalReceiptResult.Success(
            receiptId = receipt.id,
            fiscalCheckNumber = checkNumber,
            timestamp = System.currentTimeMillis()
        )
        _executionState.value = FiscalExecutionState.Completed(success)
        return success
    }

    private suspend fun executePayment(paymentType: Int, amountKopecks: Long): Boolean {
        val status = sendAndCheckStatus(DatecsCommand.Total(paymentType = paymentType, amountKopecks = amountKopecks))
        if (status == null || status.hasSyntaxError || status.isCommandNotAllowed) {
            voidReceipt()
            return false
        }
        return true
    }

    /**
     * Виконує передачу позицій чека в касовий апарат Exellio у режимі «Реєстрація»
     * шляхом відправки 13-значних внутрішніх штрих-кодів ('23<PLU:5><INT:2><FRAC:3><CS>\r').
     */
    private suspend fun executeScannerRegistrationReceipt(receipt: Receipt): FiscalReceiptResult {
        val orderedItems = ExellioScannerCodec.orderItemsForRegistration(receipt)
        for ((index, item) in orderedItems.withIndex()) {
            _executionState.value = FiscalExecutionState.Executing(FiscalStep.REGISTER_ITEMS, index + 1, orderedItems.size)
            val itemBytes = ExellioScannerCodec.encodePluSaleBytes(item.productId, item.quantity)
            if (!transport.sendFrame(itemBytes)) {
                return fail(FiscalError.TRANSPORT_DISCONNECTED, FiscalStep.REGISTER_ITEMS, "Помилка відправки коду товару '${item.productName}' на РРО")
            }
            val stamp = item.exciseStamp?.trim()
            if (!stamp.isNullOrEmpty()) {
                kotlinx.coroutines.delay(200L)
                if (!transport.sendFrame(ExellioScannerCodec.encodeRawLineBytes(stamp))) {
                    return fail(FiscalError.TRANSPORT_DISCONNECTED, FiscalStep.REGISTER_ITEMS, "Помилка відправки акцизної марки '${item.productName}' на РРО")
                }
                if (index < orderedItems.lastIndex) kotlinx.coroutines.delay(3500L)
            } else if (index < orderedItems.lastIndex) {
                kotlinx.coroutines.delay(600L)
            }
        }
        val success = FiscalReceiptResult.Success(receipt.id, "РРО", System.currentTimeMillis())
        _executionState.value = FiscalExecutionState.Completed(success)
        return success
    }

    /**
     * Анулювання відкритого чека при аварійній ситуації.
     */
    suspend fun voidReceipt(): Boolean {
        _executionState.value = FiscalExecutionState.Executing(FiscalStep.VOID_RECEIPT)
        val status = sendAndCheckStatus(DatecsCommand.VoidReceipt)
        return status != null && !status.hasSyntaxError
    }

    private suspend fun sendAndCheckStatus(command: DatecsCommand): KasaStatus? {
        val decoded = sendAndReceive(command) ?: return null
        if (!decoded.isBccValid) return null
        return extractStatus(decoded.rawPayloadBytes)
    }

    private suspend fun sendAndReceive(command: DatecsCommand): DatecsFrameBuilder.DecodedFrame? {
        val frame = frameBuilder.buildFrame(command)
        if (!transport.sendFrame(frame)) return null
        val response = transport.receiveFrame() ?: return null
        return frameBuilder.decodeFrame(response)
    }

    private fun extractStatus(rawPayloadBytes: ByteArray): KasaStatus {
        return KasaStatusDecoder.extractFromPayload(rawPayloadBytes)
    }

    private fun extractCheckNumber(payloadText: String): String? {
        val parts = payloadText.split('\t')
        return parts.getOrNull(1)?.trim()?.ifEmpty { null }
    }

    private fun fail(error: FiscalError, step: FiscalStep, message: String): FiscalReceiptResult.Failure {
        val failure = FiscalReceiptResult.Failure(error, step, message)
        _executionState.value = FiscalExecutionState.Completed(failure)
        return failure
    }
}
