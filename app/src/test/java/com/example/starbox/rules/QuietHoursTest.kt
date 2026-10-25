package com.example.starbox.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun disabledIsNeverActive() {
        assertFalse(QuietHours(enabled = false).isActive(at(2)))
    }

    @Test
    fun windowAcrossMidnight() {
        val night = QuietHours(enabled = true, startMinute = at(23), endMinute = at(7))
        assertTrue(night.isActive(at(23)))
        assertTrue(night.isActive(at(2, 30)))
        assertTrue(night.isActive(at(6, 59)))
        assertFalse(night.isActive(at(7)))
        assertFalse(night.isActive(at(12)))
        assertFalse(night.isActive(at(22, 59)))
    }

    @Test
    fun windowWithinOneDay() {
        val lunch = QuietHours(enabled = true, startMinute = at(12), endMinute = at(13, 30))
        assertTrue(lunch.isActive(at(12)))
        assertTrue(lunch.isActive(at(13, 29)))
        assertFalse(lunch.isActive(at(13, 30)))
        assertFalse(lunch.isActive(at(11, 59)))
    }

    @Test
    fun zeroLengthWindowIsNeverActive() {
        assertFalse(QuietHours(enabled = true, startMinute = at(8), endMinute = at(8)).isActive(at(8)))
    }
}
