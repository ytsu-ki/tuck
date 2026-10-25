package com.example.starbox.rules

import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.Rule
import java.security.MessageDigest

object AlertPolicy {
    val SILENT = AlertProfile(sound = false, soundUri = null, vibrate = false, headsUp = false)

    /**
     * When several rules star the same message we alert once, as strongly as the strongest rule:
     * each of sound, vibration and banner is on if any matched rule wants it. The custom sound
     * comes from the first matched rule (in rule order) that plays one.
     *
     * During quiet hours only rules marked [Rule.bypassQuietHours] count; the rest stay silent.
     */
    fun effective(matched: List<Rule>, default: AlertProfile, quietNow: Boolean = false): AlertProfile {
        val audible = if (quietNow) matched.filter { it.bypassQuietHours } else matched
        val profiles = audible.map { if (it.useDefaultAlert) default else it.alert }
        if (profiles.isEmpty()) return SILENT
        val withSound = profiles.filter { it.sound }
        return AlertProfile(
            sound = withSound.isNotEmpty(),
            soundUri = withSound.firstNotNullOfOrNull { it.soundUri },
            vibrate = profiles.any { it.vibrate },
            headsUp = profiles.any { it.headsUp },
        )
    }

    /**
     * Android freezes a channel's sound, vibration and importance once created, so every
     * distinct alert profile gets its own channel ID derived from its settings. [generation]
     * changes when the user resets alerts: a brand-new ID is the only way to get a channel back
     * to its original settings after it was muted.
     */
    fun channelId(profile: AlertProfile, generation: Int): String {
        val flags = buildString {
            append(if (profile.sound) 's' else '-')
            append(if (profile.vibrate) 'v' else '-')
            append(if (profile.headsUp) 'h' else '-')
        }
        val sound = if (profile.sound) profile.soundUri?.let(::shortHash) ?: "default" else "none"
        return "${CHANNEL_PREFIX}g${generation}_${flags}_$sound"
    }

    const val CHANNEL_PREFIX = "alert_"

    private fun shortHash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .take(6)
            .joinToString("") { "%02x".format(it) }
}
