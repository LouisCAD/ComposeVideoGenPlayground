package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.extensions.quitOnceComplete
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.library.MercedesVsBacchettaPreview
import com.louiscad.playground.compose.videogen.ui.components.MediaGenJobList
import com.louiscad.playground.compose.videogen.ui.components.VideoGenSetup
import composevideogenplayground.composeapp.generated.resources.Res
import composevideogenplayground.composeapp.generated.resources.video_template_24dp
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource

object AppDefaults {
    val icons = Icons.Rounded
    val iconsAutoMirrored = Icons.AutoMirrored.Rounded
}


fun main() = application {
    val defaultScope = rememberCoroutineScope { Dispatchers.Default }
    val mediaGenApp: MediaGenApp = remember { MediaGenAppImpl(defaultScope) }
    MediaGenTray(mediaGenApp)
    quitOnceComplete { mediaGenApp.isGeneratingMedia.not() }
    PreviewWindow()
}

@Composable
private fun ApplicationScope.PreviewWindow() {
    val windowState = rememberWindowState(size = DpSize(360.dp, 200.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        transparent = true
    ) {
        WindowDraggableArea(
            Modifier.border(1.dp, Color.Cyan)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MercedesVsBacchettaPreview()
            }
        }
    }
}

@Composable
private fun ApplicationScope.MediaGenTray(mediaGenApp: MediaGenApp) {
    val mainWindowState = rememberWindowState()
    var showVideoGenSetup by remember { mutableStateOf(false) }

    if (showVideoGenSetup) Window(onCloseRequest = { showVideoGenSetup = false }) {
        WindowDraggableArea {
            VideoGenSetup(
                contentToRecord = { sortedTriggerNanos ->
                    ProvideTextStyle(
                        MaterialTheme.typography.h2.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        CounterOverlay(rememberIncrementCounter(sortedTriggerNanos))
                    }
                },
                onGenRequested = { mediaGenApp.addComposableToRecord(it) }
            )
        }
    }

    var showJobs by remember { mutableStateOf(false) }
    if (showJobs) Window(onCloseRequest = { showJobs = false }) { MediaGenJobList(mediaGenApp) }

    val trayState = rememberTrayState()
    Tray(painterResource(Res.drawable.video_template_24dp), trayState) {
        Item("Try notification", onClick = {
            trayState.sendNotification(Notification(title = "Yolo?", message = "Hello World!", Notification.Type.Info))
        })
        Item("Show jobs window", enabled = !showJobs, onClick = { showJobs = true })
        Item("Show video gen window", enabled = !showVideoGenSetup, onClick = { showVideoGenSetup = true })
        MediaGenAwareQuitItem(mediaGenApp, ::exitApplication)
    }
}

@Composable
private fun MenuScope.MediaGenAwareQuitItem(mediaGenApp: MediaGenApp, exitApplication: () -> Unit) {
    if (mediaGenApp.isGeneratingMedia) {
        Menu("Force Quit") { Menu("Abort ongoing media gen") { Item("Quit anyway", onClick = exitApplication) } }
    } else {
        Item("Quit", onClick = exitApplication)
    }
}
