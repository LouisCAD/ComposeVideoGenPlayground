package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlin.time.Duration

data class VideoGenerationRequest(
    val outputDir: File,
    val outputFileNameWithoutExtension: String,
    val size: IntSize,
    val density: Density,
    val framesPerSecond: Int,
    val duration: Duration,
    val getContent: suspend () -> (@Composable () -> Unit),
)
