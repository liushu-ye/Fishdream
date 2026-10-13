package com.yudemeng.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 纯框架 SQLite（不依赖 Room / 任何第三方库），完全离线可用。
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                policy TEXT NOT NULL,
                expires_at INTEGER,
                trashed_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_expires ON notes(expires_at)")
        db.execSQL("CREATE INDEX idx_notes_trashed ON notes(trashed_at)")
        db.execSQL("CREATE INDEX idx_notes_updated ON notes(updated_at)")

        db.execSQL(
            """
            CREATE TABLE tags(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE note_tags(
                note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                PRIMARY KEY(note_id, tag_id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_note_tags_tag ON note_tags(tag_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 第一版，暂无迁移
    }

    companion object {
        const val DB_NAME = "fishdream.db"
        const val DB_VERSION = 1
    }
}
