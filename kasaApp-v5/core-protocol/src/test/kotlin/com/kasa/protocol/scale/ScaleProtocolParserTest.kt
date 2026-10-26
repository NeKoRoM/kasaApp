package com.kasa.protocol.scale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScaleProtocolParserTest {

    @Test
    fun testParseStableWeight() {
        val message = "ST,GS,+  1.250kg\r\n"
        val scaleWeight = ScaleProtocolParser.parse(message)

        assertNotNull(scaleWeight)
        assertEquals(1250, scaleWeight.weightGrams)
        assertEquals(1.250, scaleWeight.weightKilograms)
        assertTrue(scaleWeight.isStable)
    }

    @Test
    fun testParseUnstableWeight() {
        val message = "US,GS,+  0.504kg\r\n"
        val scaleWeight = ScaleProtocolParser.parse(message)

        assertNotNull(scaleWeight)
        assertEquals(504, scaleWeight.weightGrams)
        assertFalse(scaleWeight.isStable)
    }

    @Test
    fun testParseNegativeWeight() {
        val message = "ST,GS,-  0.020kg\r\n"
        val scaleWeight = ScaleProtocolParser.parse(message)

        assertNotNull(scaleWeight)
        assertEquals(-20, scaleWeight.weightGrams)
        assertTrue(scaleWeight.isStable)
    }

    @Test
    fun testParseInvalidOrEmptyReturnsNull() {
        assertNull(ScaleProtocolParser.parse(""))
        assertNull(ScaleProtocolParser.parse("   "))
        assertNull(ScaleProtocolParser.parse("ERROR"))
    }

    @Test
    fun testTareCommandBytes() {
        val expected = "T\r\n".toByteArray(Charsets.US_ASCII)
        assertTrue(ScaleProtocolParser.TARE_COMMAND_BYTES.contentEquals(expected))
    }
}
