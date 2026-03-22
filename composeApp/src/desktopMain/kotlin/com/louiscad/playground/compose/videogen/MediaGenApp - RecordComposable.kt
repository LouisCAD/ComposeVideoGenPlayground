package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.louiscad.playground.compose.videogen.MediaGenJob.Status
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.extensions.compose.onEachFrame
import com.louiscad.playground.compose.videogen.ui.components.VideoGenProgressLine
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import splitties.coroutines.raceOf
import kotlin.time.TimeSource

fun MediaGenApp.addComposableToRecord(request: VideoGeneratorUi.GenerationRequest) {
    val startSignal: CompletableJob = Job()
    val statusState = mutableStateOf<Status>(Status.Enqueued(startNow = startSignal::complete))
    var framesGenerationProgress: VideoGeneratorUi.FramesGenerationProgress? by mutableStateOf(null)
    val videoEncodingProgressChannel = Channel<FfmpegProgressLine>(capacity = Channel.CONFLATED)
    val videoEncodingProgress = MutableSharedFlow<FfmpegProgressLine>(replay = 1)
    val mediaGenJob = MediaGenJob(
        title = request.outputFileNameWithoutExtension,
        statusState = statusState,
        statusRow = status@{
            VideoGenProgressLine(
                framesGenerationProgress = framesGenerationProgress ?: return@status,
                videoEncodingProgress = videoEncodingProgress,
            )
        }
    )
    addJob(mediaGenJob) {
        startSignal.join()
        var status: Status by statusState
        val timeMark = TimeSource.Monotonic.markNow()
        status = Status.Running(
            pause = null,
            timeSpent = timeMark.elapsedNow(),
            estimatedTimeRemaining = null,
            completionRatio = 0f,
        )
        raceOf({
            videoEncodingProgressChannel.receiveAsFlow().collect { line ->
                videoEncodingProgress.emit(line)
                val total = framesGenerationProgress?.totalFrames
                    ?: return@collect // Shouldn't happen.
                status = Status.Running(
                    pause = null,
                    timeSpent = timeMark.elapsedNow(),
                    estimatedTimeRemaining = null,
                    completionRatio = .5f + line.frameNumber / (total * 2f), // Second "half" is frames gen
                )
            }
        }, {
            recordComposableAsVideo(
                request = request,
                progressHandler = { total, getWrittenFrames ->
                    val writtenFramesState = mutableIntStateOf(getWrittenFrames())
                    VideoGeneratorUi.FramesGenerationProgress(
                        totalFrames = total,
                        writtenFramesState = writtenFramesState
                    ).also { framesGenerationProgress = it }
                    onEachFrame {
                        val writtenFrames = getWrittenFrames()
                        writtenFramesState.value = writtenFrames
                        status = Status.Running(
                            pause = null,
                            timeSpent = timeMark.elapsedNow(),
                            estimatedTimeRemaining = null,
                            completionRatio = writtenFrames / (total * 2f), // First "half" is frames gen
                        )
                    }
                },
                progressChannel = videoEncodingProgressChannel
            )
        })
        status = Status.Done(timeSpent = timeMark.elapsedNow())
    }
}
