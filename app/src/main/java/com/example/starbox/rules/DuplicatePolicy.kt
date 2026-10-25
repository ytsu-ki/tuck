package com.example.starbox.rules

import android.app.NotificationManager
import com.example.starbox.data.model.Rule

/** How a source app's notification will behave next to Tuck's own alert. */
enum class SourceLoudness { Unknown, Quiet, Loud }

/** Pure rules for avoiding duplicate alerts; unit tested. */
object DuplicatePolicy {

    /**
     * Dismiss the original only when a matched rule asks for it AND Tuck actually posted its own
     * alert. If Tuck stayed silent (silent rule, quiet hours, notifications off), the original is
     * kept so a starred message can never vanish without the user seeing it anywhere.
     */
    fun shouldDismissOriginal(matched: List<Rule>, tuckAlertPosted: Boolean): Boolean =
        tuckAlertPosted && matched.any { it.dismissOriginal }

    /**
     * Below "default" importance a channel neither sounds nor vibrates, so the source app is
     * quiet and only Tuck's alert will be heard. So is a channel whose [sound] and [vibrates]
     * were both turned off, which is how some phones' app-wide Ringtone/Vibrate switches work.
     * [importance] is null when nothing was seen yet.
     */
    fun loudness(importance: Int?, sound: Boolean = true, vibrates: Boolean = true): SourceLoudness = when {
        importance == null || importance == NotificationManager.IMPORTANCE_UNSPECIFIED -> SourceLoudness.Unknown
        importance < NotificationManager.IMPORTANCE_DEFAULT -> SourceLoudness.Quiet
        !sound && !vibrates -> SourceLoudness.Quiet
        else -> SourceLoudness.Loud
    }
}
