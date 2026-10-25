package com.example.starbox.capture

import android.app.PendingIntent

/**
 * Tap actions of source notifications that are still in the shade, keyed by notification key.
 * A PendingIntent can't be stored on disk, so "Open in WeChat" jumps straight to the chat only
 * while the original notification exists; otherwise we fall back to launching the app.
 */
class LiveSourceIntents(private val capacity: Int = 200) {
    private val intents = object : LinkedHashMap<String, PendingIntent>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PendingIntent>) = size > capacity
    }

    @Synchronized
    fun put(key: String, intent: PendingIntent) {
        intents[key] = intent
    }

    @Synchronized
    fun get(key: String): PendingIntent? = intents[key]

    @Synchronized
    fun remove(key: String) {
        intents.remove(key)
    }
}
