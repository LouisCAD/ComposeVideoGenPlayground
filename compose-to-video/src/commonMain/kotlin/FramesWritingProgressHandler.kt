package com.louiscad.playground.compose.videogen.core

fun interface FramesWritingProgressHandler {

    /**
     * This is deliberately not an observable type, because updates are expected to arrive faster
     * than a human could perceive, and faster than the screen refresh-rate.
     *
     * The right way is therefore to poll [getCurrentWrittenFrames] on each frame,
     * with a loop and [withFrameNanos], for a UI made with Compose, that is.
     */
    suspend fun handleProgress(
        totalFrames: Int,
        getCurrentWrittenFrames: () -> Int
    ): Nothing
}
