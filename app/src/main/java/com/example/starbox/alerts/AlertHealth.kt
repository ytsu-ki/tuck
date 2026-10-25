package com.example.starbox.alerts

import android.app.NotificationManager
import android.media.AudioManager
import com.example.starbox.data.model.AlertProfile

/** Something on the phone that keeps a StarBox alert from sounding, vibrating or popping up. */
enum class AlertProblem(val fixedInNotificationSettings: Boolean) {
    NotificationsOff(true),
    ChannelBlocked(true),
    BannerOff(true),
    SoundOff(true),
    VibrationOff(true),
    DoNotDisturb(false),
    PhoneSilent(false),
    PhoneVibrateOnly(false),
    VolumeZero(false),
}

/** What the system actually did with our alert channel (users and phone makers can change it). */
data class ChannelSnapshot(val importance: Int, val hasSound: Boolean, val vibrates: Boolean)

data class AlertHealth(val problems: List<AlertProblem>) {
    val ok: Boolean get() = problems.isEmpty()

    /** Problems in StarBox's own notification settings, as opposed to phone-wide sound modes. */
    val settingsProblems: List<AlertProblem> get() = problems.filter { it.fixedInNotificationSettings }
    val phoneProblems: List<AlertProblem> get() = problems.filterNot { it.fixedInNotificationSettings }

    companion object {
        val OK = AlertHealth(emptyList())
    }
}

/** Compares the alert we asked for with what the phone will really do. Pure, so it is unit tested. */
object AlertHealthCheck {

    fun evaluate(
        wanted: AlertProfile,
        notificationsEnabled: Boolean,
        channel: ChannelSnapshot?,
        doNotDisturb: Boolean,
        ringerMode: Int,
        notificationVolume: Int,
    ): AlertHealth {
        if (!notificationsEnabled) return AlertHealth(listOf(AlertProblem.NotificationsOff))
        // A silent profile only collects messages, so there is nothing to check.
        if (wanted.isSilent) return AlertHealth.OK

        val problems = mutableListOf<AlertProblem>()
        if (channel != null) {
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                problems += AlertProblem.ChannelBlocked
            } else {
                // Below "default" importance a channel neither sounds nor vibrates; below "high" it doesn't pop up.
                val audible = channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
                if (wanted.headsUp && channel.importance < NotificationManager.IMPORTANCE_HIGH) problems += AlertProblem.BannerOff
                if (wanted.sound && (!audible || !channel.hasSound)) problems += AlertProblem.SoundOff
                if (wanted.vibrate && (!audible || !channel.vibrates)) problems += AlertProblem.VibrationOff
            }
        }
        if (doNotDisturb) problems += AlertProblem.DoNotDisturb
        when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> if (wanted.sound || wanted.vibrate) problems += AlertProblem.PhoneSilent
            AudioManager.RINGER_MODE_VIBRATE -> if (wanted.sound) problems += AlertProblem.PhoneVibrateOnly
            else -> if (wanted.sound && notificationVolume == 0) problems += AlertProblem.VolumeZero
        }
        return AlertHealth(problems)
    }
}
