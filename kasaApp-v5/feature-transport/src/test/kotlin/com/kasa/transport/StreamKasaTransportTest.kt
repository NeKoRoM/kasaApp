package com.kasa.transport

import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreamKasaTransportTest {

    @Test
    fun testSendFrameWritesToOutputStream() = runTest {
        val inStream = ByteArrayInputStream(byteArrayOf())
        val outStream = ByteArrayOutputStream()
        val transport = StreamKasaTransport(inStream, outStream, "test-stream")

        assertTrue(transport.isConnected)
        assertIs<TransportState.Connected>(transport.state.value)

        val testData = byteArrayOf(0x01, 0x30, 0x35, 0x03)
        val success = transport.sendFrame(testData)

        assertTrue(success)
        assertEquals(4, outStream.toByteArray().size)
        assertTrue(testData.contentEquals(outStream.toByteArray()))
    }

    @Test
    fun testReceiveFrameExtractsSohEtxFrame() = runTest {
        val pipedOut = PipedOutputStream()
        val pipedIn = PipedInputStream(pipedOut)

        val outStream = ByteArrayOutputStream()
        val transport = StreamKasaTransport(pipedIn, outStream, "test-stream")

        // Симулюємо відправку фрагментованого кадру Datecs: <0x01> "003>" <0x03>
        val frameToSend = byteArrayOf(0x01, 0x30, 0x30, 0x33, 0x3E, 0x03)

        val writeJob = launch {
            delay(50)
            pipedOut.write(frameToSend.copyOfRange(0, 3))
            pipedOut.flush()
            delay(50)
            pipedOut.write(frameToSend.copyOfRange(3, frameToSend.size))
            pipedOut.flush()
        }

        val received = transport.receiveFrame(timeoutMs = 1000L)
        writeJob.join()

        assertNotNull(received)
        assertTrue(frameToSend.contentEquals(received))

        transport.disconnect()
        assertEquals(TransportState.Disconnected, transport.state.value)
    }

    @Test
    fun testReceiveFrameTimeoutReturnsNull() = runTest {
        val inStream = ByteArrayInputStream(byteArrayOf())
        val outStream = ByteArrayOutputStream()
        val transport = StreamKasaTransport(inStream, outStream, "test-stream")

        val received = transport.receiveFrame(timeoutMs = 100L)
        assertNull(received)
    }

    @Test
    fun testReceiveFrameFlushesIncompleteFrameAfterSilence() = runTest {
        val pipedOut = PipedOutputStream()
        val pipedIn = PipedInputStream(pipedOut)

        val outStream = ByteArrayOutputStream()
        val transport = StreamKasaTransport(pipedIn, outStream, "test-stream")

        // Обірваний кадр без 0x03 (ETX), як у END_TRANSACTION в kasaApp3.0
        val partialFrame = byteArrayOf(0x01, 0x30, 0x30, 0x39, 0x30, 0x36, 0x30, 0x30, 0x36, 0x3B, 0x52, 0x30, 0x09, 0x31, 0x37, 0x09)
        pipedOut.write(partialFrame)
        pipedOut.flush()

        val received = transport.receiveFrame(timeoutMs = 1000L)
        assertNotNull(received)
        assertTrue(partialFrame.contentEquals(received))

        transport.disconnect()
    }
}
