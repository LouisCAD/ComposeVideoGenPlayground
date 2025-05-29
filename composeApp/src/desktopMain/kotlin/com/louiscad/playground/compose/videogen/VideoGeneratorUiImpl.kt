package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.KeyboardType
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
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

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
        SizeLine()
        SecondsToRecordLine()
        NameWithoutExtensionField()
        TODO("Add drag'N'drop for timecodes file and for output directory")
    }

    @Composable
    private fun SizeLine() = Row {
        OutlinedTextField(
            state = widthFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = digitOnlyInputTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("width") }
        )
        OutlinedTextField(
            state = heightFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = digitOnlyInputTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("height") }
        )
        //TODO: Allow changing fps
    }

    @Composable
    private fun NameWithoutExtensionField() = OutlinedTextField(
        state = outputNameWithoutExtensionFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = digitOnlyInputTransformation,
        label = { Text("seconds (keep blank to use last timecode)") },
    )

    @Composable
    private fun SecondsToRecordLine() = OutlinedTextField(
        state = secondsToRecordFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = digitOnlyInputTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text("seconds (keep blank to use last timecode)") },
    )

    @Composable
    private fun GenRequestInProgress() = Column {
        //TODO()
    }

    private val startGeneratingRequest = CallableState<Unit>()

    private var outputDir: File? by mutableStateOf(null)
    private var outputNameWithoutExtensionFieldState = TextFieldState(initialText = "generated-video")
    private var timeCodesSourceFile: File? by mutableStateOf(null)
    private var framesPerSecond: Int by mutableIntStateOf(60)
    private var secondsToRecordFieldState = TextFieldState(initialText = "10")
    private val widthFieldState = TextFieldState(initialText = "1920")
    private val heightFieldState = TextFieldState(initialText = "1080")

    override suspend fun awaitGenerationRequest(): GenerationRequest {
        val fps = framesPerSecond
        val output: File
        val nanosOffsets: LongArray
        val outputDuration: Duration
        while (true) {
            startGeneratingRequest.awaitOneCall()
            val timeCodesFile = timeCodesSourceFile ?: continue
            val timeCodes = Dispatchers.IO { readTimecodes(timeCodesFile) }.ifEmpty { null } ?: continue
            output = outputDir ?: continue
            nanosOffsets = LongArray(timeCodes.size) { timeCodes[it].toNanosOffset(fps) }
            outputDuration = secondsToRecordFieldState.text.let {
                if (it.isEmpty()) nanosOffsets.last().nanoseconds else it.toString().toInt().seconds
            }
            break
        }
        val width = widthFieldState.text.toString().toInt()
        val height = heightFieldState.text.toString().toInt()
        val outputFileNameWithoutExtension = outputNameWithoutExtensionFieldState.text.toString()
        return GenerationRequest(
            outputDir = output,
            outputFileNameWithoutExtension = outputFileNameWithoutExtension,
            size = IntSize(width, height),
            duration = outputDuration,
            framesPerSecond = fps,
            getContent = {
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
                println(progressLine)
                //TODO()
            }
        })
    }

    private val digitOnlyInputTransformation = InputTransformation {
        if (asCharSequence().any { it.isDigit().not() }) revertAllChanges()
    }

    private val acceptableFilenameTransformation = InputTransformation {
        val text = asCharSequence()
        if ('/' in text) revertAllChanges()
        if ('\\' in text) revertAllChanges()
        //TODO: Filter filesystem all/most filesystem problematic characters.
    }
}
