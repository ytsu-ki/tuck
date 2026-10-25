package com.example.starbox.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderErrorTest {
    @Test
    fun summarizesTypeMessageAndWhere() {
        val error = NullPointerException("Attempt to invoke virtual method on a null object reference").apply {
            stackTrace = arrayOf(StackTraceElement("androidx.core.app.Person", "fromBundle", "Person.java", 92))
        }
        val recorded = ReaderError.from(error, at = 42L)
        assertEquals(42L, recorded.at)
        assertEquals(
            "NullPointerException: Attempt to invoke virtual method on a null object reference (Person.fromBundle)",
            recorded.summary,
        )
    }

    @Test
    fun keepsLongMessagesShort() {
        val recorded = ReaderError.from(IllegalStateException("x".repeat(1_000)).apply { stackTrace = emptyArray() })
        assertTrue(recorded.summary.length < 200)
        assertEquals("IllegalStateException: " + "x".repeat(160), recorded.summary)
    }
}
