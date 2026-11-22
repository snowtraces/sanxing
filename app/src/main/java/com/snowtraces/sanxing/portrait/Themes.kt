package com.snowtraces.sanxing.portrait

import com.snowtraces.sanxing.journal.Reflection
import java.util.UUID

data class Theme(val id: String, val label: String, val sources: Map<String, String>, val taps: Int = 0)

private val han = Regex("\\p{IsHan}")
private val content = Regex("[\\p{L}\\p{N}]")
private fun normalized(text: String): String = content.findAll(text.lowercase()).joinToString("") { it.value }

private val STOP_WORDS = Regex("^(材料中?(提出|提到|指出)|我(反思|反省|计划|总是|不应该|不该|应该|需要|经常|容易)?|面对|遇到|处于|在|关于|由于|因为)+")

private fun shortLabel(text: String): String {
    val clean = text.trim()
        .replace(STOP_WORDS, "")
        .replace(Regex("^(我不该|不应该|应该|需要|总是|容易|动辄|往往)"), "")
        .trim()
    val head = clean.split(Regex("[，,。；;：:\\s]")).firstOrNull { it.isNotBlank() } ?: clean
    val chinese = han.findAll(head).take(6).joinToString("") { it.value }
    if (chinese.length in 2..6) return chinese
    val fallback = han.findAll(clean).take(6).joinToString("") { it.value }
    return if (fallback.length >= 2) fallback.take(6) else "省思自省"
}

fun reconcileThemes(records: List<Reflection>, previous: List<Theme>): List<Theme> {
    val byId = records.associateBy { it.id }
    val themes = previous.mapNotNull { theme ->
        val active = theme.sources.filter { (id, text) -> byId[id]?.reflection == text }
        if (active.isEmpty()) null else theme.copy(sources = active)
    }.toMutableList()
    val known = themes.flatMap { it.sources.keys }.toSet()
    val owners = mutableMapOf<String, Int>()
    themes.forEachIndexed { index, theme ->
        theme.sources.values.forEach { text ->
            val key = normalized(text)
            if (key.isNotEmpty()) owners.putIfAbsent(key, index)
        }
    }
    records.filterNot { it.id in known }.sortedBy { it.createdAtMillis }.forEach { record ->
        val text = record.reflection
        val key = normalized(text)
        val index = owners[key]
        if (index == null) {
            if (key.isNotEmpty()) owners[key] = themes.size
            themes.add(Theme(UUID.randomUUID().toString(), shortLabel(text), mapOf(record.id to text)))
        } else {
            val theme = themes[index]
            themes[index] = theme.copy(sources = theme.sources + (record.id to text))
        }
    }
    return themes
}

fun visibleThemes(themes: List<Theme>, records: List<Reflection>): List<Theme> {
    val times = records.associate { it.id to it.occurredAtMillis }
    return themes.filter { it.taps < 3 }.sortedWith(compareByDescending<Theme> { it.sources.size }
        .thenByDescending { theme -> theme.sources.keys.maxOfOrNull { times[it] ?: 0L } ?: 0L }
        .thenBy { it.id }).take(6)
}

fun resolvedThemes(themes: List<Theme>): List<Theme> =
    themes.filter { it.taps >= 3 }.sortedByDescending { it.sources.size }

fun tapTheme(themes: List<Theme>, id: String): List<Theme> =
    themes.map { if (it.id == id) it.copy(taps = (it.taps + 1).coerceAtMost(3)) else it }

data class ThemeCandidate(val left: Theme, val right: Theme, val leftText: String, val rightText: String, val score: Double)

fun similarCandidates(themes: List<Theme>): List<ThemeCandidate> {
    val recent = themes.takeLast(240).map { it to it.sources.values.last() }
    val pairs = mutableListOf<ThemeCandidate>()
    for ((index, left) in recent.withIndex()) {
        for (right in recent.drop(index + 1)) {
            val score = similarity(left.second, right.second)
            if (score >= 0.34) pairs.add(ThemeCandidate(left.first, right.first, left.second, right.second, score))
        }
    }
    return pairs.sortedByDescending { it.score }.take(8)
}

private fun similarity(left: String, right: String): Double {
    val a = normalized(left).take(256).toSet()
    val b = normalized(right).take(256).toSet()
    val shared = a.intersect(b).size
    return if (shared < 2) 0.0 else 2.0 * shared / (a.size + b.size)
}

fun mergeThemes(themes: List<Theme>, candidate: ThemeCandidate, label: String): List<Theme> {
    val left = themes.find { it.id == candidate.left.id } ?: return themes
    val right = themes.find { it.id == candidate.right.id } ?: return themes
    if (candidate.leftText !in left.sources.values || candidate.rightText !in right.sources.values) return themes
    val keep = if (left.taps >= right.taps) left else right
    val drop = if (keep.id == left.id) right else left
    return themes.filterNot { it.id == drop.id }.map {
        if (it.id == keep.id) it.copy(label = label, sources = left.sources + right.sources) else it
    }
}
