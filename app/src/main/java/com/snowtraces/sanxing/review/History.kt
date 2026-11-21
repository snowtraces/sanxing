package com.snowtraces.sanxing.review

import com.snowtraces.sanxing.journal.Reflection

fun searchHistory(
    records: List<Reflection>,
    categories: Map<String, String>,
    query: String,
    categoryFilter: String? = null,
): List<Reflection> {
    val term = query.trim()
    return records.filter { record ->
        val matchesCategory = categoryFilter.isNullOrBlank() || record.categoryId == categoryFilter
        val matchesTerm = term.isEmpty() || listOf(
            record.title,
            categories[record.categoryId].orEmpty(),
            record.description,
            record.reflection,
            record.action
        ).any { it.contains(term, ignoreCase = true) }
        matchesCategory && matchesTerm
    }.sortedWith(compareByDescending<Reflection> { it.occurredAtMillis }
        .thenByDescending { it.createdAtMillis }.thenBy { it.id })
}

fun chooseReflection(records: List<Reflection>, current: String? = null): Reflection? =
    records.filter { it.id != current }.randomOrNull() ?: records.firstOrNull()
