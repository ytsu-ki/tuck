package com.example.starbox.rules

import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.NotificationManager.IMPORTANCE_MIN
import android.app.NotificationManager.IMPORTANCE_UNSPECIFIED
import com.example.starbox.data.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatePolicyTest {
    private fun rule(tidy: Boolean) = Rule(name = "r", senders = listOf("x"), dismissOriginal = tidy)

    @Test
    fun dismissesOnlyWhenAskedAndTuckAlerted() {
        assertTrue(DuplicatePolicy.shouldDismissOriginal(listOf(rule(false), rule(true)), tuckAlertPosted = true))
        assertFalse(DuplicatePolicy.shouldDismissOriginal(listOf(rule(false)), tuckAlertPosted = true))
        // Tuck stayed silent: keep the original so the message is still visible somewhere.
        assertFalse(DuplicatePolicy.shouldDismissOriginal(listOf(rule(true)), tuckAlertPosted = false))
    }

    @Test
    fun classifiesSourceLoudness() {
        assertEquals(SourceLoudness.Unknown, DuplicatePolicy.loudness(null))
        assertEquals(SourceLoudness.Unknown, DuplicatePolicy.loudness(IMPORTANCE_UNSPECIFIED))
        assertEquals(SourceLoudness.Quiet, DuplicatePolicy.loudness(IMPORTANCE_LOW))
        assertEquals(SourceLoudness.Quiet, DuplicatePolicy.loudness(IMPORTANCE_MIN))
        assertEquals(SourceLoudness.Loud, DuplicatePolicy.loudness(IMPORTANCE_DEFAULT))
        assertEquals(SourceLoudness.Loud, DuplicatePolicy.loudness(IMPORTANCE_HIGH))
        // Sound and vibration both off counts as quiet; either one still on still rings.
        assertEquals(SourceLoudness.Quiet, DuplicatePolicy.loudness(IMPORTANCE_HIGH, sound = false, vibrates = false))
        assertEquals(SourceLoudness.Loud, DuplicatePolicy.loudness(IMPORTANCE_HIGH, sound = false, vibrates = true))
        assertEquals(SourceLoudness.Loud, DuplicatePolicy.loudness(IMPORTANCE_DEFAULT, sound = true, vibrates = false))
        assertEquals(SourceLoudness.Unknown, DuplicatePolicy.loudness(null, sound = false, vibrates = false))
    }
}
