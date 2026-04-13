package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.louiscad.playground.compose.videogen.ui.components.MediaGeneratorItem

private val windowPosition = WindowPosition.Aligned(Alignment.Center)

@Composable
fun PreviewWindow(
    item: MediaGeneratorItem,
    onCloseRequest: () -> Unit,
    onGoRequest: () -> Unit,
) {
   PreviewWindow(
       size = item.defaultSize.run { DpSize(width.dp, height.dp) },
       onCloseRequest = onCloseRequest,
       onGoRequest = onGoRequest,
       content = { item.preview() }
   )
}

@Composable
private fun PreviewWindow(
    size: DpSize = DpSize(360.dp, 200.dp),
    onCloseRequest: () -> Unit,
    onGoRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val windowState = rememberWindowState(size = size, position = windowPosition)
    LaunchedEffect(size) {
        windowState.size = size
        windowState.position = windowPosition
    }
    Window(
        onCloseRequest = {}, // Can't be called since decorations are disabled.
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        transparent = true
    ) {
        val toolkit = window.toolkit
        toolkit.screenSize
        toolkit.screenResolution
        toolkit.getScreenInsets(window.graphicsConfiguration)
        //TODO: Update windowState.size based on calculations from item.defaultSize, screen size.
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
