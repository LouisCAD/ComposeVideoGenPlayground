@file:OptIn(ExperimentalMaterial3Api::class)

package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.*
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.extensions.quitOnceComplete
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
}

@Composable
private fun PreviewWindow(
    size: DpSize = DpSize(360.dp, 200.dp),
    onCloseRequest: () -> Unit,
    onGoRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val windowState = rememberWindowState(size = size, position = WindowPosition.Aligned(Alignment.Center))
    LaunchedEffect(size) {
        windowState.size = size
        windowState.position = WindowPosition.Aligned(Alignment.Center)
    }
    Window(
        onCloseRequest = {}, // Can't be called since decorations are disabled.
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        transparent = true
    ) {
        WindowDraggableArea(
            Modifier.border(1.dp, Color(0xFF_C800FF))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onCloseRequest,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(AppDefaults.icons.Close, contentDescription = null)
                }
                IconButton(
                    onClick = onGoRequest,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(AppDefaults.icons.PlayArrow, contentDescription = null)
                }
                content()
            }
        }
    }
}

@Composable
private fun MainScreen(
    onMediaGeneratorClicked: (MediaGeneratorItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Video Gen") },
            )
        }
    ) { contentPadding ->
        MediaGeneratorList(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            onItemClicked = onMediaGeneratorClicked,
        )
    }
}

@Composable
private fun MediaGenSetup(mediaGenApp: MediaGenApp, item: MediaGeneratorItem) {
    when (val content = item.content) {
        is MediaGeneratorItem.Content.CounterBasedVideo -> VideoGenSetup(
            name = item.name,
            initialSize = (item.defaultSize.toSize() * item.defaultDensity).roundToIntSize(),
            initialDensity = 1f,
            contentToRecord = { sortedTriggerNanos ->
                content.content(rememberIncrementCounter(sortedTriggerNanos))
            },
            onGenRequested = { mediaGenApp.addComposableToRecord(it) }
        )
    }
}

@Composable
private fun ApplicationScope.MediaGenTray(mediaGenApp: MediaGenApp) {
    val mainWindowState = rememberWindowState(size = DpSize(480.dp, 640.dp))
    var showGeneratorList by remember { mutableStateOf(true) }
    var videoGenToPreview: MediaGeneratorItem? by remember { mutableStateOf(null) }
    var videoGenToSetup: MediaGeneratorItem? by remember { mutableStateOf(null) }

    if (showGeneratorList) Window(
        onCloseRequest = { showGeneratorList = false },
        state = mainWindowState
    ) {
        MainScreen { mediaGeneratorItem ->
            when(mediaGeneratorItem) {
                videoGenToSetup -> {
                    videoGenToSetup = null
                    videoGenToPreview = null
                }
                videoGenToPreview -> videoGenToSetup = mediaGeneratorItem
                else -> videoGenToPreview = mediaGeneratorItem
            }
        }
    }

    videoGenToSetup?.let {
        Window(onCloseRequest = { videoGenToSetup = null }) {
            WindowDraggableArea { MediaGenSetup(mediaGenApp, it) }
        }
    }
    videoGenToPreview?.let {
        PreviewWindow(
            size = it.defaultSize.run { DpSize(width.dp, height.dp) },
            onCloseRequest = { videoGenToPreview = null },
            onGoRequest = { videoGenToSetup = it },
            content = { it.preview() }
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
        Item(
            text = "Show video generators",
            enabled = !showGeneratorList || mainWindowState.isMinimized,
            onClick = { showGeneratorList = true; mainWindowState.isMinimized = false },
        )
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
