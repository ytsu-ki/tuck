package com.example.starbox.capture

import com.example.starbox.data.db.MessageDao
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Rule
import com.example.starbox.rules.RuleEngine
import com.example.starbox.rules.toMatchInput

/** A newly starred message and the rules that starred it. */
data class CaptureResult(val message: CapturedMessage, val matchedRules: List<Rule>)

/** Runs a notification through the rules and saves it to the inbox if any rule matches. */
class CaptureProcessor(
    private val ruleDao: RuleDao,
    private val messageDao: MessageDao,
) {
    /** Returns null when no rule matches or the message was already captured. */
    suspend fun process(notification: ParsedNotification): CaptureResult? {
        val matched = RuleEngine.matchingRules(notification.toMatchInput(), ruleDao.getEnabled())
        if (matched.isEmpty()) return null

        val message = CapturedMessage(
            dedupKey = notification.dedupKey,
            packageName = notification.packageName,
            appLabel = notification.appLabel,
            sender = notification.sender,
            conversation = notification.conversation,
            title = notification.title,
            text = notification.text,
            postedAt = notification.postedAt,
            matchedRuleIds = matched.map { it.id },
            tags = matched.mapNotNull { it.tag?.trim()?.ifEmpty { null } }.distinct(),
            notificationKey = notification.key,
        )
        // The unique dedupKey index makes reposts of the same message a no-op.
        val id = messageDao.insert(message)
        if (id == -1L) return null
        return CaptureResult(message.copy(id = id), matched)
    }
}
