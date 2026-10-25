package com.example.starbox.rules

import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPolicyTest {
    private val default = AlertProfile(sound = true, soundUri = null, vibrate = true, headsUp = false)

    private fun rule(alert: AlertProfile? = null) =
        Rule(name = "r", senders = listOf("x"), useDefaultAlert = alert == null, alert = alert ?: AlertProfile())

    @Test
    fun noMatchesIsSilent() {
        assertTrue(AlertPolicy.effective(emptyList(), default).isSilent)
    }

    @Test
    fun defaultRuleUsesGlobalDefault() {
        assertEquals(default, AlertPolicy.effective(listOf(rule()), default))
    }

    @Test
    fun silentRuleStaysSilent() {
        assertTrue(AlertPolicy.effective(listOf(rule(AlertPolicy.SILENT)), default).isSilent)
    }

    @Test
    fun strongestRuleWins() {
        val bannerOnly = AlertProfile(sound = false, soundUri = null, vibrate = false, headsUp = true)
        val customSound = AlertProfile(sound = true, soundUri = "content://ding", vibrate = false, headsUp = false)
        val merged = AlertPolicy.effective(listOf(rule(bannerOnly), rule(customSound), rule()), default)
        assertEquals(AlertProfile(sound = true, soundUri = "content://ding", vibrate = true, headsUp = true), merged)
    }

    @Test
    fun quietHoursSilenceOrdinaryRules() {
        assertTrue(AlertPolicy.effective(listOf(rule()), default, quietNow = true).isSilent)
    }

    @Test
    fun urgentRuleBreaksThroughQuietHours() {
        val urgent = rule().copy(bypassQuietHours = true)
        val bannerOnly = AlertProfile(sound = false, soundUri = null, vibrate = false, headsUp = true)
        // Only the urgent rule's alert counts; the ordinary rule's banner is dropped.
        assertEquals(default, AlertPolicy.effective(listOf(rule(bannerOnly), urgent), default, quietNow = true))
    }

    @Test
    fun channelIdChangesWithSettings() {
        val a = AlertProfile(sound = true, soundUri = null, vibrate = true, headsUp = true)
        assertEquals("alert_g1_svh_default", AlertPolicy.channelId(a, generation = 1))
        assertNotEquals(AlertPolicy.channelId(a, 1), AlertPolicy.channelId(a.copy(soundUri = "content://ding"), 1))
        // A sound URI left over from before sound was switched off doesn't create a new channel.
        assertEquals("alert_g1_-vh_none", AlertPolicy.channelId(a.copy(sound = false, soundUri = "content://ding"), 1))
        // Resetting alerts moves every profile to a fresh channel.
        assertNotEquals(AlertPolicy.channelId(a, 1), AlertPolicy.channelId(a, 2))
    }
}
