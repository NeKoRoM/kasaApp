package com.kasa.transport

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScaleStreamTransportTest {

    @Test
    fun testScaleWeightStreamParsing() = runTest {
        val pipedOut = PipedOutputStream()
        val pipedIn = PipedInputStream(pipedOut)
        val outStream = ByteArrayOutputStream()

        val scaleTransport = ScaleStreamTransport()
        scaleTransport.startListening(pipedIn, outStream)

        // Пишемо потік показань ваги від ваг
        pipedOut.write("ST,GS,+  1.250kg\r\n".toByteArray(Charsets.US_ASCII))
        pipedOut.flush()

        var attempts = 0
        while (scaleTransport.currentWeight.value == null && attempts < 50) {
            Thread.sleep(20)
            attempts++
        }

        val reading = scaleTransport.currentWeight.value
        assertNotNull(reading)
        assertEquals(1250, reading.weightGrams)
        assertEquals(1.25, reading.weightKilograms)
        assertTrue(reading.isStable)

        scaleTransport.stopListening()
    }

    @Test
    fun testScaleTareCommand() = runTest {
        val pipedIn = PipedInputStream()
        val outStream = ByteArrayOutputStream()

        val scaleTransport = ScaleStreamTransport()
        scaleTransport.startListening(pipedIn, outStream)

        val result = scaleTransport.tare()
        assertTrue(result)

        val sentBytes = outStream.toByteArray()
        assertEquals("T\r\n", String(sentBytes, Charsets.US_ASCII))

        scaleTransport.stopListening()
    }
}
