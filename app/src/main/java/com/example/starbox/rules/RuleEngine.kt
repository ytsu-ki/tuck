package com.example.starbox.rules

import com.example.starbox.capture.ParsedNotification
import com.example.starbox.data.model.Rule
import java.text.Normalizer
import java.util.Locale

/** The fields a rule can look at. */
data class MatchInput(
    val packageName: String,
    val title: String?,
    val sender: String?,
    val conversation: String?,
    val text: String,
    /** Other text on the notification, e.g. its ticker line; searched by keywords only. */
    val extraText: String? = null,
)

fun ParsedNotification.toMatchInput() = MatchInput(packageName, title, sender, conversation, text, extraText)

/**
 * Decides which rules star a notification.
 *
 * - Apps: the notification's package must be one of them.
 * - Senders: any value contained in the sender, group/conversation or title.
 * - Keywords: any value contained anywhere in the notification.
 * Fields combine with AND, values within a field with OR, empty fields are ignored.
 * Text is compared case-insensitively and with full-width/half-width forms folded together.
 */
object RuleEngine {

    fun matchingRules(input: MatchInput, rules: List<Rule>): List<Rule> =
        rules.filter { it.enabled && matches(it, input) }

    /** A rule with no conditions would star everything, so it never matches. */
    fun hasConditions(rule: Rule): Boolean =
        rule.packages.isNotEmpty() || rule.senders.folded().isNotEmpty() || rule.keywords.folded().isNotEmpty()

    /** Ignores [Rule.enabled] so the editor can preview a disabled rule. */
    fun matches(rule: Rule, input: MatchInput): Boolean {
        val senders = rule.senders.folded()
        val keywords = rule.keywords.folded()
        if (rule.packages.isEmpty() && senders.isEmpty() && keywords.isEmpty()) return false

        if (rule.packages.isNotEmpty() && input.packageName !in rule.packages) return false

        if (senders.isNotEmpty()) {
            val names = listOfNotNull(input.sender, input.conversation, input.title).map(::fold)
            if (senders.none { wanted -> names.any { it.contains(wanted) } }) return false
        }

        if (keywords.isNotEmpty()) {
            val haystack = fold(
                listOfNotNull(input.title, input.sender, input.conversation, input.text, input.extraText).joinToString("\n"),
            )
            if (keywords.none { haystack.contains(it) }) return false
        }
        return true
    }

    fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)

    private fun List<String>.folded(): List<String> = mapNotNull { fold(it.trim()).ifEmpty { null } }
}
