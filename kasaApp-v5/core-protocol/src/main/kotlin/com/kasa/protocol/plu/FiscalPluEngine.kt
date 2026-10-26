package com.kasa.protocol.plu

import com.kasa.model.Product
import com.kasa.protocol.command.DatecsCommand
import com.kasa.protocol.fiscal.FiscalError
import com.kasa.protocol.frame.DatecsFrameBuilder
import com.kasa.protocol.transport.KasaTransport
import kotlinx.coroutines.delay

/**
 * Рушій взаємодії з енергонезалежною базою артикулів (PLU) касового апарата Datecs / Exellio:
 * - Покроковий дамп товарної бази з РРО зі стійким відновленням при обривах Bluetooth-пакетів
 * - Програмування (запис) товарів у пам'ять каси з верифікацією
 * - Видалення окремих артикулів
 */
class FiscalPluEngine(
    private val transport: KasaTransport,
    private val frameBuilder: DatecsFrameBuilder = DatecsFrameBuilder()
) {

    /**
     * Зчитування окремого артикулу за номером PLU.
     */
    suspend fun readPlu(pluCode: Long): FiscalPluResult<Product> {
        if (!transport.isConnected) {
            return FiscalPluResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "РРО не підключено")
        }

        val decoded = sendAndReceiveWithRetry(DatecsCommand.ReadPlu(pluCode), maxAttempts = 3)
            ?: return FiscalPluResult.Failure(FiscalError.TIMEOUT, "Каса не відповідає на запит артикулу $pluCode")

        if (decoded.payloadText.contains("-111004")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Товар з кодом $pluCode відсутній у касі")
        }
        if (decoded.payloadText.contains("-1030")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Товар $pluCode заблоковано через активні продажі у зміні")
        }

        val product = PluRecordParser.parse(decoded.payloadText)
            ?: return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Помилка розбору відповіді РРО", decoded.payloadText)

        return FiscalPluResult.Success(product, "Товар успішно зчитано")
    }

    /**
     * Запис (програмування) товару в енергонезалежну пам'ять РРО з верифікацією.
     */
    suspend fun programPlu(product: Product): FiscalPluResult<Unit> {
        if (!transport.isConnected) {
            return FiscalPluResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "РРО не підключено")
        }

        val taxCode = if (product.taxGroup.hasExcise && product.taxGroup.codeLetter == "А") {
            "6"
        } else {
            product.taxGroup.codeLetter
        }

        val command = DatecsCommand.ProgramPlu(
            pluCode = product.id,
            taxGroupLetter = taxCode,
            section = product.section,
            group = product.group,
            isWeighted = product.isWeighted,
            priceKopecks = kotlin.math.round(product.price * 100.0).toLong(),
            remainder = product.remainder,
            barcodes = product.barcodes,
            name = product.printableName,
            uktzed = product.uktzed,
            unitName = product.unitOfMeasurement
        )

        val writeResponse = sendAndReceiveWithRetry(command, maxAttempts = 2)
            ?: return FiscalPluResult.Failure(FiscalError.TIMEOUT, "Каса не відповіла на запис товару ${product.name}")

        if (writeResponse.payloadText.contains("-1030")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Товар ${product.id} заблоковано (проданий у відкритій зміні)")
        }
        if (writeResponse.payloadText.contains("ERROR") || writeResponse.payloadText.contains("-111")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Помилка програмування товару ${product.name}")
        }

        // Крок верифікації: перечитуємо щойно записаний товар
        delay(60L)
        val verifyResponse = sendAndReceiveWithRetry(DatecsCommand.ReadPlu(product.id), maxAttempts = 2)
        if (verifyResponse == null || verifyResponse.payloadText.contains("-111004")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Верифікація запису товару ${product.name} не пройшла")
        }
        return FiscalPluResult.Success(Unit, "Товар ${product.name} успішно запрограмовано в РРО")
    }

    /** Видалення артикулу з пам'яті РРО за кодом. */
    suspend fun deletePlu(pluCode: Long): FiscalPluResult<Unit> {
        if (!transport.isConnected) {
            return FiscalPluResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "РРО не підключено")
        }
        val decoded = sendAndReceiveWithRetry(DatecsCommand.DeletePlu(pluCode), maxAttempts = 2)
            ?: return FiscalPluResult.Failure(FiscalError.TIMEOUT, "Каса не відповіла на команду видалення")
        if (decoded.payloadText.contains("-1030")) {
            return FiscalPluResult.Failure(FiscalError.COMMAND_REJECTED, "Неможливо видалити: товар $pluCode продавався у поточній зміні")
        }
        return FiscalPluResult.Success(Unit, "Товар $pluCode видалено з РРО")
    }

    /** Повне вивантаження товарної бази з пам'яті РРО зі стійким відновленням (checkReadingDB / END_TRANSACTION). */
    suspend fun dumpAllPlu(
        onProgress: (PluSyncProgress) -> Unit = {}
    ): FiscalPluResult<List<Product>> {
        if (!transport.isConnected) {
            return FiscalPluResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "РРО не підключено")
        }

        var lastCode = 0L
        var gotLastResponse = false
        for (attempt in 1..3) {
            val decoded = sendAndReceive(DatecsCommand.ReadLastPlu)
            if (decoded != null) {
                gotLastResponse = true
                if (decoded.payloadText.contains("-111004")) break
                lastCode = PluRecordParser.parseLastCode(decoded.payloadText)
                if (lastCode > 0L) break
            }
            if (attempt < 3) delay(80L)
        }
        if (!gotLastResponse) {
            return FiscalPluResult.Failure(
                FiscalError.TIMEOUT,
                "Немає відповіді на запит бази (переконайтесь, що каса у 6-му режимі ПК/Програмування)"
            )
        }
        if (lastCode <= 0L) {
            return FiscalPluResult.Success(emptyList(), "База артикулів РРО порожня")
        }

        val productsById = LinkedHashMap<Long, Product>()
        var currentFrame = sendAndReceiveWithRetry(DatecsCommand.ReadFirstPlu(1L), maxAttempts = 4)
        var currentCode = 0L

        while (currentCode < lastCode) {
            val payload = currentFrame?.payloadText
            if (payload != null && (payload.contains("-111004") || payload.contains("ERROR"))) {
                break
            }

            val product = if (payload != null) PluRecordParser.parse(payload) else null
            if (product != null) {
                productsById[product.id] = product
                currentCode = product.id
                onProgress(PluSyncProgress(productsById.size, lastCode.toInt(), product.name))
                if (currentCode >= lastCode) break
                currentFrame = sendAndReceive(DatecsCommand.ReadNextPlu)
            } else {
                currentFrame = recoverPluRead(currentFrame, currentCode, lastCode) ?: break
            }
        }

        val products = productsById.values.toList()
        if (products.isNotEmpty()) {
            onProgress(PluSyncProgress(products.size, products.size, products.last().name))
        }
        return FiscalPluResult.Success(products, "Успішно завантажено ${products.size} товарів з РРО")
    }

    /** Відновлює читання бази після неповного кадру або таймауту через пряму адресацію 006;F\t<targetCode>\t. */
    private suspend fun recoverPluRead(
        failedFrame: DatecsFrameBuilder.DecodedFrame?,
        currentCode: Long,
        lastCode: Long
    ): DatecsFrameBuilder.DecodedFrame? {
        val partialCode = failedFrame?.let { PluRecordParser.parseLastCode(it.payloadText) } ?: 0L
        var targetCode = if (partialCode > currentCode) partialCode else (currentCode + 1L)

        for (skipStep in 0..2) {
            if (targetCode > lastCode) return null
            for (attempt in 1..5) {
                delay(100L)
                val retryFrame = sendAndReceive(DatecsCommand.ReadFirstPlu(targetCode)) ?: continue
                if (retryFrame.payloadText.contains("-111004")) return null
                if (PluRecordParser.parse(retryFrame.payloadText) != null) return retryFrame
                val newPartial = PluRecordParser.parseLastCode(retryFrame.payloadText)
                if (newPartial > currentCode) targetCode = newPartial
            }
            targetCode++
        }
        return null
    }

    /** Пакетне програмування каталогу товарів у пам'ять РРО. */
    suspend fun uploadCatalog(
        products: List<Product>,
        onProgress: (PluSyncProgress) -> Unit = {}
    ): FiscalPluResult<Int> {
        if (!transport.isConnected) {
            return FiscalPluResult.Failure(FiscalError.TRANSPORT_DISCONNECTED, "РРО не підключено")
        }
        if (products.isEmpty()) {
            return FiscalPluResult.Success(0, "Каталог товарів порожній")
        }

        var programmedCount = 0
        for ((index, product) in products.withIndex()) {
            val result = programPlu(product)
            if (result is FiscalPluResult.Success) programmedCount++
            onProgress(PluSyncProgress(index + 1, products.size, product.name))
        }
        return FiscalPluResult.Success(programmedCount, "Запрограмовано $programmedCount з ${products.size} товарів")
    }

    private suspend fun sendAndReceiveWithRetry(
        command: DatecsCommand,
        maxAttempts: Int = 3
    ): DatecsFrameBuilder.DecodedFrame? {
        for (attempt in 1..maxAttempts) {
            val decoded = sendAndReceive(command)
            if (decoded != null) return decoded
            if (attempt < maxAttempts) delay(80L)
        }
        return null
    }

    private suspend fun sendAndReceive(command: DatecsCommand): DatecsFrameBuilder.DecodedFrame? {
        delay(25L)
        val frame = frameBuilder.buildFrame(command)
        if (!transport.sendFrame(frame)) return null
        val response = transport.receiveFrame(timeoutMs = 950L) ?: return null
        return frameBuilder.decodeFrameOrPartial(response)
    }
}
