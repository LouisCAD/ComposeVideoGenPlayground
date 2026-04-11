package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.extensions.quitOnceComplete
import com.louiscad.playground.compose.videogen.library.MercedesVsBacchettaPreview
import com.louiscad.playground.compose.videogen.ui.components.MediaGenJobList
import com.louiscad.playground.compose.videogen.ui.components.MediaGeneratorItem
import com.louiscad.playground.compose.videogen.ui.components.MediaGeneratorList
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
    val mainWindowState = rememberWindowState(size = DpSize(400.dp, 600.dp))
    var showGeneratorList by remember { mutableStateOf(true) }
    var videoGenToSetup: MediaGeneratorItem? by remember { mutableStateOf(null) }

    if (showGeneratorList) Window(onCloseRequest = { showGeneratorList = false }, state = mainWindowState) {
        MediaGeneratorList(
            modifier = Modifier.fillMaxSize(),
            onItemClicked = { mediaGenItem -> videoGenToSetup = mediaGenItem }
        )
    }

    when (val mediaGenItem = videoGenToSetup) {
        null -> Unit
        else -> Window(onCloseRequest = { videoGenToSetup = null }) {
            WindowDraggableArea {
                VideoGenSetup(
                    name = mediaGenItem.name,
                    initialSize = mediaGenItem.defaultSize,
                    contentToRecord = { sortedTriggerNanos ->
                        when (val content = mediaGenItem.content) {
                            is MediaGeneratorItem.Content.CounterBasedVideo -> {
                                content.content(rememberIncrementCounter(sortedTriggerNanos))
                            }
                        }
                    },
                    onGenRequested = { mediaGenApp.addComposableToRecord(it) }
                )
            }
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
        Item("Show video generators", enabled = !showGeneratorList, onClick = { showGeneratorList = true })
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
