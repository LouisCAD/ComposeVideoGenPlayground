@file:OptIn(ExperimentalFoundationApi::class)

package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.readTimecodes
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.core.toNanosOffset
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import composevideogenplayground.composeapp.generated.resources.Res
import composevideogenplayground.composeapp.generated.resources.info_24dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.invoke
import org.jetbrains.compose.resources.painterResource
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
        Text("${MaterialTheme.typography.h2.fontSize}")
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
    ) {
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
        Row(
            modifier = Modifier.padding(8.dp).dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            ).let {
                when {
                    dragAndDropTarget.canDrop -> it.border(width = 2.dp, color = Color.Green)
                    dragAndDropTarget.isDragging -> it.border(width = 2.dp, color = Color.LightGray)
                    else -> it
                }
            }
        ) {
            val file by state
            Column {
                Text(label)
                Text(text = file?.name ?: "?")
            }
            Spacer(Modifier.weight(1f))
//            Box(
//                Modifier.size(48.dp).background(
//                    color = when {
//                        dragAndDropTarget.canDrop -> Color.Green
//                        dragAndDropTarget.isDragging -> Color.Cyan
//                        else -> Color.LightGray
//                    }
//                )
//            )
        }
    }

    @Composable
    private fun SizeLine() = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        OutlinedTextField(
            state = densityFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = decimalOnlyInputTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("density") }
        )
        //TODO: Allow changing fps
    }

    @Composable
    private fun NameWithoutExtensionField() = OutlinedTextField(
        state = outputNameWithoutExtensionFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = acceptableFilenameTransformation,
        label = { Text("name (without extension)") },
    )

    @Composable
    private fun SecondsToRecordLine() = Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            state = secondsToRecordFieldState,
            modifier = Modifier.width(150.dp),
            lineLimits = TextFieldLineLimits.SingleLine,
            trailingIcon = {
                TooltipArea(
                    tooltip = { Surface(shape = RoundedCornerShape(8.dp)) { Text("Keep blank to use last timecode", Modifier.padding(4.dp)) } },
                    delayMillis = 0,
                    tooltipPlacement = TooltipPlacement.ComponentRect(anchor = Alignment.CenterEnd, alignment = Alignment.CenterEnd)
                ) {
                    Icon(painterResource(Res.drawable.info_24dp), contentDescription = "Info")
                }
            },
            inputTransformation = decimalOnlyInputTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("seconds") },
        )
    }

    @Composable
    private fun GenRequestInProgress() = Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val framesGenerationProgress = framesGenProgress ?: return
        Text("Progress", style = MaterialTheme.typography.h2)
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Frames generation", style = MaterialTheme.typography.h2)
                LinearProgressIndicator(
                    progress = framesGenerationProgress.let {
                        it.writtenFrames.toFloat() / it.totalFrames
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    Text("Frames:", style = MaterialTheme.typography.h6)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = with(framesGenerationProgress) { "$writtenFrames/$totalFrames" },
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Encoding", style = MaterialTheme.typography.h2)
                val ratio = encodingProgressLine?.let {
                    it.frameNumber.toFloat() / framesGenerationProgress.totalFrames
                } ?: 0f
                LinearProgressIndicator(
                    progress = ratio,
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    Text("Speed:", style = MaterialTheme.typography.h6)
                    Text(text = encodingProgressLine?.let { "${it.speedFactor}x" } ?: "-")
                }
                Row {
                    Text("Frames throughput:", style = MaterialTheme.typography.h6)
                    Spacer(Modifier.weight(1f))
                    Text(text = encodingProgressLine?.let { "${it.fps}fps" } ?: "-", textAlign = TextAlign.End)
                }
            }
        }
    }

    private val startGeneratingRequest = CallableState<Unit>()

    private var outputDirState = mutableStateOf<File?>(null)
    private var outputNameWithoutExtensionFieldState = TextFieldState(initialText = "generated-video")
    private var timeCodesSourceFileState = mutableStateOf<File?>(null)
    private var framesPerSecond: Int by mutableIntStateOf(60)
    private var secondsToRecordFieldState = TextFieldState(initialText = "")
    private val widthFieldState = TextFieldState(initialText = "1920")
    private val heightFieldState = TextFieldState(initialText = "1080")
    private val densityFieldState = TextFieldState(initialText = "1.0")
    private var framesGenProgress: FramesGenerationProgress? by mutableStateOf(null)
    private var encodingProgressLine: FfmpegProgressLine? by mutableStateOf(null)

    override suspend fun awaitGenerationRequest(): GenerationRequest {
        val fps = framesPerSecond
        val output: File
        val nanosOffsets: LongArray
        val outputDuration: Duration
        val width: Int
        val height: Int
        val density: Density
        while (true) {
            startGeneratingRequest.awaitOneCall()
            val _output = outputDirState.value ?: continue
            val _density = densityFieldState.text.toString().toFloatOrNull() ?: continue
            val _width = widthFieldState.text.toString().toInt()
            val _height = heightFieldState.text.toString().toInt()
            val secondsToRecord = secondsToRecordFieldState.text.takeUnless { it.isEmpty() }?.toString()?.toDouble()
            val timeCodesSourceFile = timeCodesSourceFileState.value ?: continue
            val timeCodes = Dispatchers.IO {
                readTimecodes(timeCodesSourceFile)
            }.ifEmpty { null } ?: continue

            nanosOffsets = LongArray(timeCodes.size) { timeCodes[it].toNanosOffset(fps) }
            println("WE HAVE ${nanosOffsets.size} timecodes")
            outputDuration = secondsToRecord?.seconds ?: nanosOffsets.last().nanoseconds
            output = _output
            width = _width
            height = _height
            density = Density(_density)
            break
        }
        val outputFileNameWithoutExtension = outputNameWithoutExtensionFieldState.text.toString()
        return GenerationRequest(
            outputDir = output,
            outputFileNameWithoutExtension = outputFileNameWithoutExtension,
            size = IntSize(width, height),
            density = density,
            duration = outputDuration,
            framesPerSecond = fps,
            getContent = {
                ContentToRecord(nanosOffsets)
            }
        )
    }

    private fun ContentToRecord(sortedTriggerNanos: LongArray): @Composable () -> Unit = {
        ProvideTextStyle(MaterialTheme.typography.h2.copy(color = Color.White, fontWeight = FontWeight.Medium)) {
            CounterOverlay(rememberIncrementCounter(sortedTriggerNanos))
        }
    }

    override suspend fun showGenerationProgress(
        framesGenerationProgress: FramesGenerationProgress,
        videoEncodingProgress: Flow<FfmpegProgressLine>
    ) {
        raceOf({
            framesGenProgress = framesGenerationProgress
            try {
                awaitCancellation()
            } finally {
                framesGenProgress = null
            }
        }, {
            videoEncodingProgress.onEach { progressLine ->
                encodingProgressLine = progressLine
                println(progressLine)
            }.onCompletion {
                encodingProgressLine = null
            }.collect()
        })
    }

    private val digitOnlyInputTransformation = InputTransformation {
        if (asCharSequence().any { it.isDigit().not() }) revertAllChanges()
    }

    private val decimalOnlyInputTransformation = InputTransformation {
        if (asCharSequence().any { it.isDigit().not() && it != '.' }) revertAllChanges()
    }

    private val acceptableFilenameTransformation = InputTransformation {
        val text = asCharSequence()
        if ('/' in text) revertAllChanges()
        if ('\\' in text) revertAllChanges()
        //TODO: Filter filesystem all/most filesystem problematic characters.
    }
}
