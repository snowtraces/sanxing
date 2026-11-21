package com.snowtraces.sanxing.backup

import com.snowtraces.sanxing.journal.Category
import com.snowtraces.sanxing.journal.JournalSnapshot
import com.snowtraces.sanxing.journal.Reflection
import com.snowtraces.sanxing.portrait.Theme
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale

data class Backup(val exportedAt: Long, val snapshot: JournalSnapshot)

fun encodeThemes(themes: List<Theme>): String = JSONArray().apply {
    themes.forEach { theme ->
        val sources = JSONObject()
        theme.sources.forEach { (id, text) -> sources.put(id, text) }
        put(JSONObject().put("id", theme.id).put("label", theme.label)
            .put("sources", sources).put("taps", theme.taps))
    }
}.toString()

fun decodeThemes(text: String): List<Theme> = JSONArray(text).objects { item ->
    val sources = item.getJSONObject("sources")
    Theme(item.text("id"), item.text("label"), sources.keys().asSequence().associateWith(sources::text),
        item.number("taps").also { require(it in 0..3) }.toInt())
}

fun encodeBackup(snapshot: JournalSnapshot, exportedAt: Long): String = JSONObject()
    .put("schemaVersion", 1)
    .put("exportedAtMillis", exportedAt)
    .put("categories", JSONArray().apply {
        snapshot.categories.forEach {
            put(JSONObject().put("id", it.id).put("name", it.name)
                .put("archived", it.archived).put("position", it.position))
        }
    })
    .put("reflections", JSONArray().apply {
        snapshot.records.forEach {
            put(JSONObject().put("id", it.id).put("occurredAtMillis", it.occurredAtMillis)
                .put("title", it.title).put("categoryId", it.categoryId)
                .put("description", it.description).put("reflection", it.reflection).put("action", it.action)
                .put("createdAtMillis", it.createdAtMillis).put("updatedAtMillis", it.updatedAtMillis))
        }
    })
    .put("portraitThemes", JSONArray(snapshot.portrait))
    .toString(2)

fun readBackup(stream: InputStream): Backup {
    val text = stream.bufferedReader(Charsets.UTF_8).use { reader ->
        val buffer = CharArray(8192)
        buildString {
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                require(length + count <= 16_000_000) { "备份文件过大" }
                append(buffer, 0, count)
            }
        }
    }
    return decodeBackup(text)
}

fun decodeBackup(text: String): Backup {
    val root = JSONObject(text)
    require(root.number("schemaVersion") == 1L) { "备份版本不受支持" }
    val exportedAt = root.time("exportedAtMillis")
    val categories = root.getJSONArray("categories").objects {
        Category(it.text("id"), it.text("name").also { name -> require(name.length <= 24) },
            it.opt("archived") as? Boolean ?: error("分类状态格式错误"),
            it.number("position").also { position -> require(position in 0..Int.MAX_VALUE.toLong()) }.toInt())
    }
    require(categories.any { !it.archived })
    val categoriesById = categories.associateBy { it.id }
    require(categoriesById.size == categories.size)
    require(categories.map { it.name.lowercase(Locale.ROOT) }.distinct().size == categories.size)
    val records = root.getJSONArray("reflections").objects {
        Reflection(it.text("id"), it.number("occurredAtMillis"), it.text("title"), it.text("categoryId"),
            it.text("description"), it.text("reflection"), it.text("action"),
            it.time("createdAtMillis"), it.time("updatedAtMillis"))
    }
    require(records.all { it.categoryId in categoriesById && it.updatedAtMillis >= it.createdAtMillis })
    val recordsById = records.associateBy { it.id }
    require(recordsById.size == records.size)
    val portrait = root.getJSONArray("portraitThemes").toString()
    val themes = decodeThemes(portrait)
    require(themes.map { it.id }.distinct().size == themes.size)
    val sourceIds = mutableSetOf<String>()
    for (theme in themes) {
        require(theme.sources.isNotEmpty() && theme.label.matches(Regex("[\\p{IsHan}]{2,5}")))
        require(theme.sources.all { (id, version) ->
            recordsById[id]?.reflection == version && sourceIds.add(id)
        })
    }
    return Backup(exportedAt, JournalSnapshot(records, categories, portrait))
}

private fun JSONObject.text(key: String): String =
    (opt(key) as? String)?.also { require(it.isNotBlank() && it == it.trim()) }
        ?: error("备份字段格式错误")

private fun JSONObject.number(key: String): Long = when (val value = opt(key)) {
    is Int -> value.toLong()
    is Long -> value
    else -> error("备份数值格式错误")
}

private fun JSONObject.time(key: String): Long =
    number(key).also { require(it > 0) }

private fun <T> JSONArray.objects(read: (JSONObject) -> T): List<T> =
    List(length()) { read(getJSONObject(it)) }
