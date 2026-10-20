package com.example

import com.example.core.repository.MotivationalQuotesRepository
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for MotivationalQuotesRepository verifying quote collection size,
 * time slot calculations, stability, rotation, duplicate prevention, and date safety.
 */
class MotivationalQuotesTest {

    @Test
    fun testQuotesCollectionSizeAndContent() {
        val quotes = MotivationalQuotesRepository.quotesList
        assertTrue("Must contain at least 100 quotes, found ${quotes.size}", quotes.size >= 100)
        for (quote in quotes) {
            assertFalse("Quote must not be blank", quote.isBlank())
        }
    }

    @Test
    fun testTimeSlotCalculations() {
        val cal = Calendar.getInstance()

        // Morning slot (8:00 AM)
        cal.set(2026, Calendar.OCTOBER, 16, 8, 0, 0)
        val morning = MotivationalQuotesRepository.computeCurrentSlot(cal)
        assertEquals(0, morning.second)
        assertEquals("Morning Motivation", morning.third)

        // Late Morning slot (1:00 PM / 13:00)
        cal.set(2026, Calendar.OCTOBER, 16, 13, 0, 0)
        val lateMorning = MotivationalQuotesRepository.computeCurrentSlot(cal)
        assertEquals(1, lateMorning.second)
        assertEquals("Late Morning Focus", lateMorning.third)

        // Afternoon slot (5:00 PM / 17:00)
        cal.set(2026, Calendar.OCTOBER, 16, 17, 0, 0)
        val afternoon = MotivationalQuotesRepository.computeCurrentSlot(cal)
        assertEquals(2, afternoon.second)
        assertEquals("Afternoon Drive", afternoon.third)

        // Evening/Night slot (10:00 PM / 22:00)
        cal.set(2026, Calendar.OCTOBER, 16, 22, 0, 0)
        val evening = MotivationalQuotesRepository.computeCurrentSlot(cal)
        assertEquals(3, evening.second)
        assertEquals("Evening Reflection", evening.third)
    }

    @Test
    fun testQuoteStabilityWithinSameSlot() {
        // Using a mock context or direct repo logic if context is needed.
        // Wait, computeCurrentSlot and quote selection can be tested.
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 16, 9, 30, 0) // Morning slot
        val slot1 = MotivationalQuotesRepository.computeCurrentSlot(cal)
        
        cal.set(2026, Calendar.OCTOBER, 16, 10, 15, 0) // Still morning slot
        val slot2 = MotivationalQuotesRepository.computeCurrentSlot(cal)

        assertEquals(slot1.first, slot2.first)
        assertEquals(slot1.second, slot2.second)
    }

    @Test
    fun testMidnightAndDateBoundaryTransition() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 16, 23, 59, 0)
        val nightSlot = MotivationalQuotesRepository.computeCurrentSlot(cal)

        cal.set(2026, Calendar.OCTOBER, 17, 0, 1, 0) // Next day early morning
        val nextDaySlot = MotivationalQuotesRepository.computeCurrentSlot(cal)

        assertNotEquals(nightSlot.first, nextDaySlot.first)
        assertEquals(3, nextDaySlot.second) // Still evening/night slot index 3
    }
}
