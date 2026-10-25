package com.example.starbox.rules

/**
 * A daily window in which StarBox's own alerts stay silent (messages are still collected).
 * Times are minutes after midnight; a window may cross midnight, e.g. 23:00–07:00.
 */
data class QuietHours(
    val enabled: Boolean = false,
    val startMinute: Int = 23 * 60,
    val endMinute: Int = 7 * 60,
) {
    /** A zero-length window (start == end) is never active. */
    fun isActive(minuteOfDay: Int): Boolean {
        if (!enabled || startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            minuteOfDay in startMinute until endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }
}
