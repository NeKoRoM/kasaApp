package com.kasa.protocol.fiscal

/**
 * Етапи виконання фіскального друку чека.
 */
enum class FiscalStep {
    VALIDATION,
    HARDWARE_CHECK,
    OPEN_RECEIPT,
    REGISTER_ITEMS,
    TOTAL_PAYMENT,
    CLOSE_RECEIPT,
    VOID_RECEIPT
}

/**
 * Типізовані коди фіскальних помилок.
 */
enum class FiscalError {
    HARDWARE_ERROR,
    SHIFT_EXPIRED_24H,
    TRANSPORT_DISCONNECTED,
    TIMEOUT,
    COMMAND_REJECTED,
    INVALID_RECEIPT,
    BCC_CHECKSUM_ERROR,
    VOIDING_FAILED
}

/**
 * Результат виконання операції фіскалізації чека на РРО.
 */
sealed interface FiscalReceiptResult {
    data class Success(
        val receiptId: String,
        val fiscalCheckNumber: String?,
        val timestamp: Long
    ) : FiscalReceiptResult

    data class Failure(
        val error: FiscalError,
        val step: FiscalStep,
        val message: String
    ) : FiscalReceiptResult
}

/**
 * Поточний стан виконання чекового пайплайну.
 */
sealed interface FiscalExecutionState {
    data object Idle : FiscalExecutionState

    data class Executing(
        val step: FiscalStep,
        val currentItemIndex: Int = 0,
        val totalItems: Int = 0
    ) : FiscalExecutionState

    data class Completed(
        val result: FiscalReceiptResult
    ) : FiscalExecutionState
}
