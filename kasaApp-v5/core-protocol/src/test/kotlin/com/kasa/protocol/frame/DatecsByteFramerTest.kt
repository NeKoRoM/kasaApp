package com.kasa.protocol.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatecsByteFramerTest {

    @Test
    fun testExtractSingleCompleteFrame() {
        val framer = DatecsByteFramer()
        val mockFrame = byteArrayOf(0x01, 0x30, 0x30, 0x32, 0x30, 0x40, 0x05, 0x30, 0x30, 0x30, 0x30, 0x03)

        val frames = framer.appendAndExtractFrames(mockFrame)
        assertEquals(1, frames.size)
        assertTrue(frames[0].contentEquals(mockFrame))
    }

    @Test
    fun testExtractMultipleFramesInSingleChunk() {
        val framer = DatecsByteFramer()
        val frame1 = byteArrayOf(0x01, 0x30, 0x31, 0x03)
        val frame2 = byteArrayOf(0x01, 0x30, 0x32, 0x03)
        val combined = frame1 + frame2

        val frames = framer.appendAndExtractFrames(combined)
        assertEquals(2, frames.size)
        assertTrue(frames[0].contentEquals(frame1))
        assertTrue(frames[1].contentEquals(frame2))
    }

    @Test
    fun testDefragmentationAcrossMultipleChunks() {
        val framer = DatecsByteFramer()
        val frame = byteArrayOf(0x01, 0x31, 0x32, 0x33, 0x34, 0x35, 0x03)

        // Чанк 1: перша половина (без ETX)
        val chunk1 = byteArrayOf(0x01, 0x31, 0x32)
        val frames1 = framer.appendAndExtractFrames(chunk1)
        assertEquals(0, frames1.size)

        // Чанк 2: залишок з ETX
        val chunk2 = byteArrayOf(0x33, 0x34, 0x35, 0x03)
        val frames2 = framer.appendAndExtractFrames(chunk2)
        assertEquals(1, frames2.size)
        assertTrue(frames2[0].contentEquals(frame))
    }

    @Test
    fun testIgnoreGarbageBeforeSoh() {
        val framer = DatecsByteFramer()
        val garbage = byteArrayOf(0xFF.toByte(), 0x00, 0x20)
        val frame = byteArrayOf(0x01, 0x35, 0x03)
        val input = garbage + frame

        val frames = framer.appendAndExtractFrames(input)
        assertEquals(1, frames.size)
        assertTrue(frames[0].contentEquals(frame))
    }

    @Test
    fun testTruncatedFrameFollowedByNewSohFrameDiscardsPartial() {
        val framer = DatecsByteFramer()
        // Перший кадр обірвався без 0x03 (ETX)
        val truncated = byteArrayOf(0x01, 0x30, 0x30, 0x36, 0x3B, 0x52, 0x30)
        assertEquals(0, framer.appendAndExtractFrames(truncated).size)
        assertTrue(framer.hasPendingFrame())

        // Надійшов новий валідний кадр після повторного запиту ReadFirstPlu
        val validRetryFrame = byteArrayOf(0x01, 0x30, 0x30, 0x36, 0x3B, 0x52, 0x30, 0x09, 0x31, 0x38, 0x03)
        val extracted = framer.appendAndExtractFrames(validRetryFrame)
        assertEquals(1, extracted.size)
        assertTrue(extracted[0].contentEquals(validRetryFrame))
    }

    @Test
    fun testHasPendingFrameAndFlushIncompleteFrame() {
        val framer = DatecsByteFramer()
        val partial = byteArrayOf(0x01, 0x30, 0x30, 0x39, 0x30, 0x33, 0x30, 0x30, 0x36, 0x3B, 0x52, 0x30, 0x09, 0x32, 0x39, 0x09)
        assertEquals(0, framer.appendAndExtractFrames(partial).size)
        assertTrue(framer.hasPendingFrame())

        val flushed = framer.flushIncompleteFrame()
        assertTrue(flushed != null && flushed.contentEquals(partial))
        assertEquals(false, framer.hasPendingFrame())
    }
}
