package com.snowtraces.sanxing.journal

import java.util.UUID

data class Reflection(
    val id: String,
    val occurredAtMillis: Long,
    val title: String,
    val categoryId: String,
    val description: String,
    val reflection: String,
    val action: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun draft() = Draft(occurredAtMillis, title, categoryId, description, reflection, action)
}

data class Draft(
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val title: String = "",
    val categoryId: String = "",
    val description: String = "",
    val reflection: String = "",
    val action: String = "",
) {
    fun missing(): Set<String> = mapOf(
        "title" to title, "category" to categoryId, "description" to description,
        "reflection" to reflection, "action" to action,
    ).filterValues { it.isBlank() }.keys

    fun toReflection(id: String, created: Long, updated: Long): Reflection {
        require(missing().isEmpty())
        return Reflection(id, occurredAtMillis, title.trim(), categoryId.trim(), description.trim(),
            reflection.trim(), action.trim(), created, updated)
    }
}

data class Category(val id: String, val name: String, val archived: Boolean, val position: Int)

fun defaultCategories(): List<Category> = listOf(
    "learning" to "学习", "method" to "方法论", "emotion" to "情绪",
    "daily" to "日常", "work" to "工作", "strategy" to "策略", "social" to "交际",
).mapIndexed { index, (id, name) -> Category(id, name, false, index) }

fun newId(): String = UUID.randomUUID().toString()
