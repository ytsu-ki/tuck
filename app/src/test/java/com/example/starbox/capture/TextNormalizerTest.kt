package com.example.starbox.capture

import com.example.starbox.capture.TextNormalizer.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    private val wechat = "com.tencent.mm"

    @Test
    fun wechatGroupSplitsSenderFromGroup() {
        assertEquals(
            Result(sender = "张三", conversation = "项目群", text = "明天开会"),
            TextNormalizer.normalize(wechat, "项目群", "[3条]张三: 明天开会"),
        )
    }

    @Test
    fun wechatFullWidthColon() {
        assertEquals(
            Result(sender = "张三", conversation = "项目群", text = "收到"),
            TextNormalizer.normalize(wechat, "项目群", "张三：收到"),
        )
    }

    @Test
    fun wechatPrivateChatWithRepeatedName() {
        assertEquals(
            Result(sender = "Alice", conversation = null, text = "hi"),
            TextNormalizer.normalize(wechat, "Alice", "[2]Alice: hi"),
        )
    }

    @Test
    fun wechatPrivateChatPlainMessage() {
        assertEquals(
            Result(sender = "Alice", conversation = null, text = "see you at 5"),
            TextNormalizer.normalize(wechat, "Alice", "[2 messages] see you at 5"),
        )
    }

    @Test
    fun doesNotSplitUrlsOrTimes() {
        assertEquals("https://example.com", TextNormalizer.normalize(wechat, "Alice", "https://example.com").text)
        assertEquals("10:30 meeting", TextNormalizer.normalize(wechat, "Alice", "10:30 meeting").text)
    }

    @Test
    fun otherAppsAreLeftAlone() {
        assertEquals(
            Result(sender = "Reminder", conversation = null, text = "Note: call back"),
            TextNormalizer.normalize("com.example.other", "Reminder", "Note: call back"),
        )
    }
}
