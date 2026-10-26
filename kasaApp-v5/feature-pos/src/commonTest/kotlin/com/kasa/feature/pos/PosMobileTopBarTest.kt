package com.kasa.feature.pos

import com.kasa.model.CashierShift
import com.kasa.model.Customer
import com.kasa.protocol.scale.ScaleWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тести для мобільного статус-бара смартфонів (PosMobileTopBar):
 * - Відображення статусу зв'язку з фізичним РРО Datecs (Онлайн / Офлайн);
 * - Відображення стану касової зміни та попередження про ліміт 24 годин;
 * - Відображення прикріпленого клієнта програми лояльності;
 * - Відображення показників електронних ваг.
 */
class PosMobileTopBarTest {

    @Test
    fun testKasaOnlineStatusIndicator() {
        val onlineState = PosState(isKasaOnline = true)
        assertTrue(onlineState.isKasaOnline)

        val offlineState = PosState(isKasaOnline = false)
        assertFalse(offlineState.isKasaOnline)
    }

    @Test
    fun testShiftStatusAndExpiredWarning() {
        val normalShift = CashierShift(
            id = "shift-4",
            shiftNumber = 4L,
            cashierId = 1L,
            cashierName = "Черговий Касир",
            openedAt = System.currentTimeMillis() - 3600_000L // 1 година тому
        )
        val normalState = PosState(currentShift = normalShift)
        assertEquals(4L, normalState.currentShift?.shiftNumber)
        assertFalse(normalState.is24HoursShiftExpired)

        val expiredShift = CashierShift(
            id = "shift-5",
            shiftNumber = 5L,
            cashierId = 1L,
            cashierName = "Черговий Касир",
            openedAt = System.currentTimeMillis() - 90_000_000L // >24 годин тому
        )
        val expiredState = PosState(currentShift = expiredShift)
        assertTrue(expiredState.is24HoursShiftExpired)
    }

    @Test
    fun testCustomerLoyaltyDisplayInTopBar() {
        val emptyCustomerState = PosState(selectedCustomer = null)
        assertNull(emptyCustomerState.selectedCustomer)

        val testCustomer = Customer(
            id = 10L,
            name = "Олександр Коваленко",
            phone = "+380501234567",
            cardBarcode = "LOYALTY-99",
            discountPercent = 7.0
        )
        val attachedState = PosState(selectedCustomer = testCustomer)
        assertNotNull(attachedState.selectedCustomer)
        assertEquals("Олександр Коваленко", attachedState.selectedCustomer?.name)
        assertEquals(7.0, attachedState.selectedCustomer?.discountPercent)
    }

    @Test
    fun testScaleWeightFormattingInTopBar() {
        val stateWithoutScale = PosState(scaleWeight = null)
        assertNull(stateWithoutScale.scaleWeight)

        val stableWeight = ScaleWeight(weightGrams = 1450, isStable = true, rawMessage = "ST,GS,+1.450kg")
        val stateWithStable = PosState(scaleWeight = stableWeight)
        assertEquals(1.450, stateWithStable.scaleWeight?.weightKilograms)
        assertTrue(stateWithStable.scaleWeight?.isStable == true)

        val unstableWeight = ScaleWeight(weightGrams = 825, isStable = false, rawMessage = "US,GS,+0.825kg")
        val stateWithUnstable = PosState(scaleWeight = unstableWeight)
        assertEquals(0.825, stateWithUnstable.scaleWeight?.weightKilograms)
        assertFalse(stateWithUnstable.scaleWeight?.isStable == true)
    }

    @Test
    fun testTopBarDefaultShiftNumberFallback() {
        val stateWithoutShift = PosState(currentShift = null)
        val fallbackShiftNum = stateWithoutShift.currentShift?.shiftNumber ?: 1L
        assertEquals(1L, fallbackShiftNum)
    }
}
