package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import kotlinx.coroutines.flow.Flow
import java.io.File
import kotlin.time.Duration

abstract class VideoGeneratorUi {

    abstract suspend fun awaitGenerationRequest(): GenerationRequest

    abstract suspend fun showGenerationProgress(
        framesGenerationProgress: FramesGenerationProgress,
        videoEncodingProgress: Flow<FfmpegProgressLine>
    )

    data class GenerationRequest(
        val outputDir: File,
        val outputFileNameWithoutExtension: String,
        val size: IntSize,
        val timecodesSourceFile: File,
        val duration: Duration,
        val content: @Composable () -> Unit,
    )

    @Stable
    abstract class FramesGenerationProgress private constructor(val totalFrames: Int) {
        abstract val writtenFrames: Int

        companion object {

            @Stable
            operator fun invoke(
                totalFrames: Int,
                writtenFramesState: IntState
            ) = object : FramesGenerationProgress(totalFrames) {
                override val writtenFrames by writtenFramesState
            }
        }
    }
}
