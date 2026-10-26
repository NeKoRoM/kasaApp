package com.kasa.protocol.status

/**
 * Повний розбір 6 статусних байтів фіскального реєстратора Exellio / Datecs.
 * Усі прапорці типізовані, що унеможливлює неоднозначність інтерпретації.
 */
data class KasaStatus(
    // Байт 0: Синтаксис і критичні помилки
    val hasSyntaxError: Boolean = false,
    val isInvalidCommand: Boolean = false,
    val isRtcError: Boolean = false,
    val isCommandNotAllowed: Boolean = false,
    val hasBccError: Boolean = false,

    // Байт 1: Термопринтер і механіка
    val isControlPaperOut: Boolean = false,
    val isReceiptPaperOut: Boolean = false,
    val isCoverOpen: Boolean = false,
    val isHeadOverheated: Boolean = false,

    // Байт 2: Фіскальний стан
    val isFiscalized: Boolean = false,
    val isFiscalMemoryFull: Boolean = false,
    val is24HoursShiftExpired: Boolean = false,

    // Байт 4: Режим чека
    val isNonFiscalReceiptOpen: Boolean = false,
    val isFiscalReceiptOpen: Boolean = false,
    val isCustomerDisplayConnected: Boolean = false
) {
    /**
     * Чи готова каса до друку та реєстрації розрахункових операцій.
     */
    val canPrintReceipt: Boolean
        get() = !isReceiptPaperOut &&
                !isCoverOpen &&
                !isHeadOverheated &&
                !is24HoursShiftExpired &&
                !hasSyntaxError &&
                !hasBccError
}
