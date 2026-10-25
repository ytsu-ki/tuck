package com.example.starbox.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentNotificationsTest {
    private fun item(key: String, text: String = key) = ParsedNotification(
        key = key,
        packageName = "p",
        appLabel = "App",
        title = null,
        text = text,
        sender = null,
        conversation = null,
        postedAt = 0L,
        dedupKey = key,
    )

    @Test
    fun newestFirstAndCapped() {
        val recent = RecentNotifications(capacity = 2)
        recent.add(item("a"))
        recent.add(item("b"))
        recent.add(item("c"))
        assertEquals(listOf("c", "b"), recent.items.value.map { it.key })
    }

    @Test
    fun repostMovesToTopWithoutDuplicating() {
        val recent = RecentNotifications()
        recent.add(item("a"))
        recent.add(item("b"))
        recent.add(item("a", text = "updated"))
        assertEquals(listOf("a", "b"), recent.items.value.map { it.key })
        assertEquals("updated", recent.items.value.first().text)
    }
}
