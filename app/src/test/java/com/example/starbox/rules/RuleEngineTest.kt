package com.example.starbox.rules

import com.example.starbox.data.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val wechat = "com.tencent.mm"
    private val x = "com.twitter.android"

    private fun input(
        packageName: String = wechat,
        title: String? = "项目群",
        sender: String? = "张三",
        conversation: String? = "项目群",
        text: String = "明天上午十点开会",
    ) = MatchInput(packageName, title, sender, conversation, text)

    private fun rule(
        packages: List<String> = emptyList(),
        senders: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        enabled: Boolean = true,
    ) = Rule(name = "r", packages = packages, senders = senders, keywords = keywords, enabled = enabled)

    @Test
    fun ruleWithoutConditionsNeverMatches() {
        assertFalse(RuleEngine.matches(rule(senders = listOf("  ")), input()))
        assertFalse(RuleEngine.hasConditions(rule(keywords = listOf(""))))
    }

    @Test
    fun appOnlyRuleMatchesEverythingFromThatApp() {
        assertTrue(RuleEngine.matches(rule(packages = listOf(wechat)), input()))
        assertFalse(RuleEngine.matches(rule(packages = listOf(x)), input()))
    }

    @Test
    fun senderMatchesPersonGroupOrTitle() {
        assertTrue(RuleEngine.matches(rule(senders = listOf("张三")), input()))
        assertTrue(RuleEngine.matches(rule(senders = listOf("项目")), input(sender = "李四")))
        assertFalse(RuleEngine.matches(rule(senders = listOf("王五")), input()))
    }

    @Test
    fun valuesInOneFieldAreOr() {
        assertTrue(RuleEngine.matches(rule(senders = listOf("王五", "张三")), input()))
        assertTrue(RuleEngine.matches(rule(keywords = listOf("紧急", "开会")), input()))
    }

    @Test
    fun fieldsAreAnd() {
        val r = rule(packages = listOf(wechat), senders = listOf("张三"), keywords = listOf("紧急"))
        assertFalse(RuleEngine.matches(r, input()))
        assertTrue(RuleEngine.matches(r, input(text = "紧急：服务器宕机")))
        assertFalse(RuleEngine.matches(r, input(packageName = x, text = "紧急")))
    }

    @Test
    fun caseAndWidthInsensitive() {
        val r = rule(keywords = listOf("ＵＲＧＥＮＴ"))
        assertTrue(RuleEngine.matches(r, input(text = "This is urgent!")))
        assertTrue(RuleEngine.matches(rule(senders = listOf("@NASA")), input(packageName = x, title = null, sender = "@nasa", conversation = null)))
    }

    @Test
    fun keywordOnlyRuleMatchesAnySender() {
        // No app and no sender: any notification that mentions the keyword.
        val r = rule(keywords = listOf("开会"))
        assertTrue(RuleEngine.hasConditions(r))
        assertTrue(RuleEngine.matches(r, input()))
        assertTrue(RuleEngine.matches(r, input(packageName = x, title = "Bob", sender = "Bob", conversation = null)))
        assertFalse(RuleEngine.matches(r, input(text = "晚上吃什么")))
        // Keywords plus an app: only that app.
        val wechatOnly = rule(packages = listOf(wechat), keywords = listOf("开会"))
        assertTrue(RuleEngine.matches(wechatOnly, input(sender = "李四")))
        assertFalse(RuleEngine.matches(wechatOnly, input(packageName = x)))
    }

    @Test
    fun keywordsAlsoSearchTickerText() {
        // Some apps keep the real message only in the ticker line.
        val r = rule(keywords = listOf("urgent"))
        val generic = MatchInput(x, title = "Bob", sender = "Bob", conversation = null, text = "New message")
        assertFalse(RuleEngine.matches(r, generic))
        assertTrue(RuleEngine.matches(r, generic.copy(extraText = "Bob: urgent, call me")))
        // Senders don't look at it, so a name inside the ticker text doesn't count as the sender.
        assertFalse(RuleEngine.matches(rule(senders = listOf("Carol")), generic.copy(extraText = "Bob: ask Carol")))
    }

    @Test
    fun matchingRulesSkipsDisabled() {
        val on = rule(senders = listOf("张三"))
        val off = rule(senders = listOf("张三"), enabled = false)
        assertEquals(listOf(on), RuleEngine.matchingRules(input(), listOf(on, off)))
        // The editor preview still evaluates disabled rules.
        assertTrue(RuleEngine.matches(off, input()))
    }
}
