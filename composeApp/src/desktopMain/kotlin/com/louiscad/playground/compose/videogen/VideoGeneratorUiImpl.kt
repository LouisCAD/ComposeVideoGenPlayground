package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import java.net.URI
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class VideoGeneratorUiImpl : VideoGeneratorUi() {

    @Composable
    fun Content() = Box(Modifier.padding(16.dp)) {
        if (startGeneratingRequest.isAwaitingCall) GenRequestSetup()
        else GenRequestInProgress()
    }

    @Composable
    private fun GenRequestSetup() = Column(
        Modifier.onPreviewKeyEvent { keyEvent ->
            (keyEvent.key == Key.Enter).also { if (it) startGeneratingRequest.call() }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SizeLine()
        SecondsToRecordLine()
        NameWithoutExtensionField()
        FileDragNDropTarget(state = outputDirState, label = "output dir", dir = true)
        FileDragNDropTarget(state = timeCodesSourceFileState, label = "timecodes")
        Text(text = "fps: $framesPerSecond (FYI)")
        Button(
            onClick = startGeneratingRequest,
            enabled = startGeneratingRequest.isAwaitingCall
        ) { Text("Generate") }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FileDragNDropTarget(
        state: MutableState<File?>,
        label: String,
        dir: Boolean = false
    ) = Row {
        val file by state
        Column {
            Text(label)
            Text(text = file?.name ?: "?")
        }
        Spacer(Modifier.weight(1f))
        val dragAndDropTarget = remember {
            DragNDropTarget { dropEvent ->
                when (val data = dropEvent.dragData()) {
                    is DragData.FilesList -> {
                        val fileUri = data.readFiles().singleOrNull() ?: return@DragNDropTarget false
                        val targetFile = Paths.get(URI.create(fileUri)).toFile()
                        if (dir != targetFile.isDirectory) return@DragNDropTarget false
                        state.value = targetFile
                        true
                    }
                    else -> false
                }
            }
        }
        Box(
            Modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            ).size(48.dp).background(
                color = when {
                    dragAndDropTarget.canDrop -> Color.Green
                    dragAndDropTarget.isDragging -> Color.Cyan
                    else -> Color.LightGray
                }
            )
        )
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

    private var outputDirState = mutableStateOf<File?>(null)
    private var outputNameWithoutExtensionFieldState = TextFieldState(initialText = "generated-video")
    private var timeCodesSourceFileState = mutableStateOf<File?>(null)
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
            val timeCodesSourceFile = timeCodesSourceFileState.value ?: continue
            val timeCodes = Dispatchers.IO {
                readTimecodes(timeCodesSourceFile)
            }.ifEmpty { null } ?: continue

            output = outputDirState.value ?: continue
            nanosOffsets = LongArray(timeCodes.size) { timeCodes[it].toNanosOffset(fps) }
            println("WE HAVE ${nanosOffsets.size} timecodes")
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
        ProvideTextStyle(MaterialTheme.typography.h2.copy(color = Color.White)) {
            CounterOverlay(rememberIncrementCounter(sortedTriggerNanos))
        }
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
