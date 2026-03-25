package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.*
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import kotlinx.coroutines.flow.Flow

abstract class VideoGeneratorUi {

    abstract suspend fun awaitGenerationRequest(): VideoGenerationRequest

    abstract suspend fun showGenerationProgress(
        framesGenerationProgress: FramesGenerationProgress,
        videoEncodingProgress: Flow<FfmpegProgressLine>
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
