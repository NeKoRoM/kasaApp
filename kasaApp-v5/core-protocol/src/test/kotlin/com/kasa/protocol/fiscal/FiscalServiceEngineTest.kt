package com.kasa.protocol.fiscal

import com.kasa.protocol.transport.SimulatedKasaTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FiscalServiceEngineTest {

    private lateinit var transport: SimulatedKasaTransport
    private lateinit var serviceEngine: FiscalServiceEngine

    @BeforeTest
    fun setUp() = runTest {
        transport = SimulatedKasaTransport()
        transport.connect("SIMULATED_DATECS_02")
        serviceEngine = FiscalServiceEngine(transport)
    }

    @Test
    fun testPrintXReport() = runTest {
        val result = serviceEngine.printXReport()
        assertTrue(result is ServiceResult.Success)
        assertEquals("X-звіт успішно роздруковано", result.message)
    }

    @Test
    fun testPrintZReportResetsShift() = runTest {
        transport.simulateShiftExpired = true

        val result = serviceEngine.printZReport()
        assertTrue(result is ServiceResult.Success)
        assertEquals("Z-звіт успішно роздруковано. Зміну закрито", result.message)
        // Z-звіт має скинути прапорець вичерпання зміни
        assertEquals(false, transport.simulateShiftExpired)
    }

    @Test
    fun testReadAndSetDateTime() = runTest {
        val readResult = serviceEngine.readDateTime()
        assertTrue(readResult is ServiceResult.Success)
        assertTrue(readResult.payload.contains("25-09-26"))

        val setResult = serviceEngine.setDateTime("25-09-26 15:30:00")
        assertTrue(setResult is ServiceResult.Success)
    }

    @Test
    fun testGetKasaId() = runTest {
        val result = serviceEngine.getKasaId()
        assertTrue(result is ServiceResult.Success)
        assertTrue(result.payload.contains("DP05"))
    }

    @Test
    fun testCashInOut() = runTest {
        val inResult = serviceEngine.cashInOut(50000L)
        assertTrue(inResult is ServiceResult.Success)
        assertTrue(inResult.message.contains("внесення"))

        val outResult = serviceEngine.cashInOut(-20000L)
        assertTrue(outResult is ServiceResult.Success)
        assertTrue(outResult.message.contains("видача"))
    }

    @Test
    fun testOpenDrawer() = runTest {
        val result = serviceEngine.openDrawer()
        assertTrue(result is ServiceResult.Success)
        assertEquals("Грошову скриньку відкрито", result.message)
    }

    @Test
    fun testReadTaxRates() = runTest {
        val result = serviceEngine.readTaxRates()
        assertTrue(result is ServiceResult.Success)
        assertTrue(result.payload.contains("20.00"))
    }

    @Test
    fun testReadAndSetHeaderLines() = runTest {
        // Читання рядка 0
        val readResult = serviceEngine.readHeaderLine(0)
        assertTrue(readResult is ServiceResult.Success)
        assertEquals("ТОВ \"РОЗДРІБ ТРЕЙД\"", readResult.payload)

        // Запис нового тексту в рядок 0
        val setResult = serviceEngine.setHeaderLine(0, "ТОВ \"НОВА ТОРГІВЛЯ\"")
        assertTrue(setResult is ServiceResult.Success)

        // Перевірка оновленого значення
        val verifyResult = serviceEngine.readHeaderLine(0)
        assertTrue(verifyResult is ServiceResult.Success)
        assertEquals("ТОВ \"НОВА ТОРГІВЛЯ\"", verifyResult.payload)
    }

    @Test
    fun testDisplayTextAndClear() = runTest {
        val resultLine1 = serviceEngine.displayText(1, "Ласкаво просимо!")
        assertTrue(resultLine1 is ServiceResult.Success)
        assertEquals("Ласкаво просимо!", transport.simulatedDisplayLines[1])

        val resultLine2 = serviceEngine.displayText(2, "Сума: 150.00 грн")
        assertTrue(resultLine2 is ServiceResult.Success)
        assertEquals("Сума: 150.00 грн", transport.simulatedDisplayLines[2])

        // Очищення дисплея
        val clearResult = serviceEngine.clearDisplay()
        assertTrue(clearResult is ServiceResult.Success)
        assertTrue(transport.simulatedDisplayLines.isEmpty())
    }

    @Test
    fun testVoidReceipt() = runTest {
        transport.sendFrame(com.kasa.protocol.frame.DatecsFrameBuilder().buildFrame(com.kasa.protocol.command.DatecsCommand.OpenReceipt()))
        assertTrue(transport.isReceiptOpen)

        val voidResult = serviceEngine.voidReceipt()
        assertTrue(voidResult is ServiceResult.Success)
        assertEquals("Чек успішно анульовано", voidResult.message)
        assertEquals(false, transport.isReceiptOpen)
    }

    @Test
    fun testQueryStatus() = runTest {
        transport.simulatePaperOut = true
        transport.simulateCoverOpen = false

        val status = serviceEngine.queryStatus()
        kotlin.test.assertNotNull(status)
        assertTrue(status.isReceiptPaperOut)
        assertEquals(false, status.isCoverOpen)
    }
}
