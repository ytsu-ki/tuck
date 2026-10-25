package com.example.starbox.capture

import android.app.Notification
import java.security.MessageDigest

/** The fields StarBox reads out of another app's notification. */
data class ParsedNotification(
    /** StatusBarNotification key of the source notification. */
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String,
    val sender: String?,
    /** Group or conversation name when it differs from the sender. */
    val conversation: String?,
    val postedAt: Long,
    val dedupKey: String,
    /**
     * Other text on the notification that keywords also search, such as its ticker line
     * ("Alice: see you at 5"), for apps that leave the main text short or generic.
     */
    val extraText: String? = null,
)

/**
 * One post of a notification: the entry shown in Recently seen and the messages to check
 * against rules. A chat notification can bring several new messages at once.
 */
data class ParsedPost(
    /** The notification as it reads now (its newest message from someone else). */
    val latest: ParsedNotification,
    /** Messages not checked yet, oldest first. For other notifications just [latest]. */
    val toCheck: List<ParsedNotification>,
    /** [CaptureFilter.lineId]s of a chat notification's messages, so its next update only adds what's new. */
    val lineIds: Set<String>? = null,
)

/** One message inside a chat (MessagingStyle) notification, free of Android types for tests. */
data class ChatLine(val text: String, val timestamp: Long, val sender: String?, val fromSelf: Boolean)

/** Pure decisions about which notifications to read, kept free of Android calls so they can be unit tested. */
object CaptureFilter {
    private const val IGNORED_FLAGS =
        Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_FOREGROUND_SERVICE or
            Notification.FLAG_GROUP_SUMMARY

    /** On first sight of a chat notification, its messages from this long before the post are new. */
    const val FIRST_SIGHT_WINDOW_MS = 60 * 60 * 1000L

    /** A cap so a long history can't flood the inbox and alerts in one go. */
    const val MAX_MESSAGES_PER_POST = 10

    /**
     * Skips our own alerts (to avoid loops), ongoing/foreground notifications (music, downloads,
     * navigation) and group summaries, which only repeat the child notifications.
     */
    fun shouldIgnore(packageName: String, ownPackage: String, flags: Int): Boolean =
        packageName == ownPackage || flags and IGNORED_FLAGS != 0

    /**
     * Identity of one message. Messaging apps repost the whole conversation each time a new
     * message arrives, so for MessagingStyle we key on the individual message's own timestamp.
     * Otherwise we key on the notification slot, its content and its time ([notificationWhen]):
     * a repost keeps its time, while the same words sent again later ("ok") are a new message.
     */
    fun dedupKey(
        packageName: String,
        notificationKey: String,
        conversation: String?,
        sender: String?,
        text: String,
        messageTimestamp: Long?,
        notificationWhen: Long? = null,
    ): String {
        val raw = if (messageTimestamp != null) {
            listOf(packageName, conversation.orEmpty(), sender.orEmpty(), text, messageTimestamp.toString())
        } else {
            listOf(packageName, notificationKey, sender.orEmpty(), text, notificationWhen?.toString().orEmpty())
        }.joinToString("\u001F")
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /** Identity of one message inside its chat notification. */
    fun lineId(line: ChatLine): String =
        listOf(line.timestamp.toString(), line.sender.orEmpty(), line.fromSelf.toString(), line.text).joinToString("\u001F")

    /**
     * Which messages of a chat notification go through the rules. Apps often deliver several
     * at once (a burst, or messages the phone held back) and a keyword can be in any of them,
     * not only the newest. The user's own replies never count.
     *
     * [seenBefore] are the [lineId]s of this notification's previous update. Chat apps append
     * new messages at the end, so whatever follows the last message seen before is new. That
     * holds whatever the times say: some apps send no time, and a sender's clock can be off.
     * On first sight (or when everything seen before has scrolled out), the unread tail
     * counts: messages after the user's last reply from the last [FIRST_SIGHT_WINDOW_MS] before
     * [postedAt], or at least the newest one, so a clock that's off can't hide a message.
     */
    fun messagesToCheck(lines: List<ChatLine>, seenBefore: Set<String>?, postedAt: Long): List<ChatLine> {
        val lastSeen = if (seenBefore == null) -1 else lines.indexOfLast { lineId(it) in seenBefore }
        val fresh = if (lastSeen >= 0) {
            lines.drop(lastSeen + 1).filterNot { it.fromSelf }
        } else {
            val unread = lines.drop(lines.indexOfLast { it.fromSelf } + 1)
            unread.filter { it.timestamp >= postedAt - FIRST_SIGHT_WINDOW_MS }.ifEmpty { listOfNotNull(unread.lastOrNull()) }
        }
        return fresh.takeLast(MAX_MESSAGES_PER_POST)
    }
}
