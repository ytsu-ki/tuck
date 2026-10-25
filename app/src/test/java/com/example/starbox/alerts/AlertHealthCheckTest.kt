package com.example.starbox.alerts

import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.NotificationManager.IMPORTANCE_NONE
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import com.example.starbox.alerts.AlertProblem.BannerOff
import com.example.starbox.alerts.AlertProblem.ChannelBlocked
import com.example.starbox.alerts.AlertProblem.DoNotDisturb
import com.example.starbox.alerts.AlertProblem.NotificationsOff
import com.example.starbox.alerts.AlertProblem.PhoneSilent
import com.example.starbox.alerts.AlertProblem.PhoneVibrateOnly
import com.example.starbox.alerts.AlertProblem.SoundOff
import com.example.starbox.alerts.AlertProblem.VibrationOff
import com.example.starbox.alerts.AlertProblem.VolumeZero
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.rules.AlertPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertHealthCheckTest {
    private val everything = AlertProfile(sound = true, soundUri = null, vibrate = true, headsUp = true)
    private val healthyChannel = ChannelSnapshot(IMPORTANCE_HIGH, hasSound = true, vibrates = true)

    private fun check(
        wanted: AlertProfile = everything,
        enabled: Boolean = true,
        channel: ChannelSnapshot? = healthyChannel,
        dnd: Boolean = false,
        ringer: Int = RINGER_MODE_NORMAL,
        volume: Int = 5,
    ) = AlertHealthCheck.evaluate(wanted, enabled, channel, dnd, ringer, volume).problems

    @Test
    fun healthyChannelHasNoProblems() {
        assertTrue(check().isEmpty())
    }

    @Test
    fun notificationsOffHidesEverythingElse() {
        assertEquals(listOf(NotificationsOff), check(enabled = false, dnd = true, ringer = RINGER_MODE_SILENT))
    }

    @Test
    fun blockedChannel() {
        assertEquals(listOf(ChannelBlocked), check(channel = healthyChannel.copy(importance = IMPORTANCE_NONE)))
    }

    @Test
    fun mutedOrLoweredChannelIsReportedPerFeature() {
        // What a phone maker's "no banners, no sound" default looks like.
        assertEquals(
            listOf(BannerOff, SoundOff, VibrationOff),
            check(channel = ChannelSnapshot(IMPORTANCE_LOW, hasSound = true, vibrates = true)),
        )
        assertEquals(listOf(BannerOff), check(channel = healthyChannel.copy(importance = IMPORTANCE_DEFAULT)))
        assertEquals(listOf(SoundOff), check(channel = healthyChannel.copy(hasSound = false)))
        assertEquals(listOf(VibrationOff), check(channel = healthyChannel.copy(vibrates = false)))
    }

    @Test
    fun onlyRequestedFeaturesAreChecked() {
        val bannerOnly = AlertProfile(sound = false, soundUri = null, vibrate = false, headsUp = true)
        assertTrue(check(wanted = bannerOnly, channel = ChannelSnapshot(IMPORTANCE_HIGH, hasSound = false, vibrates = false)).isEmpty())
        assertTrue(check(wanted = AlertPolicy.SILENT, channel = healthyChannel.copy(importance = IMPORTANCE_LOW)).isEmpty())
    }

    @Test
    fun phoneWideSoundModes() {
        assertEquals(listOf(DoNotDisturb), check(dnd = true))
        assertEquals(listOf(PhoneSilent), check(ringer = RINGER_MODE_SILENT))
        assertEquals(listOf(PhoneVibrateOnly), check(ringer = RINGER_MODE_VIBRATE))
        assertEquals(listOf(VolumeZero), check(volume = 0))
        // Vibrate mode is fine for a vibrate-only alert.
        val vibrateOnly = AlertProfile(sound = false, soundUri = null, vibrate = true, headsUp = false)
        assertTrue(check(wanted = vibrateOnly, channel = healthyChannel.copy(importance = IMPORTANCE_DEFAULT), ringer = RINGER_MODE_VIBRATE).isEmpty())
    }
}
