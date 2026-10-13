package com.yudemeng.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yudemeng.app.R
import com.yudemeng.app.data.ExpiryPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(state: AppState, noteId: Long?) {
    val host = remember { SnackbarHostState() }
    rememberMessageEffect(state, host)

    val initial = state.editorNote
    var text by remember(noteId) { mutableStateOf(initial?.content ?: "") }
    // 默认保留时间：一周
    var policy by remember(noteId) { mutableStateOf(initial?.policy ?: ExpiryPolicy.ONE_WEEK) }
    var selectedTags by remember(noteId) { mutableStateOf(state.editorTagIds.toSet()) }

    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    fun commit() {
        val content = text.trim()
        if (content.isEmpty()) {
            if (noteId != null) state.trash(noteId) else state.openList()
        } else {
            state.saveNote(noteId, content, policy, selectedTags.toList())
        }
    }

    BackHandler { commit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "新笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = { commit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回并保存")
                    }
                },
                actions = {
                    if (noteId != null) {
                        IconButton(onClick = { state.trash(noteId) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "移入回收站")
                        }
                    }
                    IconButton(onClick = { commit() }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(host) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("写点什么…") },
            )

            Text(
                text = stringResource(R.string.retention_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExpiryPolicy.entries.forEach { candidate ->
                    FilterChip(
                        selected = policy == candidate,
                        onClick = { policy = candidate },
                        label = { Text(stringResource(candidate.labelRes)) },
                    )
                }
            }
            if (policy == ExpiryPolicy.TODAY) {
                Text(
                    text = "提示：如果临近午夜创建，「今天」可能很快到期。编辑笔记会按当前时间重新计算到期时刻。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(text = "标签", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.tags.forEach { tag ->
                    val picked = selectedTags.contains(tag.id)
                    FilterChip(
                        selected = picked,
                        onClick = {
                            selectedTags = if (picked) selectedTags - tag.id else selectedTags + tag.id
                        },
                        label = { Text(tag.name) },
                    )
                }
                AssistChip(
                    onClick = { showNewTagDialog = true },
                    label = { Text("＋ 新建标签") },
                )
            }
        }
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("新建标签") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    singleLine = true,
                    placeholder = { Text("标签名") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.createTag(newTagName)
                    newTagName = ""
                    showNewTagDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) { Text("取消") }
            },
        )
    }
}
