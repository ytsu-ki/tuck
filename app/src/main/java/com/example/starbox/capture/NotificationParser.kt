package com.example.starbox.capture

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat

/** Reads a [StatusBarNotification] into a [ParsedPost]. Never modifies the notification. */
class NotificationParser(
    private val labelOf: (packageName: String) -> String,
    private val ownPackage: String,
) {
    /**
     * [seenLines] are the chat messages this notification showed last time (see
     * [CaptureFilter.messagesToCheck]); null the first time it is seen.
     */
    fun parse(sbn: StatusBarNotification, seenLines: Set<String>? = null): ParsedPost? {
        val notification = sbn.notification ?: return null
        if (CaptureFilter.shouldIgnore(sbn.packageName, ownPackage, notification.flags)) return null

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()?.ifEmpty { null }
        val lines = chatLines(extras)
        if (lines.isNotEmpty()) {
            val conversationTitle = extras.nonBlank(Notification.EXTRA_CONVERSATION_TITLE)
                ?: extras.nonBlank(EXTRA_HIDDEN_CONVERSATION_TITLE)
            fun entry(line: ChatLine): ParsedNotification? {
                if (line.text.isEmpty() && title == null) return null
                val sender = line.sender ?: title
                val conversation = conversationTitle?.takeIf { it != sender }
                // Some apps send no time per message; the notification's own time stands in, so
                // the same words sent again later still count as a new message.
                val time = line.timestamp.takeIf { it > 0 } ?: notification.`when`.takeIf { it > 0 } ?: sbn.postTime
                return ParsedNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = labelOf(sbn.packageName),
                    title = title,
                    text = line.text,
                    sender = sender,
                    conversation = conversation,
                    postedAt = time,
                    dedupKey = CaptureFilter.dedupKey(
                        packageName = sbn.packageName,
                        notificationKey = sbn.key,
                        conversation = conversation,
                        sender = sender,
                        text = line.text,
                        messageTimestamp = time,
                    ),
                )
            }
            val toCheck = CaptureFilter.messagesToCheck(lines, seenLines, sbn.postTime).mapNotNull(::entry)
            // A newest message without text (a photo) mustn't hide the new ones that have some.
            val latest = entry(lines.lastOrNull { !it.fromSelf } ?: lines.last()) ?: toCheck.lastOrNull() ?: return null
            return ParsedPost(latest = latest, toCheck = toCheck, lineIds = lines.map(CaptureFilter::lineId).toSet())
        }

        val ticker = notification.tickerText?.toString()?.trim()?.ifEmpty { null }
        // Big text first (it's the full message); an empty one mustn't hide the normal text.
        val body = extras.nonBlank(Notification.EXTRA_BIG_TEXT)
            ?: extras.nonBlank(Notification.EXTRA_TEXT)
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim()?.ifEmpty { null } }
                ?.joinToString("\n")
                ?.ifEmpty { null }
            ?: ticker
            ?: ""
        val normalized = TextNormalizer.normalize(sbn.packageName, title, body)
        val text = normalized.text
        val sender = normalized.sender
        val conversation = normalized.conversation
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.takeIf { it != sender }

        if (text.isEmpty() && title == null) return null

        val parsed = ParsedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = labelOf(sbn.packageName),
            title = title,
            text = text,
            sender = sender,
            conversation = conversation,
            postedAt = sbn.postTime,
            dedupKey = CaptureFilter.dedupKey(
                packageName = sbn.packageName,
                notificationKey = sbn.key,
                conversation = conversation,
                sender = sender,
                text = text,
                messageTimestamp = null,
                notificationWhen = notification.`when`,
            ),
            extraText = ticker?.takeIf { it != body },
        )
        return ParsedPost(latest = parsed, toCheck = listOf(parsed))
    }

    /**
     * The messages of a chat (MessagingStyle) notification, read straight from its extras.
     * Not via AndroidX's MessagingStyle reader: it throws on notifications built with
     * Android's own MessagingStyle (it expects its own format for the user's Person), and that
     * crash took the whole reader down. Android and AndroidX write these per-message keys alike.
     */
    private fun chatLines(extras: Bundle): List<ChatLine> {
        val messages = BundleCompat.getParcelableArray(extras, Notification.EXTRA_MESSAGES, Bundle::class.java)
            ?: return emptyList()
        return messages.filterIsInstance<Bundle>().map { message ->
            ChatLine(
                text = message.getCharSequence(KEY_TEXT)?.toString()?.trim().orEmpty(),
                timestamp = message.getLong(KEY_TIME),
                sender = message.getCharSequence(KEY_SENDER)?.toString()?.trim()?.ifEmpty { null },
                // MessagingStyle leaves the sender out of the user's own messages.
                fromSelf = SENDER_KEYS.none(message::containsKey),
            )
        }
    }

    private fun Bundle.nonBlank(key: String): String? = getCharSequence(key)?.toString()?.trim()?.ifEmpty { null }

    private companion object {
        /** Set for one-to-one chats, where Android hides the title but it's still the chat's name. */
        const val EXTRA_HIDDEN_CONVERSATION_TITLE = "android.hiddenConversationTitle"
        const val KEY_TEXT = "text"
        const val KEY_TIME = "time"
        const val KEY_SENDER = "sender"
        val SENDER_KEYS = listOf(KEY_SENDER, "sender_person", "person")
    }
}
