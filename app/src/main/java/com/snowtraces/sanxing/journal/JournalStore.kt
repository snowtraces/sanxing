package com.snowtraces.sanxing.journal

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class JournalSnapshot(val records: List<Reflection>, val categories: List<Category>, val portrait: String)

class JournalStore(context: Context, name: String = "sanxing.db") :
    SQLiteOpenHelper(context, name, null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL UNIQUE, archived INTEGER NOT NULL, position INTEGER NOT NULL)")
        db.execSQL("""CREATE TABLE records (
            id TEXT PRIMARY KEY NOT NULL, occurred INTEGER NOT NULL, title TEXT NOT NULL,
            category TEXT NOT NULL REFERENCES categories(id), description TEXT NOT NULL,
            reflection TEXT NOT NULL, action TEXT NOT NULL, created INTEGER NOT NULL, updated INTEGER NOT NULL
        )""".trimIndent())
        db.execSQL("CREATE TABLE portrait (id INTEGER PRIMARY KEY CHECK(id = 1), body TEXT NOT NULL)")
        defaultCategories().forEach { insertCategory(db, it) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Unsupported journal schema")
    }

    fun snapshot(): JournalSnapshot = transaction { db ->
        val records = db.rawQuery(
            "SELECT id, occurred, title, category, description, reflection, action, created, updated FROM records",
            null,
        ).collect { Reflection(getString(0), getLong(1), getString(2), getString(3), getString(4),
            getString(5), getString(6), getLong(7), getLong(8)) }
        val categories = readCategories(db)
        val portrait = db.rawQuery("SELECT body FROM portrait WHERE id = 1", null).use {
            if (it.moveToFirst()) it.getString(0) else "[]"
        }
        JournalSnapshot(records, categories, portrait)
    }

    fun ensureSampleData(): Boolean = false

    fun <T> atomically(action: () -> T): T = transaction { action() }

    fun save(item: Reflection) = transaction { db ->
        val previous = db.rawQuery("SELECT category FROM records WHERE id = ?", arrayOf(item.id)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        require(item.draft().missing().isEmpty())
        require(readCategories(db).any { it.id == item.categoryId && (!it.archived || previous == it.id) })
        writeReflection(db, item)
    }

    fun delete(id: String) = transaction { db ->
        db.delete("records", "id = ?", arrayOf(id))
    }

    fun updateCategory(id: String?, name: String, archive: Boolean? = null) = transaction { db ->
        val categories = readCategories(db)
        val existing = categories.firstOrNull { it.id == id }
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 24) { "分类名称须为 1～24 个字符" }
        require(categories.none { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) { "分类名称已存在" }
        require(id == null || existing != null)
        if (existing == null) {
            insertCategory(db, Category(newId(), trimmed, false, (categories.maxOfOrNull { it.position } ?: -1) + 1))
        } else {
            val archived = archive ?: existing.archived
            require(!archived || categories.any { it.id != id && !it.archived }) { "至少保留一个可用分类" }
            db.update("categories", ContentValues().apply {
                put("name", trimmed)
                put("archived", if (archived) 1 else 0)
            }, "id = ?", arrayOf(existing.id))
        }
    }

    fun writePortrait(body: String) = transaction { db -> putPortrait(db, body) }

    fun restore(snapshot: JournalSnapshot) = transaction { db ->
        db.delete("records", null, null)
        db.delete("categories", null, null)
        snapshot.categories.forEach { insertCategory(db, it) }
        snapshot.records.forEach { db.insertOrThrow("records", null, recordValues(it)) }
        putPortrait(db, snapshot.portrait)
    }

    private fun readCategories(db: SQLiteDatabase): List<Category> =
        db.rawQuery("SELECT id, name, archived, position FROM categories ORDER BY position, id", null)
            .collect { Category(getString(0), getString(1), getInt(2) != 0, getInt(3)) }

    private fun insertCategory(db: SQLiteDatabase, item: Category) {
        db.insertOrThrow("categories", null, ContentValues().apply {
            put("id", item.id)
            put("name", item.name)
            put("archived", if (item.archived) 1 else 0)
            put("position", item.position)
        })
    }

    private fun writeReflection(db: SQLiteDatabase, item: Reflection) {
        val values = recordValues(item)
        if (db.update("records", values, "id = ?", arrayOf(item.id)) == 0) {
            db.insertOrThrow("records", null, values)
        }
    }

    private fun recordValues(item: Reflection) = ContentValues().apply {
        put("id", item.id)
        put("occurred", item.occurredAtMillis)
        put("title", item.title)
        put("category", item.categoryId)
        put("description", item.description)
        put("reflection", item.reflection)
        put("action", item.action)
        put("created", item.createdAtMillis)
        put("updated", item.updatedAtMillis)
    }

    private fun putPortrait(db: SQLiteDatabase, body: String) {
        check(db.insertWithOnConflict("portrait", null, ContentValues().apply {
            put("id", 1)
            put("body", body)
        }, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }

    private fun <T> transaction(action: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = action(db)
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }
}

private fun <T> Cursor.collect(read: Cursor.() -> T): List<T> = use {
    buildList { while (moveToNext()) add(read(this@collect)) }
}
