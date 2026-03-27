package com.louiscad.playground.compose.videogen.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.FrameWindowScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.Desktop
import java.awt.desktop.QuitResponse
import kotlin.coroutines.resume

fun FrameWindowScope.redDotForModifiedDocument(enabled: Boolean) {
    window.rootPane.putClientProperty("Window.documentModified", enabled)
}

@Composable
fun quitOnceComplete(canQuit: () -> Boolean) {
    val canQuitFlow = remember(canQuit) { snapshotFlow { canQuit() } }
    val desktop = Desktop.getDesktop()
    LaunchedEffect(Unit) {
        canQuitFlow.collect { canQuit ->
            if (canQuit) desktop.enableSuddenTermination() else desktop.disableSuddenTermination()
        }
    }
    LaunchedEffect(Unit) {
        val quitResponse = desktop.awaitQuitRequest()
        canQuitFlow.first { it }
        quitResponse.performQuit()
    }
}

private suspend fun Desktop.awaitQuitRequest(): QuitResponse {
    try {
        return suspendCancellableCoroutine { continuation ->
            setQuitHandler { _, response ->
                setQuitHandler(null)
                continuation.resume(response)
            }
        }
    } finally {
        setQuitHandler(null)
    }
}
