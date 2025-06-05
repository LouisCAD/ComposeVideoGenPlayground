package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.mutableIntStateOf
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.FramesWritingProgressHandler
import com.louiscad.playground.compose.videogen.core.extensions.compose.onEachFrame
import com.louiscad.playground.compose.videogen.core.recordComposableAsVideo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import splitties.coroutines.raceOf
import splitties.coroutines.repeatWhileActive

suspend fun handleVideoGeneration(ui: VideoGeneratorUi): Nothing = repeatWhileActive {
    val genRequest = ui.awaitGenerationRequest()
    val framesGenProgress = CompletableDeferred<VideoGeneratorUi.FramesGenerationProgress>()
    val videoEncodingProgressChannel = Channel<FfmpegProgressLine>(capacity = Channel.CONFLATED)
    raceOf({
        ui.showGenerationProgress(
            framesGenerationProgress = framesGenProgress.await(),
            videoEncodingProgress = videoEncodingProgressChannel.consumeAsFlow()
        )
    }, {
        recordComposableAsVideo(
            request = genRequest,
            progressHandler = { total, getWrittenFrames ->
                val writtenFramesState = mutableIntStateOf(getWrittenFrames())
                VideoGeneratorUi.FramesGenerationProgress(
                    totalFrames = total,
                    writtenFramesState = writtenFramesState
                ).also { framesGenProgress.complete(it) }
                onEachFrame {
                    writtenFramesState.value = getWrittenFrames()
                }
            },
            progressChannel = videoEncodingProgressChannel
        )
    })
}

private suspend fun recordComposableAsVideo(
    request: VideoGeneratorUi.GenerationRequest,
    progressHandler: FramesWritingProgressHandler,
    progressChannel: SendChannel<FfmpegProgressLine>
) {
    recordComposableAsVideo(
        size = request.size,
        density = request.density,
        framesPerSecond = request.framesPerSecond,
        outputDir = request.outputDir,
        outputFileNameWithoutExtension = request.outputFileNameWithoutExtension,
        duration = request.duration,
        progressHandler = progressHandler,
        convertingWebpsToVideo = { terminalOutput ->
            terminalOutput.collect { progressLine ->
                progressChannel.send(progressLine)
            }
        },
        content = request.getContent()
    )
}
