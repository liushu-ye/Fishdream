package com.yudemeng.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App(state: AppState) {
    LaunchedEffect(Unit) { state.refresh() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val current = state.screen) {
            is Screen.NoteList -> NoteListScreen(state)
            is Screen.Editor -> EditorScreen(state, current.noteId)
            is Screen.Trash -> TrashScreen(state)
            is Screen.Tags -> TagsScreen(state)
        }
    }
}

/** 把 state.message 显示成 Snackbar，显示后清空。 */
@Composable
fun rememberMessageEffect(state: AppState, host: SnackbarHostState) {
    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            host.showSnackbar(text)
            state.message = null
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
