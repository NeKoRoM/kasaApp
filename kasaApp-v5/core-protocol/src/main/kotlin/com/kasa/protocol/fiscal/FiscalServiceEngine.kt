package com.kasa.protocol.fiscal

import com.kasa.protocol.command.DatecsCommand
import com.kasa.protocol.frame.DatecsFrameBuilder
import com.kasa.protocol.status.KasaStatus
import com.kasa.protocol.status.KasaStatusDecoder
import com.kasa.protocol.transport.KasaTransport

/**
 * Результат виконання сервісної/адміністративної операції РРО.
 */
sealed interface ServiceResult {
    data class Success(val message: String, val payload: String = "") : ServiceResult
    data class Failure(val error: FiscalError, val message: String) : ServiceResult
}

/**
 * Рушій виконання сервісних функцій РРО:
 * - Друк X-звіту та Z-звіту
 * - Зчитування та синхронізація часу годинника (RTC)
 * - Опитування заводського та фіскального номерів
 */
class FiscalServiceEngine(
    private val transport: KasaTransport,
    private val frameBuilder: DatecsFrameBuilder = DatecsFrameBuilder()
) {

    /**
     * Друк денного X-звіту без обнулення оперативних регістрів.
     */
    suspend fun printXReport(): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.PrintXReport,
            successMessage = "X-звіт успішно роздруковано",
            errorMessage = "Помилка друку X-звіту"
        )
    }

    /**
     * Друк фіскального денного Z-звіту з обнуленням зміни.
     */
    suspend fun printZReport(): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.PrintZReport,
            successMessage = "Z-звіт успішно роздруковано. Зміну закрито",
            errorMessage = "Помилка друку Z-звіту"
        )
    }

    /**
     * Зчитування поточної дати та часу з годинника реального часу (RTC) каси.
     */
    suspend fun readDateTime(): ServiceResult {
        val decoded = sendAndReceive(DatecsCommand.ReadDateTime) ?: return ServiceResult.Failure(
            FiscalError.TIMEOUT, "Каса не відповідає на запит часу (перевірте, що каса у 6-му режимі ПК)"
        )
        val cleanDateTime = decoded.payloadText
            .substringAfter(DatecsCommand.ReadDateTime.rawCommandCode)
            .removePrefix("0\t")
            .substringBefore('\t')
            .substringBefore('\u0004')
            .trim()
        return ServiceResult.Success("Час зчитано успішно", cleanDateTime)
    }

    /**
     * Синхронізація годинника каси (формат: dd-MM-yy HH:mm:ss).
     */
    suspend fun setDateTime(formattedDateTime: String): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.SetDateTime(formattedDateTime),
            successMessage = "Час РРО успішно синхронізовано",
            errorMessage = "Помилка встановлення часу"
        )
    }

    /**
     * Запит заводського та фіскального номерів каси.
     */
    suspend fun getKasaId(): ServiceResult {
        val decoded = sendAndReceive(DatecsCommand.GetKasaId) ?: return ServiceResult.Failure(
            FiscalError.TIMEOUT, "Каса не відповідає на запит ідентифікатора (перевірте, що каса у 6-му режимі ПК)"
        )
        val cleanInfo = decoded.payloadText
            .substringAfter(DatecsCommand.GetKasaId.rawCommandCode)
            .removePrefix("0\t")
            .substringBefore('\u0004')
            .substringBeforeLast('\t')
            .trim()
        if (!cleanInfo.contains("ФН")) {
            val diag = sendAndReceive(DatecsCommand.ReadDiagnosticInfo)
            val parts = diag?.payloadText
                ?.substringAfter(DatecsCommand.ReadDiagnosticInfo.rawCommandCode)
                ?.removePrefix("0\t")
                ?.substringBefore('\u0004')
                ?.split('\t')
            if (parts != null && parts.size >= 8 && parts[0].isNotBlank()) {
                val fullId = "${parts[0]} (FW ${parts[1]}) | ЗН: ${parts[6]} | ФН: ${parts[7]}"
                return ServiceResult.Success("Ідентифікатори отримано", fullId)
            }
        }
        return ServiceResult.Success("Ідентифікатори отримано", cleanInfo)
    }

    /**
     * Службове внесення або видача готівки (додатне = внесення, від'ємне = видача).
     */
    suspend fun cashInOut(amountKopecks: Long): ServiceResult {
        val actionText = if (amountKopecks >= 0L) "Службове внесення" else "Службова видача"
        return executeSimpleCommand(
            command = DatecsCommand.CashInOut(amountKopecks),
            successMessage = "$actionText успішно проведено",
            errorMessage = "Помилка операції $actionText"
        )
    }

    /**
     * Відкриття грошової скриньки (імпульс на соленоїд).
     */
    suspend fun openDrawer(): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.OpenDrawer,
            successMessage = "Грошову скриньку відкрито",
            errorMessage = "Помилка відкриття скриньки"
        )
    }

    /**
     * Опитування податкових ставок РРО.
     */
    suspend fun readTaxRates(): ServiceResult {
        val decoded = sendAndReceive(DatecsCommand.ReadTaxRates) ?: return ServiceResult.Failure(
            FiscalError.TIMEOUT, "Каса не відповідає на запит ставок ПДВ"
        )
        val cleanRates = decoded.payloadText
            .substringAfter(DatecsCommand.ReadTaxRates.rawCommandCode)
            .removePrefix("0\t")
            .substringBefore('\u0004')
            .trim()
        return ServiceResult.Success("Ставки ПДВ отримано", cleanRates)
    }

    /**
     * Зчитування окремого рядка кліше (шапки) чека (0..6).
     */
    suspend fun readHeaderLine(index: Int): ServiceResult {
        val command = DatecsCommand.ReadHeaderLine(index)
        val decoded = sendAndReceive(command) ?: return ServiceResult.Failure(
            FiscalError.TIMEOUT, "Каса не відповідає на запит рядка кліше $index"
        )
        val body = decoded.payloadText.substringBefore('\u0004')
        val cleanLine = if (body.contains("Header")) {
            body.substringAfter("Header").substringAfter('\t').substringAfter('\t').substringBefore('\t').trim()
        } else {
            body.substringAfter(command.rawCommandCode).removePrefix("0\t").substringBefore('\t').trim()
        }
        return ServiceResult.Success("Рядок кліше $index зчитано", cleanLine)
    }

    /**
     * Програмування рядка кліше (шапки) чека (0..6).
     */
    suspend fun setHeaderLine(index: Int, text: String): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.ProgramHeaderLine(index, text),
            successMessage = "Рядок кліше $index успішно збережено",
            errorMessage = "Помилка встановлення рядка кліше $index"
        )
    }

    /**
     * Вивід тексту на дисплей покупця (1 = верхній рядок, 2 = нижній рядок, до 20 символів).
     */
    suspend fun displayText(line: Int, text: String): ServiceResult {
        val sanitized = text.take(20)
        return executeSimpleCommand(
            command = DatecsCommand.DisplayText(line, sanitized),
            successMessage = "Текст виведено на дисплей",
            errorMessage = "Помилка виводу на дисплей"
        )
    }

    /**
     * Очищення дисплея покупця (line: 0 = весь екран).
     */
    suspend fun clearDisplay(line: Int = 0): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.ClearDisplay(line),
            successMessage = "Дисплей очищено",
            errorMessage = "Помилка очищення дисплея"
        )
    }

    /**
     * Анулювання відкритого чека в РРО (Void / Анулювання).
     */
    suspend fun voidReceipt(): ServiceResult {
        return executeSimpleCommand(
            command = DatecsCommand.VoidReceipt,
            successMessage = "Чек успішно анульовано",
            errorMessage = "Помилка анулювання чека"
        )
    }

    /**
     * Опитування поточного апаратного стану РРО (6 або 8 статусних байтів після 0x04).
     */
    suspend fun queryStatus(): KasaStatus? {
        val decoded = sendAndReceive(DatecsCommand.ReadDateTime) ?: return null
        if (!decoded.isBccValid || decoded.rawPayloadBytes.size < 6) return null
        return KasaStatusDecoder.extractFromPayload(decoded.rawPayloadBytes)
    }

    private suspend fun executeSimpleCommand(
        command: DatecsCommand,
        successMessage: String,
        errorMessage: String
    ): ServiceResult {
        if (!transport.isConnected) {
            return ServiceResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "Касовий апарат не підключено")
        }
        val decoded = sendAndReceive(command) ?: return ServiceResult.Failure(
            FiscalError.TIMEOUT, "Каса не відповіла на команду"
        )
        if (!decoded.isBccValid) {
            return ServiceResult.Failure(FiscalError.BCC_CHECKSUM_ERROR, "Помилка контрольної суми відповіді")
        }
        val bodyAfterCmd = decoded.payloadText.substringAfter(command.rawCommandCode).substringBefore('\u0004')
        val status = KasaStatusDecoder.extractFromPayload(decoded.rawPayloadBytes)
        if (decoded.payloadText.contains("ERROR") || bodyAfterCmd.startsWith("-") ||
            status.hasSyntaxError || status.isInvalidCommand || status.isCommandNotAllowed
        ) {
            return ServiceResult.Failure(FiscalError.COMMAND_REJECTED, errorMessage)
        }
        return ServiceResult.Success(successMessage, decoded.payloadText)
    }

    private suspend fun sendAndReceive(command: DatecsCommand): DatecsFrameBuilder.DecodedFrame? {
        val frame = frameBuilder.buildFrame(command)
        if (!transport.sendFrame(frame)) return null
        val response = transport.receiveFrame() ?: return null
        return frameBuilder.decodeFrame(response)
    }
}
