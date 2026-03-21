@file:OptIn(ExperimentalFoundationApi::class)

package com.louiscad.playground.compose.videogen.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.DragNDropTarget
import com.louiscad.playground.compose.videogen.VideoGeneratorUi
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.readTimecodes
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.core.toNanosOffset
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.ui.components.SecondsToRecordLine
import com.louiscad.playground.compose.videogen.ui.components.SizeLine
import com.louiscad.playground.compose.videogen.ui.components.VideoGenProgressCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.invoke
import splitties.coroutines.CallableState
import splitties.coroutines.call
import java.io.File
import java.net.URI
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class VideoGeneratorUiImpl : VideoGeneratorUi() {

    @Composable
    fun Content() = Box(Modifier.padding(16.dp)) {
        when {
            startGeneratingRequest.isAwaitingCall -> GenRequestSetup()
            else -> VideoGenProgressCards(
                framesGenerationProgress = framesGenProgress ?: return@Box,
                videoEncodingProgress = videoEncodingProgressFlow ?: return@Box,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }
    }

    @Composable
    private fun GenRequestSetup() = Column(
        Modifier.onPreviewKeyEvent { keyEvent ->
            (keyEvent.key == Key.Enter).also { if (it) startGeneratingRequest.call() }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SizeLine(
            widthFieldState = widthFieldState,
            heightFieldState = heightFieldState,
            densityFieldState = densityFieldState
        )
        Text("${MaterialTheme.typography.h2.fontSize}")
        SecondsToRecordLine(secondsToRecordFieldState)
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
    private fun NameWithoutExtensionField() = OutlinedTextField(
        state = outputNameWithoutExtensionFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformations.acceptableFilename,
        outputTransformation = OutputTransformations.suffix(".mov"),
        label = { Text("name") },
    )

    private val startGeneratingRequest = CallableState.Companion<Unit>()

    private var outputDirState = mutableStateOf<File?>(null)
    private var outputNameWithoutExtensionFieldState = TextFieldState(initialText = "generated-video")
    private var timeCodesSourceFileState = mutableStateOf<File?>(null)
    private var framesPerSecond: Int by mutableIntStateOf(60)
    private var secondsToRecordFieldState = TextFieldState(initialText = "")
    private val widthFieldState = TextFieldState(initialText = "1920")
    private val heightFieldState = TextFieldState(initialText = "1080")
    private val densityFieldState = TextFieldState(initialText = "1.0")
    private var framesGenProgress: FramesGenerationProgress? by mutableStateOf(null)
    private var videoEncodingProgressFlow: Flow<FfmpegProgressLine>? by mutableStateOf(null)

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
        videoEncodingProgressFlow = videoEncodingProgress
        framesGenProgress = framesGenerationProgress
        try {
            awaitCancellation()
        } finally {
            videoEncodingProgressFlow = null
            framesGenProgress = null
        }
    }
}
