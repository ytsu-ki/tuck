package com.example.starbox.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The last few notifications the listener saw, newest first. Kept in memory only and never
 * written to disk; it backs the diagnostics list and, later, the rule-editor preview.
 */
class RecentNotifications(private val capacity: Int = 50) {
    private val _items = MutableStateFlow<List<ParsedNotification>>(emptyList())
    val items: StateFlow<List<ParsedNotification>> = _items.asStateFlow()

    fun add(notification: ParsedNotification) {
        _items.update { current ->
            (listOf(notification) + current.filterNot { it.dedupKey == notification.dedupKey })
                .take(capacity)
        }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
