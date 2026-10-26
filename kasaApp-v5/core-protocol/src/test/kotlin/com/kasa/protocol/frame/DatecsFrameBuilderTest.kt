package com.kasa.protocol.frame

import com.kasa.protocol.command.DatecsCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatecsFrameBuilderTest {

    @Test
    fun testBuildAndDecodeReadDateTimeCommand() {
        val builder = DatecsFrameBuilder()
        builder.resetSequence()

        val frame = builder.buildFrame(DatecsCommand.ReadDateTime)

        // Перевіряємо граничні байти
        assertEquals(0x01.toByte(), frame.first())
        assertEquals(0x03.toByte(), frame.last())

        // Декодуємо кадр назад
        val decoded = builder.decodeFrame(frame)
        assertNotNull(decoded)
        assertTrue(decoded.isBccValid)
        assertEquals(0x40.toByte(), decoded.sequence)
        assertEquals("003>", decoded.payloadText)
    }

    @Test
    fun testBuildRegisterSaleCommandWithUkrainianLetters() {
        val builder = DatecsFrameBuilder()

        val saleCommand = DatecsCommand.RegisterSale(
            name = "Хліб Бородинський",
            priceKopecks = 2550L,
            quantity = 1.0,
            taxGroupLetter = "А"
        )

        val frame = builder.buildFrame(saleCommand)
        val decoded = builder.decodeFrame(frame)

        assertNotNull(decoded)
        assertTrue(decoded.isBccValid)
        assertTrue(decoded.payloadText.startsWith("0049Хліб Бородинський\t25.50\t1.000\tА\t"))
    }

    @Test
    fun testSequenceCycling() {
        val builder = DatecsFrameBuilder()
        builder.resetSequence()

        val firstSeq = builder.nextSequence()
        assertEquals(0x40.toByte(), firstSeq)

        // Промотуємо до 0x7B
        for (i in 0x41..0x7B) {
            val seq = builder.nextSequence()
            assertEquals(i.toByte(), seq)
        }

        // Після 0x7B має знову повернутися до 0x40
        val wrappedSeq = builder.nextSequence()
        assertEquals(0x40.toByte(), wrappedSeq)
    }
}
