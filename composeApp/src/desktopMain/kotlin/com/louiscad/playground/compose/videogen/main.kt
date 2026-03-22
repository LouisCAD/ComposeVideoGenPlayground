package com.louiscad.playground.compose.videogen

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import composevideogenplayground.composeapp.generated.resources.Res
import composevideogenplayground.composeapp.generated.resources.video_template_24dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource

object AppDefaults {
    val icons = Icons.Rounded
    val iconsAutoMirrored = Icons.AutoMirrored.Rounded
}

private val mediaGenApp: MediaGenApp = MediaGenAppImpl(CoroutineScope(Dispatchers.Default))

fun main() = application {
    MediaGenTray(mediaGenApp)
}

@Composable
private fun ApplicationScope.MediaGenTray(mediaGenApp: MediaGenApp) {
    val mainWindowState = rememberWindowState()

    val trayState = rememberTrayState()
    Tray(painterResource(Res.drawable.video_template_24dp), trayState) {
        Item("Try notification", onClick = {
            trayState.sendNotification(Notification(title = "Yolo?", message = "Hello World!", Notification.Type.Info))
        })
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
