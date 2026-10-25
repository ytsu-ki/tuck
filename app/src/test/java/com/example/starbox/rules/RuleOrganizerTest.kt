package com.example.starbox.rules

import com.example.starbox.capture.ParsedNotification
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleOrganizerTest {
    private val wechat = "com.tencent.mm"
    private val x = "com.twitter.android"
    private val labels = mapOf(wechat to "WeChat", x to "X")

    private fun rule(id: Long, packages: List<String> = emptyList(), enabled: Boolean = true, createdAt: Long = id) =
        Rule(id = id, name = "r$id", packages = packages, senders = listOf("s"), enabled = enabled, createdAt = createdAt)

    @Test
    fun groupsByAppThenSeveralThenAny() {
        val groups = RuleOrganizer.group(
            listOf(
                rule(1),
                rule(2, listOf(x)),
                rule(3, listOf(wechat, x)),
                rule(4, listOf(wechat)),
            ),
            labelOf = { labels.getValue(it) },
        )
        assertEquals(
            listOf(
                RuleGroupKey.App(wechat),
                RuleGroupKey.App(x),
                RuleGroupKey.SeveralApps,
                RuleGroupKey.AnyApp,
            ),
            groups.map { it.key },
        )
    }

    @Test
    fun activeRulesComeBeforePausedOnes() {
        val groups = RuleOrganizer.group(
            listOf(rule(1, listOf(wechat), enabled = false), rule(2, listOf(wechat)), rule(3, listOf(wechat))),
            labelOf = { labels.getValue(it) },
        )
        assertEquals(listOf(2L, 3L, 1L), groups.single().rules.map { it.id })
    }

    @Test
    fun statsCountMessagesPerRule() {
        fun message(id: Long, at: Long, vararg rules: Long) = CapturedMessage(
            id = id, dedupKey = "$id", packageName = wechat, appLabel = "WeChat", sender = null,
            conversation = null, title = null, text = "", postedAt = at, matchedRuleIds = rules.toList(),
        )
        val stats = RuleOrganizer.stats(listOf(message(1, 100, 7), message(2, 300, 7, 8), message(3, 200, 7)))
        assertEquals(RuleStats(starredCount = 3, lastStarredAt = 300), stats[7])
        assertEquals(RuleStats(starredCount = 1, lastStarredAt = 300), stats[8])
        assertNull(stats[9])
    }

    @Test
    fun senderSuggestionsAreRecentDistinctAndFiltered() {
        fun seen(pkg: String, sender: String?, conversation: String? = null) = ParsedNotification(
            key = "k", packageName = pkg, appLabel = "", title = null, text = "", sender = sender,
            conversation = conversation, postedAt = 0, dedupKey = "$pkg$sender$conversation",
        )
        val recent = listOf(
            seen(wechat, "张三", "项目群"),
            seen(x, "@nasa"),
            seen(wechat, "李四"),
            seen(wechat, "张三"),
            seen(wechat, "Alice"),
        )
        assertEquals(
            listOf("项目群", "李四", "Alice"),
            RuleOrganizer.senderSuggestions(recent, packages = listOf(wechat), exclude = listOf(" 张三 ")),
        )
        assertEquals(listOf("张三", "项目群"), RuleOrganizer.senderSuggestions(recent, emptyList(), emptyList(), limit = 2))
    }
}
