package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.readTimecodes
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.core.toNanosOffset
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.invoke
import splitties.coroutines.CallableState
import splitties.coroutines.call
import splitties.coroutines.raceOf
import java.io.File
import kotlin.time.Duration

class VideoGeneratorUiImpl : VideoGeneratorUi() {

    @Composable
    fun Content() {
        if (startGeneratingRequest.isAwaitingCall) GenRequestSetup()
        else GenRequestInProgress()
    }

    @Composable
    private fun GenRequestSetup() = Column(
        Modifier.onPreviewKeyEvent { keyEvent ->
            (keyEvent.key == Key.Enter).also { if (it) startGeneratingRequest.call() }
        }
    ) {
        TODO()
    }

    @Composable
    private fun GenRequestInProgress() = Column {
        TODO()
    }

    private val startGeneratingRequest = CallableState<Unit>()

    private var outputDir: File? by mutableStateOf(null)
    private var outputFileNameWithoutExtension: String by mutableStateOf("generated-video")
    private var timeCodesSourceFile: File by mutableStateOf(File("timecodes.txt"))
    private var framesPerSecond: Int by mutableIntStateOf(60)
    private var outputDuration: Duration by mutableStateOf(Duration.ZERO)
    private var width: Int by mutableIntStateOf(0)
    private var height: Int by mutableIntStateOf(0)

    override suspend fun awaitGenerationRequest(): GenerationRequest {
        val output: File
        while (true) {
            startGeneratingRequest.awaitOneCall()
            output = outputDir ?: continue
            break
        }
        val fps = framesPerSecond
        return GenerationRequest(
            outputDir = output,
            outputFileNameWithoutExtension = outputFileNameWithoutExtension,
            size = IntSize(width, height),
            duration = outputDuration,
            framesPerSecond = fps,
            getContent =  {
                val timeCodes = Dispatchers.IO { readTimecodes(timeCodesSourceFile) }
                val nanosOffsets = LongArray(timeCodes.size) { timeCodes[it].toNanosOffset(fps) }
                ContentToRecord(nanosOffsets)
            }
        )
    }

    private fun ContentToRecord(sortedTriggerNanos: LongArray): @Composable () -> Unit = {
        CounterOverlay(rememberIncrementCounter(sortedTriggerNanos))
    }

    override suspend fun showGenerationProgress(
        framesGenerationProgress: FramesGenerationProgress,
        videoEncodingProgress: Flow<FfmpegProgressLine>
    ) {
        raceOf({
            awaitCancellation()
        }, {
            videoEncodingProgress.collect { progressLine ->
                TODO()
            }
        })
    }
}
