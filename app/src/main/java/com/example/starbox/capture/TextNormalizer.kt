package com.example.starbox.capture

/**
 * Cleans up notification text from chat apps that don't use MessagingStyle.
 *
 * WeChat and QQ post plain notifications shaped like:
 *   title = "Project group",  text = "[3条]Alice: see you at 5"
 * We drop the unread counter and, in group chats, split the "Alice:" prefix into the sender
 * so rules can match the person rather than only the group name.
 */
object TextNormalizer {
    data class Result(val sender: String?, val conversation: String?, val text: String)

    private val SENDER_PREFIX_APPS = setOf(
        "com.tencent.mm", // WeChat
        "com.tencent.mobileqq", // QQ
        "com.tencent.tim", // TIM
        "com.tencent.wework", // WeCom
    )

    /** "[3条]", "[3條]", "[3]", "[3 messages]" at the start of the text. */
    private val UNREAD_PREFIX = Regex("""^\[\d+\s*(条|條|messages?)?]\s*""", RegexOption.IGNORE_CASE)

    /** "Name: message" with an ASCII or full-width colon; not a URL scheme such as "https://". */
    private val SENDER_PREFIX = Regex("""^([^\n:：]{1,32})[:：](?!//)\s*(.+)$""", RegexOption.DOT_MATCHES_ALL)

    private val DIGITS_ONLY = Regex("""^\d+$""")

    fun normalize(packageName: String, title: String?, text: String): Result {
        val body = text.trim()
        if (packageName !in SENDER_PREFIX_APPS) return Result(sender = title, conversation = null, text = body)

        val withoutCounter = body.replace(UNREAD_PREFIX, "")
        val match = SENDER_PREFIX.find(withoutCounter)
        val prefix = match?.groupValues?.get(1)?.trim()
        if (match == null || prefix.isNullOrEmpty() || DIGITS_ONLY.matches(prefix)) {
            return Result(sender = title, conversation = null, text = withoutCounter)
        }
        val message = match.groupValues[2].trim()
        return if (title == null || prefix == title) {
            // Private chat that repeats the contact name: "Alice: hi" under title "Alice".
            Result(sender = prefix, conversation = null, text = message)
        } else {
            // Group chat: the title is the group, the prefix is the person.
            Result(sender = prefix, conversation = title, text = message)
        }
    }
}
