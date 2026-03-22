package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.ui.VideoGeneratorUiImpl
import splitties.coroutines.rememberCallableState
import splitties.coroutines.repeatWhileActive

fun applicationDraft() = application {
//    val trayState = rememberTrayState()
//    Tray(painterResource(Res.drawable.compose_multiplatform), trayState, tooltip = "Compose Video Gen") {
//        Item("About", onClick = {})
//    }
    var isSecondWindowOpen by remember { mutableStateOf(false) }
    var isTestWindowOpen by remember { mutableStateOf(false) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "ComposeVideoGenPlayground",
    ) {
        val ui = remember { VideoGeneratorUiImpl() }
        LaunchedEffect(Unit) { handleVideoGeneration(ui) }
        ui.Content()
    }
    if (false) Window(
        onCloseRequest = ::exitApplication,
        title = "ComposeVideoGenPlayground",
    ) {
        Box {
            App()
            if (isSecondWindowOpen.not()) Button(onClick = { isSecondWindowOpen = true}) {
                Text("Open second window")
            }
            if (isTestWindowOpen.not()) Button(onClick = { isTestWindowOpen = true}) {
                Text("Open test window")
            }
        }
    }
    if (isTestWindowOpen) Window(
        state = rememberWindowState(),
        onCloseRequest = { isTestWindowOpen = false },
        title = "Test window",
    ) {
        MaterialTheme {
            Surface {
                CounterOverlay()
            }
        }
    }
    val minimizeRequests = rememberCallableState<Unit>()
    if (isSecondWindowOpen) Window(
        onCloseRequest = minimizeRequests,
        title = "Video Generator prototype!",
    ) {
        MenuBar {
            Menu("🪟") {
                Item("Close", onClick = minimizeRequests)
                Item("Quit", onClick = ::exitApplication)
            }
        }
        LaunchedEffect(Unit) {
            repeatWhileActive {
                minimizeRequests.awaitOneCall()
                window.isMinimized = true
            }
        }
        LegacyMvpVideoGenerator() {
            MaterialTheme(colors = lightColors()) {
                ProvideTextStyle(MaterialTheme.typography.h2.copy(color = Color.White)) {
                    CounterOverlay()
                }
            }
        }
    }
}
