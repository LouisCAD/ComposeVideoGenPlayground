package com.louiscad.playground.compose.videogen

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.ui.components.MediaGenJobList
import com.louiscad.playground.compose.videogen.ui.components.VideoGenSetup
import composevideogenplayground.composeapp.generated.resources.Res
import composevideogenplayground.composeapp.generated.resources.video_template_24dp
import kotlinx.coroutines.CoroutineScope
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
}

@Composable
private fun ApplicationScope.MediaGenTray(mediaGenApp: MediaGenApp) {
    val mainWindowState = rememberWindowState()
    var showVideoGenSetup by remember { mutableStateOf(true) }

    if (showVideoGenSetup) Window(onCloseRequest = { showVideoGenSetup = false }) {
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
