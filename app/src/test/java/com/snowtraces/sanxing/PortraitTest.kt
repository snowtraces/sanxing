package com.snowtraces.sanxing

import com.snowtraces.sanxing.journal.Reflection
import com.snowtraces.sanxing.portrait.*
import org.junit.Assert.*
import org.junit.Test

class PortraitTest {
    private fun item(id: String, text: String) = Reflection(id, 100, "主题", "work", "问题", text, "计划", 100, 100)

    @Test fun equivalentTextGroupsAndHiddenStateSurvivesMoreSources() {
        val records = listOf(item("a", "开会看手机"), item("b", "开会看手机！"))
        var themes = reconcileThemes(records, emptyList())
        assertEquals(2, themes.single().sources.size)
        val id = themes.single().id
        repeat(3) { themes = tapTheme(themes, id) }
        themes = reconcileThemes(records + item("c", "开会看手机"), themes)
        assertEquals(3, themes.single().taps)
        assertEquals(3, themes.single().sources.size)
        assertEquals(emptyList<Theme>(), visibleThemes(themes, records))
        assertEquals(listOf(id), resolvedThemes(themes).map { it.id })
    }

    @Test fun changedSourcesInvalidateAnOldConfirmedGroup() {
        val previous = listOf(Theme("theme", "会议分心", mapOf("a" to "开会看手机", "b" to "开会刷手机"), 1))
        val updated = reconcileThemes(listOf(item("a", "开会看手机"), item("b", "睡觉太晚")), previous)
        assertEquals(2, updated.size)
        assertEquals(setOf("a"), updated.single { it.id == "theme" }.sources.keys)
    }
}
