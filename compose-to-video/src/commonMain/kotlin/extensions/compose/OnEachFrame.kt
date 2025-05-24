package com.louiscad.playground.compose.videogen.core.extensions.compose

import androidx.compose.runtime.withFrameNanos
import splitties.coroutines.repeatWhileActive

suspend fun onEachFrame(onFrame: (frameTimeNanos: Long) -> Unit): Nothing {
    repeatWhileActive {
        withFrameNanos(onFrame)
    }
}
