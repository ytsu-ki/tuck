package com.example.starbox.capture

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFilterTest {
    private val own = "com.example.starbox"

    @Test
    fun ignoresOwnNotifications() {
        assertTrue(CaptureFilter.shouldIgnore(own, own, 0))
    }

    @Test
    fun ignoresOngoingForegroundAndGroupSummaries() {
        assertTrue(CaptureFilter.shouldIgnore("a", own, Notification.FLAG_ONGOING_EVENT))
        assertTrue(CaptureFilter.shouldIgnore("a", own, Notification.FLAG_FOREGROUND_SERVICE))
        assertTrue(CaptureFilter.shouldIgnore("a", own, Notification.FLAG_GROUP_SUMMARY))
    }

    @Test
    fun keepsOrdinaryNotifications() {
        assertFalse(CaptureFilter.shouldIgnore("com.tencent.mm", own, Notification.FLAG_AUTO_CANCEL))
    }

    @Test
    fun messagingRepostOfSameMessageHasSameKey() {
        // WeChat/Telegram repost the conversation under the same notification key each time;
        // the same message must keep its identity.
        val first = CaptureFilter.dedupKey("com.tencent.mm", "k1", "Team", "Alice", "hi", 1_000L)
        val repost = CaptureFilter.dedupKey("com.tencent.mm", "k2", "Team", "Alice", "hi", 1_000L)
        assertEquals(first, repost)
    }

    @Test
    fun sameTextAtDifferentTimesIsDistinct() {
        val a = CaptureFilter.dedupKey("com.tencent.mm", "k", null, "Alice", "ok", 1_000L)
        val b = CaptureFilter.dedupKey("com.tencent.mm", "k", null, "Alice", "ok", 2_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun nonMessagingKeyedOnSlotAndContent() {
        val a = CaptureFilter.dedupKey("com.twitter.android", "slot1", null, "@nasa", "Launch!", null, 1_000L)
        val same = CaptureFilter.dedupKey("com.twitter.android", "slot1", null, "@nasa", "Launch!", null, 1_000L)
        val otherSlot = CaptureFilter.dedupKey("com.twitter.android", "slot2", null, "@nasa", "Launch!", null, 1_000L)
        assertEquals(a, same)
        assertNotEquals(a, otherSlot)
    }

    @Test
    fun sameWordsSentAgainLaterAreANewMessage() {
        // WeChat: "ok" from Alice today and "ok" again tomorrow, same chat slot.
        val first = CaptureFilter.dedupKey("com.tencent.mm", "chat", null, "Alice", "ok", null, 1_000L)
        val again = CaptureFilter.dedupKey("com.tencent.mm", "chat", null, "Alice", "ok", null, 90_000_000L)
        assertNotEquals(first, again)
    }

    private fun line(text: String, at: Long, self: Boolean = false) = ChatLine(text, at, if (self) null else "Alice", self)

    private fun seen(vararg lines: ChatLine) = lines.map(CaptureFilter::lineId).toSet()

    private fun check(lines: List<ChatLine>, seenBefore: Set<String>?, postedAt: Long = 10_000) =
        CaptureFilter.messagesToCheck(lines, seenBefore, postedAt).map { it.text }

    @Test
    fun checksEveryNewMessageInOneUpdate() {
        val old = line("old", 1_000)
        val lines = listOf(old, line("urgent: server down", 5_000), line("please check", 6_000))
        // "old" was in the last update: both new messages go through the rules, not only the last.
        assertEquals(listOf("urgent: server down", "please check"), check(lines, seen(old)))
        // Nothing new (a repost): nothing to check.
        assertTrue(check(lines, seen(*lines.toTypedArray())).isEmpty())
    }

    @Test
    fun newMessagesCountWhateverTheirTime() {
        // A sender whose clock runs an hour ahead, then someone with a correct clock.
        val ahead = line("from the future", 3_600_000)
        assertEquals(listOf("on time"), check(listOf(ahead, line("on time", 5_000)), seen(ahead)))
        // An app that sends no time at all.
        val first = line("first", 0)
        assertEquals(listOf("second"), check(listOf(first, line("second", 0)), seen(first)))
    }

    @Test
    fun neverChecksOwnReplies() {
        val question = line("urgent?", 1_000)
        val reply = line("on it, urgent", 2_000, self = true)
        assertTrue(check(listOf(question, reply), seen(question)).isEmpty())
        // First sight: only what came after the user's last reply.
        assertEquals(listOf("thanks"), check(listOf(question, reply, line("thanks", 3_000)), seenBefore = null, postedAt = 3_000))
    }

    @Test
    fun historyScrolledOutFallsBackToFirstSight() {
        // Everything seen before has scrolled out of the notification: check the recent tail.
        val gone = seen(line("long gone", 1))
        assertEquals(listOf("a", "b"), check(listOf(line("a", 9_000), line("b", 9_500)), gone))
    }

    @Test
    fun firstSightSkipsOldHistoryButKeepsTheNewest() {
        val hour = CaptureFilter.FIRST_SIGHT_WINDOW_MS
        val now = 10 * hour
        val lines = listOf(line("yesterday", now - 20 * hour), line("a", now - 1_000), line("b", now))
        assertEquals(listOf("a", "b"), check(lines, seenBefore = null, postedAt = now))
        // Sender's clock far behind: the newest message is still checked.
        val skewed = listOf(line("x", now - 5 * hour), line("y", now - 4 * hour))
        assertEquals(listOf("y"), check(skewed, seenBefore = null, postedAt = now))
    }

    @Test
    fun capsMessagesPerUpdate() {
        val lines = (1..30).map { line("m$it", it * 1_000L) }
        val toCheck = CaptureFilter.messagesToCheck(lines, seen(lines.first()), postedAt = 30_000)
        assertEquals(CaptureFilter.MAX_MESSAGES_PER_POST, toCheck.size)
        assertEquals("m30", toCheck.last().text)
    }
}
