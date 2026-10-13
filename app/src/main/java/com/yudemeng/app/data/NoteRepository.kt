package com.yudemeng.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.yudemeng.app.domain.Expiry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock

/**
 * 笔记仓库。所有时间比较基于 epoch millis：
 * 过期不是一个"定时事件"，而是"查询时惰性结算"的结果。
 */
class NoteRepository(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val appContext = context.applicationContext
    private val helper = DbHelper(appContext)

    private fun now(): Long = clock.millis()
    private val zone get() = clock.zone

    // ---------------------------------------------------------------- 过期结算

    /** 把已到期的活动笔记移入回收站，返回受影响条数。App 每次刷新列表前调用。 */
    fun sweepExpired(): Int {
        val timestamp = now()
        val values = ContentValues().apply { put("trashed_at", timestamp) }
        return helper.writableDatabase.update(
            "notes",
            values,
            "trashed_at IS NULL AND expires_at IS NOT NULL AND expires_at <= ?",
            arrayOf(timestamp.toString()),
        )
    }

    // ---------------------------------------------------------------- 笔记读写

    fun createNote(content: String, policy: ExpiryPolicy, tagIds: List<Long>): Long {
        val timestamp = now()
        val expiresAt = Expiry.computeExpiresAt(policy, timestamp, zone)
        val values = ContentValues().apply {
            put("content", content)
            put("created_at", timestamp)
            put("updated_at", timestamp)
            put("policy", policy.name)
            if (expiresAt == null) putNull("expires_at") else put("expires_at", expiresAt)
            putNull("trashed_at")
        }
        val id = helper.writableDatabase.insert("notes", null, values)
        if (id > 0) setNoteTags(id, tagIds)
        return id
    }

    /** 编辑笔记：按当前时间重算过期时刻（约定：编辑即续期）。 */
    fun updateNote(id: Long, content: String, policy: ExpiryPolicy, tagIds: List<Long>) {
        val timestamp = now()
        val expiresAt = Expiry.computeExpiresAt(policy, timestamp, zone)
        val values = ContentValues().apply {
            put("content", content)
            put("updated_at", timestamp)
            put("policy", policy.name)
            if (expiresAt == null) putNull("expires_at") else put("expires_at", expiresAt)
        }
        helper.writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
        setNoteTags(id, tagIds)
    }

    fun trashNote(id: Long) {
        val values = ContentValues().apply { put("trashed_at", now()) }
        helper.writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
    }

    /** 恢复：按约定恢复后转为永久保存。 */
    fun restoreNote(id: Long) {
        val values = ContentValues().apply {
            putNull("trashed_at")
            put("policy", ExpiryPolicy.FOREVER.name)
            putNull("expires_at")
        }
        helper.writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteForever(id: Long) {
        helper.writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    /** 清空回收站，返回清除条数。 */
    fun emptyTrash(): Int =
        helper.writableDatabase.delete("notes", "trashed_at IS NOT NULL", null)

    fun getNote(id: Long): Note? {
        helper.readableDatabase
            .query("notes", null, "id = ?", arrayOf(id.toString()), null, null, null)
            .use { cursor ->
                if (cursor.moveToFirst()) return cursor.toNote().copy(tags = tagsOf(id))
            }
        return null
    }

    fun listActive(search: String, tagId: Long?): List<Note> {
        val clauses = ArrayList<String>()
        val args = ArrayList<String>()
        clauses.add("trashed_at IS NULL")

        if (search.isNotBlank()) {
            clauses.add(
                "(content LIKE ? OR id IN (" +
                    "SELECT nt.note_id FROM note_tags nt JOIN tags t ON t.id = nt.tag_id " +
                    "WHERE t.name LIKE ?))"
            )
            val like = "%" + search.trim() + "%"
            args.add(like)
            args.add(like)
        }
        if (tagId != null) {
            clauses.add("id IN (SELECT note_id FROM note_tags WHERE tag_id = ?)")
            args.add(tagId.toString())
        }
        return queryNotes(clauses.joinToString(" AND "), args.toTypedArray(), "updated_at DESC")
    }

    fun listTrash(): List<Note> =
        queryNotes("trashed_at IS NOT NULL", null, "trashed_at DESC")

    private fun queryNotes(
        selection: String?,
        selectionArgs: Array<String>?,
        orderBy: String,
    ): List<Note> {
        val result = ArrayList<Note>()
        helper.readableDatabase
            .query("notes", null, selection, selectionArgs, null, null, orderBy)
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val note = cursor.toNote()
                    result.add(note.copy(tags = tagsOf(note.id)))
                }
            }
        return result
    }

    /** 主列表 / 回收站条数。 */
    fun counts(): Pair<Int, Int> {
        val db = helper.readableDatabase
        fun countOf(where: String): Int =
            db.rawQuery("SELECT COUNT(*) FROM notes WHERE $where", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        return countOf("trashed_at IS NULL") to countOf("trashed_at IS NOT NULL")
    }

    // ---------------------------------------------------------------- 标签

    fun listTags(): List<TagCount> {
        val sql = """
            SELECT t.id, t.name,
                   (SELECT COUNT(*) FROM note_tags nt JOIN notes n ON n.id = nt.note_id
                    WHERE nt.tag_id = t.id AND n.trashed_at IS NULL) AS cnt
            FROM tags t
            ORDER BY cnt DESC, t.name ASC
        """.trimIndent()
        val out = ArrayList<TagCount>()
        helper.readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                out.add(TagCount(cursor.getLong(0), cursor.getString(1), cursor.getInt(2)))
            }
        }
        return out
    }

    /** 创建标签；已存在则返回原有 id。 */
    fun createTag(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val db = helper.writableDatabase
        db.query("tags", arrayOf("id"), "name = ?", arrayOf(trimmed), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        val values = ContentValues().apply {
            put("name", trimmed)
            put("created_at", now())
        }
        return db.insert("tags", null, values)
    }

    fun deleteTag(id: Long) {
        helper.writableDatabase.delete("tags", "id = ?", arrayOf(id.toString()))
    }

    fun tagsOf(noteId: Long): List<Tag> {
        val out = ArrayList<Tag>()
        val sql = """
            SELECT t.id, t.name FROM tags t
            JOIN note_tags nt ON nt.tag_id = t.id
            WHERE nt.note_id = ?
            ORDER BY t.name ASC
        """.trimIndent()
        helper.readableDatabase.rawQuery(sql, arrayOf(noteId.toString())).use { cursor ->
            while (cursor.moveToNext()) out.add(Tag(cursor.getLong(0), cursor.getString(1)))
        }
        return out
    }

    fun tagIdsOf(noteId: Long): List<Long> = tagsOf(noteId).map { it.id }

    fun setNoteTags(noteId: Long, tagIds: List<Long>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("note_tags", "note_id = ?", arrayOf(noteId.toString()))
            tagIds.distinct().forEach { tagId ->
                val values = ContentValues().apply {
                    put("note_id", noteId)
                    put("tag_id", tagId)
                }
                db.insertWithOnConflict("note_tags", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---------------------------------------------------------------- 备份

    /** 导出全部笔记（含回收站）为 JSON，返回导出条数。 */
    fun exportTo(uri: Uri): Int {
        val notes = queryNotes(null, null, "created_at ASC")
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject().apply {
                    put("content", note.content)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                    put("policy", note.policy.name)
                    put("expiresAt", note.expiresAt ?: JSONObject.NULL)
                    put("trashedAt", note.trashedAt ?: JSONObject.NULL)
                    put("tags", JSONArray(note.tags.map { it.name }))
                }
            )
        }
        val root = JSONObject().apply {
            put("app", "鱼的梦")
            put("schema", 1)
            put("exportedAt", now())
            put("notes", array)
        }
        val stream = appContext.contentResolver.openOutputStream(uri) ?: return 0
        stream.use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            out.flush()
        }
        return array.length()
    }

    /** 从 JSON 备份导入（追加，不覆盖已有数据），返回导入条数。 */
    fun importFrom(uri: Uri): Int {
        val text = appContext.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return 0

        val array = JSONObject(text).optJSONArray("notes") ?: return 0
        val db = helper.writableDatabase
        var imported = 0

        db.beginTransaction()
        try {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val content = item.optString("content")
                if (content.isEmpty()) continue

                val createdAt = item.optLong("createdAt", now())
                val policy = ExpiryPolicy.fromKey(item.optString("policy", ExpiryPolicy.FOREVER.name))
                val expiresAt = if (item.isNull("expiresAt")) null else item.optLong("expiresAt")
                val trashedAt = if (item.isNull("trashedAt")) null else item.optLong("trashedAt")

                val values = ContentValues().apply {
                    put("content", content)
                    put("created_at", createdAt)
                    put("updated_at", item.optLong("updatedAt", createdAt))
                    put("policy", policy.name)
                    if (expiresAt == null) putNull("expires_at") else put("expires_at", expiresAt)
                    if (trashedAt == null) putNull("trashed_at") else put("trashed_at", trashedAt)
                }
                val noteId = db.insert("notes", null, values)
                if (noteId <= 0) continue

                val tagArray = item.optJSONArray("tags")
                if (tagArray != null) {
                    for (tagIndex in 0 until tagArray.length()) {
                        val tagName = tagArray.optString(tagIndex).trim()
                        if (tagName.isEmpty()) continue
                        val tagId = createTag(tagName)
                        if (tagId <= 0) continue
                        val link = ContentValues().apply {
                            put("note_id", noteId)
                            put("tag_id", tagId)
                        }
                        db.insertWithOnConflict("note_tags", null, link, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
                imported++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return imported
    }
}

private fun Cursor.toNote(): Note {
    val expiresIndex = getColumnIndexOrThrow("expires_at")
    val trashedIndex = getColumnIndexOrThrow("trashed_at")
    return Note(
        id = getLong(getColumnIndexOrThrow("id")),
        content = getString(getColumnIndexOrThrow("content")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        policy = ExpiryPolicy.fromKey(getString(getColumnIndexOrThrow("policy"))),
        expiresAt = if (isNull(expiresIndex)) null else getLong(expiresIndex),
        trashedAt = if (isNull(trashedIndex)) null else getLong(trashedIndex),
    )
}
