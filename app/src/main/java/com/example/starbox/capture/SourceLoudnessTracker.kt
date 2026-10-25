package com.example.starbox.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The importance Android gave each source app's most recent notification, as reported by the
 * listener's ranking. Shows whether e.g. WeChat is still ringing on its own (so the user hears
 * every starred message twice) or has been set to Silent. Memory only, like Recently seen.
 */
class SourceLoudnessTracker {
    /**
     * [channelId] is the notification category it came in and [uid] the app's user id (which
     * differs for a cloned "dual" app), so "Make quiet" can open exactly that app's settings.
     * [sound] and [vibrates] are that category's own switches.
     */
    data class Sample(
        val importance: Int,
        val seenAt: Long,
        val channelId: String? = null,
        val uid: Int? = null,
        val sound: Boolean = true,
        val vibrates: Boolean = true,
    )

    private val _samples = MutableStateFlow<Map<String, Sample>>(emptyMap())
    val samples: StateFlow<Map<String, Sample>> = _samples.asStateFlow()

    fun record(
        packageName: String,
        importance: Int,
        channelId: String? = null,
        uid: Int? = null,
        sound: Boolean = true,
        vibrates: Boolean = true,
        seenAt: Long = System.currentTimeMillis(),
    ) {
        _samples.update { it + (packageName to Sample(importance, seenAt, channelId, uid, sound, vibrates)) }
    }
}
