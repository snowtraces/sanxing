package com.snowtraces.sanxing

import androidx.test.platform.app.InstrumentationRegistry
import com.snowtraces.sanxing.aiassist.AiClient
import com.snowtraces.sanxing.aiassist.AiConfig
import com.snowtraces.sanxing.backup.decodeBackup
import com.snowtraces.sanxing.backup.encodeBackup
import com.snowtraces.sanxing.backup.encodeThemes
import com.snowtraces.sanxing.journal.Draft
import com.snowtraces.sanxing.journal.JournalStore
import com.snowtraces.sanxing.journal.defaultCategories
import com.snowtraces.sanxing.portrait.reconcileThemes
import com.snowtraces.sanxing.portrait.tapTheme
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class JournalDeviceTest {
    @Test fun journalReopensAndMalformedRestoreRollsBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-${UUID.randomUUID()}.db"
        val item = Draft(100, "上午复盘", "work", "开会分心", "看手机", "会前静音")
            .toReflection("record", 100, 100)
        try {
            JournalStore(context, name).use { store ->
                store.save(item)
                store.save(item)
                store.updateCategory("work", "办公")
                var themes = reconcileThemes(listOf(item), emptyList())
                repeat(3) { themes = tapTheme(themes, themes.single().id) }
                store.writePortrait(encodeThemes(themes))
            }
            JournalStore(context, name).use { reopened ->
                val saved = reopened.snapshot()
                assertEquals(listOf(item), saved.records)
                assertEquals("办公", saved.categories.first { it.id == "work" }.name)
                val encoded = encodeBackup(saved, 200)
                val imported = decodeBackup(encoded).snapshot
                assertEquals(saved, imported)
                assertFalse(runCatching { decodeBackup(encoded.replace("\"schemaVersion\": 1", "\"schemaVersion\": 17")) }.isSuccess)
                val invalid = imported.copy(records = listOf(item.copy(categoryId = "missing")))
                assertFalse(runCatching { reopened.restore(invalid) }.isSuccess)
                assertEquals(saved, reopened.snapshot())
                reopened.delete(item.id)
                reopened.restore(imported)
                assertEquals(saved, reopened.snapshot())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun modelResultPreservesTimeAndRejectsInvalidFieldTypes() {
        val draft = Draft(10, categoryId = "work")
        val config = AiConfig("https://example.com/v1", "testing", "fake")
        val response = """{"title":"开会分心","categoryId":"other","description":"开会时看了手机","reflection":"我不该看手机","action":"我会在会前静音"}"""
        val ai = AiClient { _, _, _ -> response }
        val organized = ai.organize(config, "会议中看手机", draft, defaultCategories())
        assertEquals("work", organized.categoryId)
        assertEquals(10L, organized.occurredAtMillis)
        assertEquals("我会在会前静音", organized.action)
        val malformed = AiClient { _, _, _ -> response.replace("\"开会分心\"", "123") }
        assertFalse(runCatching { malformed.organize(config, "会议中看手机", draft, defaultCategories()) }.isSuccess)
    }
}
