package com.example.starbox.rules

import com.example.starbox.capture.ParsedNotification
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Rule

/** Where a rule is listed: under its one app, under "several apps", or under "any app". */
sealed interface RuleGroupKey {
    data class App(val packageName: String) : RuleGroupKey
    data object SeveralApps : RuleGroupKey
    data object AnyApp : RuleGroupKey
}

data class RuleGroup(val key: RuleGroupKey, val rules: List<Rule>)

/** How useful a rule has been: messages it starred that are still in the inbox. */
data class RuleStats(val starredCount: Int = 0, val lastStarredAt: Long? = null)

/** Pure helpers that keep the rules screens organized; unit tested. */
object RuleOrganizer {

    fun groupKey(rule: Rule): RuleGroupKey {
        val packages = rule.packages.distinct()
        return when (packages.size) {
            0 -> RuleGroupKey.AnyApp
            1 -> RuleGroupKey.App(packages.single())
            else -> RuleGroupKey.SeveralApps
        }
    }

    /**
     * Per-app groups first (alphabetical by app name), then "several apps", then "any app".
     * Inside a group, active rules come before paused ones, then in rule order.
     */
    fun group(rules: List<Rule>, labelOf: (String) -> String): List<RuleGroup> {
        val ruleOrder = compareByDescending<Rule> { it.enabled }.thenBy { it.sortOrder }.thenBy { it.createdAt }
        return rules.groupBy(::groupKey)
            .map { (key, members) -> RuleGroup(key, members.sortedWith(ruleOrder)) }
            .sortedWith(
                compareBy<RuleGroup> {
                    when (it.key) {
                        is RuleGroupKey.App -> 0
                        RuleGroupKey.SeveralApps -> 1
                        RuleGroupKey.AnyApp -> 2
                    }
                }.thenBy { (it.key as? RuleGroupKey.App)?.let { app -> labelOf(app.packageName).lowercase() }.orEmpty() },
            )
    }

    fun stats(messages: List<CapturedMessage>): Map<Long, RuleStats> {
        val result = HashMap<Long, RuleStats>()
        for (message in messages) {
            for (ruleId in message.matchedRuleIds.distinct()) {
                val current = result[ruleId] ?: RuleStats()
                result[ruleId] = RuleStats(
                    starredCount = current.starredCount + 1,
                    lastStarredAt = maxOf(current.lastStarredAt ?: Long.MIN_VALUE, message.postedAt),
                )
            }
        }
        return result
    }

    /**
     * Names seen in recent notifications (newest first), so senders can be picked instead of
     * typed. Limited to [packages] when any are chosen; skips names already in [exclude].
     */
    fun senderSuggestions(
        recent: List<ParsedNotification>,
        packages: List<String>,
        exclude: List<String>,
        limit: Int = 12,
    ): List<String> {
        val taken = exclude.map { RuleEngine.fold(it.trim()) }.toMutableSet()
        val result = mutableListOf<String>()
        for (notification in recent) {
            if (packages.isNotEmpty() && notification.packageName !in packages) continue
            for (name in listOfNotNull(notification.sender, notification.conversation)) {
                val trimmed = name.trim()
                if (trimmed.isEmpty() || !taken.add(RuleEngine.fold(trimmed))) continue
                result += trimmed
                if (result.size == limit) return result
            }
        }
        return result
    }
}
