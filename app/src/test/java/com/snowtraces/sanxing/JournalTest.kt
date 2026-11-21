package com.snowtraces.sanxing

import com.snowtraces.sanxing.journal.Draft
import com.snowtraces.sanxing.journal.Reflection
import com.snowtraces.sanxing.journal.defaultCategories
import com.snowtraces.sanxing.portrait.reconcileThemes
import com.snowtraces.sanxing.review.searchHistory
import com.snowtraces.sanxing.review.chooseReflection
import org.junit.Assert.*
import org.junit.Test

class JournalTest {
    @Test fun blankFieldsAreRequiredAndValuesAreTrimmed() {
        val draft = Draft(1, "  午后复盘  ", "work", " ", "走神", "关闭通知")
        assertEquals(setOf("description"), draft.missing())
        assertEquals("午后复盘", draft.copy(description = " 新消息 ").toReflection("r", 2, 3).title)
    }

    @Test fun sampleReflectionsAreValidAndCoverCategories() {
        val samples = sampleReflections()
        val categoryIds = defaultCategories().map { it.id }.toSet()
        assertTrue(samples.size >= 5)
        samples.forEach { item ->
            assertTrue(item.draft().missing().isEmpty())
            assertTrue(categoryIds.contains(item.categoryId))
        }
        val themes = reconcileThemes(samples, emptyList())
        assertTrue(themes.isNotEmpty())
        themes.forEach { theme ->
            assertTrue(theme.label.length in 2..4)
        }
    }

    @Test fun historyFindsChineseSubstringAndUsesStableTimeOrder() {
        val first = Reflection("a", 1, "午后复盘", "work", "开会走神", "分心", "关闭通知", 1, 1)
        val second = first.copy(id = "b", occurredAtMillis = 2)
        assertEquals(listOf("b", "a"), searchHistory(listOf(first, second), mapOf("work" to "工作"), " 作 ").map { it.id })
        assertEquals(listOf("b", "a"), searchHistory(listOf(first, second), mapOf("work" to "工作"), "", "work").map { it.id })
        assertEquals(emptyList<Reflection>(), searchHistory(listOf(first, second), mapOf("work" to "工作"), "", "study"))
        assertEquals(emptyList<Reflection>(), searchHistory(listOf(first), emptyMap(), "读书"))
        assertEquals("b", chooseReflection(listOf(first, second), "a")?.id)
    }
}

private fun sampleReflections(baseTimeMillis: Long = System.currentTimeMillis()): List<Reflection> {
    val day = 86_400_000L
    val hour = 3_600_000L
    return listOf(
        Reflection(
            id = "sample-reflection-1",
            occurredAtMillis = baseTimeMillis - 2 * hour,
            title = "晨会急躁插话",
            categoryId = "work",
            description = "早间方案评审会上未能耐住性子听完，便急于打断。",
            reflection = "不该急躁打断，未听完前妄下定论。",
            action = "遇意见相左时先静听三秒，笔录其要，再温言商榷。",
            createdAtMillis = baseTimeMillis - 2 * hour,
            updatedAtMillis = baseTimeMillis - 2 * hour,
        ),
        Reflection(
            id = "sample-reflection-2",
            occurredAtMillis = baseTimeMillis - 1 * day - 3 * hour,
            title = "深夜刷屏内耗",
            categoryId = "emotion",
            description = "临睡前被短视频吸引，刷到凌晨一点。",
            reflection = "不该熬夜内耗，逃避对真实的专注与安宁。",
            action = "每晚十点半将手机置于书房，阅五页书后安然就寝。",
            createdAtMillis = baseTimeMillis - 1 * day - 3 * hour,
            updatedAtMillis = baseTimeMillis - 1 * day - 3 * hour,
        ),
        Reflection(
            id = "sample-reflection-3",
            occurredAtMillis = baseTimeMillis - 3 * day,
            title = "碍情面盲目应承",
            categoryId = "strategy",
            description = "朋友委托繁杂琐事，碍于情面一口应下，导致手头核心事务延宕。",
            reflection = "不该盲目应承，边界不清终致自顾不暇。",
            action = "坦诚告知当下所守主线，有所不为方能有所大为。",
            createdAtMillis = baseTimeMillis - 3 * day,
            updatedAtMillis = baseTimeMillis - 3 * day,
        ),
        Reflection(
            id = "sample-reflection-4",
            occurredAtMillis = baseTimeMillis - 5 * day,
            title = "读书贪多走马观花",
            categoryId = "learning",
            description = "翻阅了三部大作却只满足于划线，并未内化深思。",
            reflection = "不该贪多求快，浮于皮相而不求甚解。",
            action = "每周精读一卷，必取其一义撰写札记并躬身践行。",
            createdAtMillis = baseTimeMillis - 5 * day,
            updatedAtMillis = baseTimeMillis - 5 * day,
        ),
        Reflection(
            id = "sample-reflection-5",
            occurredAtMillis = baseTimeMillis - 7 * day,
            title = "茶余附和是非",
            categoryId = "social",
            description = "午餐席间听闻旁人议论同仁，也随声附和了两句。",
            reflection = "不该附和是非，群处守嘴独处守心。",
            action = "闲谈莫论人非，默然守持中道。",
            createdAtMillis = baseTimeMillis - 7 * day,
            updatedAtMillis = baseTimeMillis - 7 * day,
        ),
    )
}
