package com.snowtraces.sanxing

import com.snowtraces.sanxing.aiassist.AiClient
import com.snowtraces.sanxing.aiassist.AiConfig
import com.snowtraces.sanxing.journal.Category
import com.snowtraces.sanxing.journal.Draft
import org.junit.Assert.*
import org.junit.Test

class AiClientTest {
    private val config = AiConfig("https://example.com/v1", "test-model", "test-key")
    private val categories = listOf(
        Category("work", "工作", false, 0),
        Category("study", "学习", false, 1)
    )

    @Test
    fun organizeParsesValidJsonWithMarkdownFence() {
        val markdownJson = """
            ```json
            {
              "title": "会议分心复盘",
              "categoryId": "work",
              "description": "下午开会时看了半小时手机消息",
              "reflection": "我不应该在重要会议上分心看社交软件",
              "action": "我应该在开会前将手机静音并收起"
            }
            ```
        """.trimIndent()
        val client = AiClient { _, _, _ -> markdownJson }
        val draft = client.organize(config, "今天开会看手机了", Draft(), categories)

        assertEquals("会议分心复盘", draft.title)
        assertEquals("work", draft.categoryId)
        assertEquals("下午开会时看了半小时手机消息", draft.description)
        assertEquals("我不应该在重要会议上分心看社交软件", draft.reflection)
        assertEquals("我应该在开会前将手机静音并收起", draft.action)
    }

    @Test
    fun fallbackToDraftCategoryWhenSuggestedUnknown() {
        val json = """
            {
              "title": "测试",
              "categoryId": "unknown_cat",
              "description": "事",
              "reflection": "省",
              "action": "悟"
            }
        """.trimIndent()
        val client = AiClient { _, _, _ -> json }
        val draft = client.organize(config, "原始草稿", Draft(categoryId = "study"), categories)

        assertEquals("study", draft.categoryId)
    }

    @Test
    fun sameBehaviorExtractsShortLabel() {
        val client = AiClient { _, _, _ -> """{"same": true, "label": "会议分心"}""" }
        val label = client.sameBehavior(config, "开会玩手机", "开会看消息")
        assertEquals("会议分心", label)
    }

    @Test
    fun endpointAppendsV1WhenMissing() {
        val cfg1 = AiConfig("https://api.example.com/ai", "qwen-flash", "test-key")
        assertEquals("https://api.example.com/ai/v1/chat/completions", cfg1.endpoint().toString())

        val cfg2 = AiConfig("https://api.example.com/v1", "gpt-4o", "test-key")
        assertEquals("https://api.example.com/v1/chat/completions", cfg2.endpoint().toString())
    }
}
