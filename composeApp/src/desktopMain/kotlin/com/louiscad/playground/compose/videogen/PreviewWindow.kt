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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.louiscad.playground.compose.videogen.components.ProvideRelativeDensity
import com.louiscad.playground.compose.videogen.extensions.screenInsetsFlow
import com.louiscad.playground.compose.videogen.extensions.screenSizeFlow
import com.louiscad.playground.compose.videogen.ui.components.MediaGeneratorItem
import kotlinx.coroutines.flow.combine

private val windowPosition = WindowPosition.Aligned(Alignment.CenterEnd)

@Composable
fun PreviewWindow(
    item: MediaGeneratorItem,
    onCloseRequest: () -> Unit,
    onGoRequest: () -> Unit,
) {
    val size = item.defaultSize.run {
        DpSize((width * item.previewScale).dp, (height * item.previewScale).dp)
    }
    val content: @Composable () -> Unit = { item.preview() }
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
        with(LocalDensity.current) {
            val s = windowState.size.toSize()
            SideEffect {
                val w = window.toolkit.screenSize.width * window.graphicsConfiguration.defaultTransform.scaleX
                println(s)
                println(w)
            }
        }
        val toolkit = window.toolkit
        LaunchedEffect(toolkit) {
            window.graphicsConfiguration.defaultTransform.scaleX
            combine(
                toolkit.screenSizeFlow(),
                window.screenInsetsFlow()
            ) { screenSize, screenInsets ->
                screenSize to screenInsets
            }.collect { (screenSize, screenInsets) ->
                println("------")
                println("screenSize: $screenSize.")
                println("screenInsets: $screenInsets")
            }
        }

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
                ProvideRelativeDensity(Density(item.previewScale)) {
                    content()
                }
            }
        }
    }
}
