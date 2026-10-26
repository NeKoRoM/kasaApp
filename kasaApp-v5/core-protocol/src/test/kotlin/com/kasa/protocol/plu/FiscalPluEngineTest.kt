package com.kasa.protocol.plu

import com.kasa.model.Product
import com.kasa.model.TaxGroup
import com.kasa.protocol.fiscal.FiscalError
import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FiscalPluEngineTest {

    private lateinit var transport: SimulatedKasaTransport
    private lateinit var engine: FiscalPluEngine

    @BeforeTest
    fun setUp() {
        transport = SimulatedKasaTransport()
        engine = FiscalPluEngine(transport)
    }

    @Test
    fun readPlu_returns_product_when_item_exists() = runTest {
        transport.connect("TEST_PORT")

        val result = engine.readPlu(1L)

        assertTrue(result is FiscalPluResult.Success)
        val product = (result as FiscalPluResult.Success).data
        assertEquals(1L, product.id)
        assertEquals("Хліб білий", product.name)
        assertEquals(24.50, product.price)
        assertEquals(TaxGroup.GROUP_A, product.taxGroup)
    }

    @Test
    fun readPlu_returns_failure_when_item_not_found() = runTest {
        transport.connect("TEST_PORT")

        val result = engine.readPlu(999L)

        assertTrue(result is FiscalPluResult.Failure)
        val failure = result as FiscalPluResult.Failure
        assertEquals(FiscalError.COMMAND_REJECTED, failure.error)
        assertTrue(failure.message.contains("відсутній"))
    }

    @Test
    fun readPlu_returns_failure_when_transport_disconnected() = runTest {
        val result = engine.readPlu(1L)

        assertTrue(result is FiscalPluResult.Failure)
        assertEquals(FiscalError.TRANSPORT_DISCONNECTED, (result as FiscalPluResult.Failure).error)
    }

    @Test
    fun programPlu_writes_product_and_verifies_it() = runTest {
        transport.connect("TEST_PORT")

        val newProduct = Product(
            id = 4L,
            name = "Сир Голландський",
            price = 320.00,
            taxGroup = TaxGroup.GROUP_A,
            section = 1,
            group = 2,
            barcodes = listOf("482000000004"),
            isWeighted = true,
            unitOfMeasurement = "кг",
            remainder = 25.5
        )

        val writeResult = engine.programPlu(newProduct)
        assertTrue(writeResult is FiscalPluResult.Success)

        // Перевіряємо що товар дійсно збережено та зчитується
        val readResult = engine.readPlu(4L)
        assertTrue(readResult is FiscalPluResult.Success)
        val saved = (readResult as FiscalPluResult.Success).data
        assertEquals("Сир Голландський", saved.name)
        assertEquals(320.00, saved.price)
        assertTrue(saved.isWeighted)
    }

    @Test
    fun deletePlu_removes_item_from_kasa_memory() = runTest {
        transport.connect("TEST_PORT")

        // Переконуємось що товар 2 існує
        assertTrue(engine.readPlu(2L) is FiscalPluResult.Success)

        // Видаляємо товар 2
        val deleteResult = engine.deletePlu(2L)
        assertTrue(deleteResult is FiscalPluResult.Success)

        // Тепер запит має повернути відсутність товару
        val readResult = engine.readPlu(2L)
        assertTrue(readResult is FiscalPluResult.Failure)
    }

    @Test
    fun dumpAllPlu_reads_all_products_with_progress_updates() = runTest {
        transport.connect("TEST_PORT")

        val progressReports = mutableListOf<PluSyncProgress>()
        val dumpResult = engine.dumpAllPlu { progress ->
            progressReports.add(progress)
        }

        assertTrue(dumpResult is FiscalPluResult.Success)
        val products = (dumpResult as FiscalPluResult.Success).data
        assertEquals(3, products.size)
        assertEquals("Хліб білий", products[0].name)
        assertEquals("Молоко 2.5%", products[1].name)
        assertEquals("Горілка Преміум", products[2].name)

        assertTrue(progressReports.isNotEmpty())
        assertEquals(3, progressReports.last().current)
    }

    @Test
    fun uploadCatalog_programs_multiple_products_in_batch() = runTest {
        transport.connect("TEST_PORT")

        val catalog = listOf(
            Product(id = 10L, name = "Кава в зернах", price = 250.00),
            Product(id = 11L, name = "Чай чорний", price = 85.00),
            Product(id = 12L, name = "Цукор 1кг", price = 34.00)
        )

        val progressReports = mutableListOf<PluSyncProgress>()
        val result = engine.uploadCatalog(catalog) { progressReports.add(it) }

        assertTrue(result is FiscalPluResult.Success)
        assertEquals(3, (result as FiscalPluResult.Success).data)
        assertEquals(3, progressReports.size)

        // Перевіряємо що кава збереглась
        val checkResult = engine.readPlu(10L)
        assertTrue(checkResult is FiscalPluResult.Success)
        assertEquals("Кава в зернах", (checkResult as FiscalPluResult.Success).data.name)
    }

    @Test
    fun PluRecordParser_handles_valid_and_invalid_strings() {
        val validRaw = "006;R0\t5\tА\t1\t1\t0\t45.00\t0.00\t0.00\t12.00\t482005\t\t\t\tКруасан\t0\t\tшт\t"
        val parsed = PluRecordParser.parse(validRaw)

        assertNotNull(parsed)
        assertEquals(5L, parsed.id)
        assertEquals("Круасан", parsed.name)
        assertEquals(45.00, parsed.price)
        assertEquals(listOf("482005"), parsed.barcodes)

        assertNull(PluRecordParser.parse("006;-111004\t"))
        assertNull(PluRecordParser.parse("006;-1030\t"))
        assertNull(PluRecordParser.parse("INVALID_GARBAGE"))

        assertEquals(10L, PluRecordParser.parseLastCode("006;L0\t10\t"))
        assertEquals(0L, PluRecordParser.parseLastCode("006;L0\t0\t"))
        // Обрізаний код без завершального табулятора не повинен парситись як валідний код
        assertEquals(0L, PluRecordParser.parseLastCode("006;R0\t2"))
        assertEquals(29L, PluRecordParser.parseLastCode("006;R0\t29\tА\t1\t"))
    }

    @Test
    fun dumpAllPlu_recovers_from_truncated_frame_and_timeout_mid_stream() = runTest {
        val frameBuilder = com.kasa.protocol.frame.DatecsFrameBuilder()

        fun buildValidPluFrame(code: Long, name: String, price: String): ByteArray {
            val payload = "006;R0\t$code\tА\t1\t1\t0\t$price\t0.00\t0.00\t10.00\t48200$code\t\t\t\t$name\t0\t\tшт\t"
            return frameBuilder.buildFrame(payload)
        }

        var currentCursor = 1L
        var injectedDrop2 = false
        var injectedTimeout4 = false
        var lastResponseBytes: ByteArray? = null

        val flakyTransport = object : com.kasa.protocol.transport.KasaTransport {
            private val _state = kotlinx.coroutines.flow.MutableStateFlow<com.kasa.protocol.transport.TransportState>(
                com.kasa.protocol.transport.TransportState.Connected("FLAKY_BT")
            )
            override val state: kotlinx.coroutines.flow.StateFlow<com.kasa.protocol.transport.TransportState> = _state
            override suspend fun connect(address: String): Boolean = true
            override suspend fun disconnect() {}

            override suspend fun sendFrame(data: ByteArray): Boolean {
                val decoded = frameBuilder.decodeFrameOrPartial(data)?.payloadText ?: ""
                when {
                    decoded.startsWith("006;L") -> {
                        val payload = "006;L0\t5\t"
                        lastResponseBytes = frameBuilder.buildFrame(payload)
                    }
                    decoded.startsWith("006;F\t") -> {
                        val reqCode = decoded.split("\t").getOrNull(1)?.toLongOrNull() ?: 1L
                        currentCursor = reqCode
                        lastResponseBytes = buildValidPluFrame(currentCursor, "Товар #$currentCursor", "${currentCursor * 10}.00")
                    }
                    decoded.startsWith("006;N") -> {
                        val nextCode = currentCursor + 1L
                        if (nextCode == 2L && !injectedDrop2) {
                            injectedDrop2 = true
                            // Симулюємо обірваний кадр END_TRANSACTION (без 0x03 і без останніх полів)
                            val full = buildValidPluFrame(2L, "Товар #2", "20.00")
                            lastResponseBytes = full.copyOfRange(0, full.size / 2)
                        } else if (nextCode == 4L && !injectedTimeout4) {
                            injectedTimeout4 = true
                            // Симулюємо NOT_ANSWER (таймаут Bluetooth)
                            lastResponseBytes = null
                        } else if (nextCode <= 5L) {
                            currentCursor = nextCode
                            lastResponseBytes = buildValidPluFrame(currentCursor, "Товар #$currentCursor", "${currentCursor * 10}.00")
                        } else {
                            val endPayload = "006;-111014\t"
                            lastResponseBytes = frameBuilder.buildFrame(endPayload)
                        }
                    }
                }
                return true
            }

            override suspend fun receiveFrame(timeoutMs: Long): ByteArray? = lastResponseBytes
        }

        val flakyEngine = FiscalPluEngine(flakyTransport)
        val result = flakyEngine.dumpAllPlu()

        assertTrue(result is FiscalPluResult.Success)
        val dumped = (result as FiscalPluResult.Success).data
        assertEquals(5, dumped.size)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), dumped.map { it.id })
        assertEquals("Товар #2", dumped[1].name)
        assertEquals("Товар #4", dumped[3].name)
        assertEquals("Товар #5", dumped[4].name)
    }
}
