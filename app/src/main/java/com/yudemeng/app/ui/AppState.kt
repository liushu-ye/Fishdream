package com.yudemeng.app.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yudemeng.app.data.ExpiryPolicy
import com.yudemeng.app.data.Note
import com.yudemeng.app.data.NoteRepository
import com.yudemeng.app.data.TagCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object NoteList : Screen
    data class Editor(val noteId: Long?) : Screen
    data object Trash : Screen
    data object Tags : Screen
}

/**
 * 极简状态容器：数据库读写放在 IO 线程，结果回主线程写入 Compose 状态。
 * （本项目不依赖 lifecycle-viewmodel-compose，因此不走 ViewModel。）
 */
class AppState(
    private val repo: NoteRepository,
    private val scope: CoroutineScope,
) {

    var screen by mutableStateOf<Screen>(Screen.NoteList)
        private set

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var trashed by mutableStateOf<List<Note>>(emptyList())
        private set

    var tags by mutableStateOf<List<TagCount>>(emptyList())
        private set

    var query by mutableStateOf("")
    var tagFilter by mutableStateOf<Long?>(null)
    var message by mutableStateOf<String?>(null)

    var editorNote by mutableStateOf<Note?>(null)
        private set

    var editorTagIds by mutableStateOf<List<Long>>(emptyList())
        private set

    /** 重新结算过期笔记并刷新三个列表。 */
    fun refresh() {
        val currentQuery = query
        val currentTag = tagFilter
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.sweepExpired()
                Triple(
                    repo.listActive(currentQuery, currentTag),
                    repo.listTrash(),
                    repo.listTags(),
                )
            }
            notes = result.first
            trashed = result.second
            tags = result.third
        }
    }

    // ------------------------------------------------------------ 导航

    fun openList() {
        screen = Screen.NoteList
        refresh()
    }

    fun openTrash() {
        screen = Screen.Trash
        refresh()
    }

    fun openTags() {
        screen = Screen.Tags
        refresh()
    }

    fun openEditor(noteId: Long?) {
        if (noteId == null) {
            editorNote = null
            editorTagIds = emptyList()
            screen = Screen.Editor(null)
            return
        }
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                repo.getNote(noteId) to repo.tagIdsOf(noteId)
            }
            editorNote = loaded.first
            editorTagIds = loaded.second
            screen = Screen.Editor(noteId)
        }
    }

    // ------------------------------------------------------------ 笔记操作

    fun saveNote(
        noteId: Long?,
        content: String,
        policy: ExpiryPolicy,
        tagIds: List<Long>,
        resetCountdown: Boolean = true,
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (noteId == null) repo.createNote(content, policy, tagIds)
                else repo.updateNote(noteId, content, policy, tagIds, resetCountdown)
            }
            message = when {
                noteId == null -> "已保存"
                resetCountdown -> "已保存，倒计时已重新计算"
                else -> "已保存，倒计时延续编辑前的"
            }
            screen = Screen.NoteList
            refresh()
        }
    }

    fun trash(noteId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { repo.trashNote(noteId) }
            message = "已移入回收站"
            screen = Screen.NoteList
            refresh()
        }
    }

    fun restore(noteId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { repo.restoreNote(noteId) }
            message = "已恢复，转为永久保存"
            refresh()
        }
    }

    fun deleteForever(noteId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { repo.deleteForever(noteId) }
            message = "已永久删除"
            refresh()
        }
    }

    fun emptyTrash() {
        scope.launch {
            val removed = withContext(Dispatchers.IO) { repo.emptyTrash() }
            message = "已清空 $removed 条"
            refresh()
        }
    }

    // ------------------------------------------------------------ 标签操作

    fun createTag(name: String) {
        if (name.isBlank()) return
        scope.launch {
            withContext(Dispatchers.IO) { repo.createTag(name) }
            message = "已创建标签"
            refresh()
        }
    }

    fun deleteTag(tagId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { repo.deleteTag(tagId) }
            if (tagFilter == tagId) tagFilter = null
            message = "已删除标签"
            refresh()
        }
    }

    // ------------------------------------------------------------ 备份

    fun exportTo(uri: Uri) {
        scope.launch {
            val count = withContext(Dispatchers.IO) { repo.exportTo(uri) }
            message = if (count > 0) "已导出 $count 条笔记" else "导出失败"
        }
    }

    fun importFrom(uri: Uri) {
        scope.launch {
            val count = withContext(Dispatchers.IO) { repo.importFrom(uri) }
            message = if (count > 0) "已导入 $count 条笔记" else "没有可导入的内容"
            refresh()
        }
    }
}
