package com.louiscad.playground.compose.videogen.core

import kotlin.time.Duration

data class FramesRecordingDurationSummary(
    val framesGeneration: Duration,
    val encodingAndWritingGeneration: Duration,
)
