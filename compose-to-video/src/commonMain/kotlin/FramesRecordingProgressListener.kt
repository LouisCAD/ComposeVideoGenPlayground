package com.louiscad.playground.compose.videogen.core

import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers

/**
 * ## Important notice:
 *
 * Expect [onFrameEncoded] and [onFrameWritten] to be **called in parallel**,
 * from **different threads** (belonging to [Dispatchers.IO]).
 *
 * Make sure their implementations are race-safe by using atomics, channels, or locks.
 */
fun interface FramesRecordingProgressListener {
    fun onFrameEncoded(index: Int, duration: Duration) = Unit
    fun onFrameWritten(index: Int, duration: Duration)
}
