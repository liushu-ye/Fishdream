package com.yudemeng.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yudemeng.app.data.NoteRepository
import com.yudemeng.app.ui.App
import com.yudemeng.app.ui.AppState
import com.yudemeng.app.ui.FishDreamTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FishDreamTheme {
                val scope = rememberCoroutineScope()
                val state = remember {
                    AppState(NoteRepository(applicationContext), scope)
                }
                App(state)
            }
        }
    }
}
