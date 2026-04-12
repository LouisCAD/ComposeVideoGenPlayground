package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.DragNDropTarget
import com.louiscad.playground.compose.videogen.VideoGenerationRequest
import com.louiscad.playground.compose.videogen.core.readTimecodes
import com.louiscad.playground.compose.videogen.core.toNanosOffset
import com.louiscad.playground.compose.videogen.ui.InputTransformations
import com.louiscad.playground.compose.videogen.ui.OutputTransformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.isActive
import splitties.coroutines.call
import splitties.coroutines.rememberCallableState
import java.io.File
import java.net.URI
import java.nio.file.Paths
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoGenSetup(
    name: String = "generated-video",
    initialSize: IntSize = IntSize(1920, 1080),
    initialDensity: Float = 1f,
    contentToRecord: @Composable (sortedTriggerNanos: LongArray) -> Unit,
    onGenRequested: (VideoGenerationRequest) -> Unit
) {

    val startGeneratingRequest = rememberCallableState<Unit>()
    val outputDirState = remember { mutableStateOf<File?>(null) }
    val outputNameWithoutExtensionFieldState = remember(name) { TextFieldState(initialText = name) }
    val timeCodesSourceFileState = remember { mutableStateOf<File?>(null) }
    var framesPerSecond: Int by remember { mutableIntStateOf(60) }
    val secondsToRecordFieldState = remember { TextFieldState(initialText = "") }
    val widthFieldState = remember(initialSize) { TextFieldState(initialText = initialSize.width.toString()) }
    val heightFieldState = remember(initialSize) { TextFieldState(initialText = initialSize.height.toString()) }
    val densityFieldState = remember(initialDensity) { TextFieldState(initialText = initialDensity.toString()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val fps = framesPerSecond
            startGeneratingRequest.awaitOneCall()
            val output: File = outputDirState.value ?: continue
            val _density = densityFieldState.text.toString().toFloatOrNull() ?: continue
            val width = widthFieldState.text.toString().toInt()
            val height = heightFieldState.text.toString().toInt()
            val secondsToRecord = secondsToRecordFieldState.text.takeUnless { it.isEmpty() }?.toString()?.toDouble()
            val timeCodesSourceFile = timeCodesSourceFileState.value ?: continue
            val timeCodes = Dispatchers.IO {
                readTimecodes(timeCodesSourceFile)
            }.ifEmpty { null } ?: continue

            val nanosOffsets = LongArray(timeCodes.size) { timeCodes[it].toNanosOffset(fps) }
            println("WE HAVE ${nanosOffsets.size} timecodes")
            val outputDuration = secondsToRecord?.seconds ?: nanosOffsets.last().nanoseconds
            val density = Density(_density)
            val outputFileNameWithoutExtension = outputNameWithoutExtensionFieldState.text.toString()
            val request = VideoGenerationRequest(
                outputDir = output,
                outputFileNameWithoutExtension = outputFileNameWithoutExtension,
                size = IntSize(width, height),
                density = density,
                duration = outputDuration,
                framesPerSecond = fps,
                getContent = { { contentToRecord(nanosOffsets) } }
            )
            onGenRequested(request)
        }
    }

    Column(
        modifier = Modifier.padding(16.dp).onPreviewKeyEvent { keyEvent ->
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
        NameWithoutExtensionField(outputNameWithoutExtensionFieldState)
        FileDragNDropTarget(state = outputDirState, label = "output dir", dir = true)
        FileDragNDropTarget(state = timeCodesSourceFileState, label = "timecodes")
        Text(text = "fps: $framesPerSecond (FYI)")
        Button(
            onClick = startGeneratingRequest,
            enabled = startGeneratingRequest.isAwaitingCall
        ) { Text("Generate") }
    }
}

@Composable
private fun NameWithoutExtensionField(outputNameWithoutExtensionFieldState: TextFieldState) = OutlinedTextField(
    state = outputNameWithoutExtensionFieldState,
    lineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation = InputTransformations.acceptableFilename,
    outputTransformation = OutputTransformations.suffix(".mov"),
    label = { Text("name") },
)

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
