package com.snowtraces.sanxing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeOpensEditorAndHistoryAndPortraitReachTheirControls() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("首页").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("＋ 新增").performClick()
        compose.onNodeWithText("原始草稿").assertExists()
        compose.onNodeWithText("‹  返回").performClick()
        compose.onNodeWithText("历史").performClick()
        compose.onNodeWithText("搜索复盘").assertExists()
        compose.onNodeWithText("画像").performClick()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("API 密钥").assertExists()
    }

    @Test fun savedReflectionCanBeFoundInHistoryAndItsBubbleCanBeDismissed() {
        val suffix = System.currentTimeMillis().toString().takeLast(5)
        val title = "测试复盘$suffix"
        val label = suffix.map { "零一二三四五六七八九"[it.digitToInt()] }.joinToString("")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("首页").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("＋ 新增").performClick()
        enter("主题", title)
        enter("问题描述", "会议期间频繁查看手机")
        enter("省 · 需要反思的行为", label)
        enter("悟 · 下一次的行动", "会前关闭通知")
        compose.onNodeWithText("保存复盘").performScrollTo().performClick()
        awaitText("复盘详情")
        compose.onNodeWithText(title).assertExists()
        compose.onNodeWithText("‹  返回").performClick()
        compose.onNodeWithText("历史").performClick()
        compose.onNode(hasSetTextAction() and hasText("搜索复盘")).performTextInput(title)
        compose.onNode(hasText(title) and hasClickAction() and !hasSetTextAction()).performClick()
        awaitText("复盘详情")
        compose.onNodeWithText("‹  返回").performClick()
        compose.onNode(hasSetTextAction() and hasText("搜索复盘")).assertExists()
        compose.onNodeWithText("画像").performClick()
        val bubble = compose.onNodeWithContentDescription("$label，出现 1 次")
        repeat(3) { bubble.performClick() }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("$label，出现 1 次").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("历史").performClick()
        compose.onNode(hasText(title) and hasClickAction() and !hasSetTextAction()).performClick()
        compose.onNodeWithText("删除").performScrollTo().performClick()
        compose.onNode(hasText("删除") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun enter(label: String, text: String) {
        compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextInput(text)
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
