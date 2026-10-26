package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyboardLayoutCorrectorTest {

    @Test
    fun testToUkrainianConversion() {
        assertEquals("хліб", KeyboardLayoutCorrector.toUkrainian("[ks,"))
        assertEquals("молоко", KeyboardLayoutCorrector.toUkrainian("vjkjrj"))
        assertEquals("кава", KeyboardLayoutCorrector.toUkrainian("rfdf"))
    }

    @Test
    fun testMatchesQuery() {
        val productName = "Хліб Бородинський нарізний"
        assertTrue(KeyboardLayoutCorrector.matchesQuery(productName, "хліб"))
        assertTrue(KeyboardLayoutCorrector.matchesQuery(productName, "hjlby")) // "родин" в англійській розкладці
    }
}

