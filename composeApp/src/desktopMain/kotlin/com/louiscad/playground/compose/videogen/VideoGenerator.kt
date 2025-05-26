@file:OptIn(ExperimentalComposeUiApi::class)

package com.louiscad.playground.compose.videogen

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import com.louiscad.playground.compose.videogen.core.extensions.compose.onEachFrame
import com.louiscad.playground.compose.videogen.core.recordComposableAsVideo
import splitties.coroutines.rememberCallableState
import splitties.coroutines.repeatWhileActive
import java.net.URI
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoGenerator(
    defaultSize: IntSize = IntSize(1920, 1080),
    contentToRecord: @Composable () -> Unit
) {
    var writtenFrames by remember { mutableIntStateOf(0) }
    var totalFrames by remember { mutableIntStateOf(0) }
    var outputName by remember { mutableStateOf("compose_video") }
    val outputPathPicking = rememberCallableState<String>()
    val isRecording by produceState(initialValue = false) {
        repeatWhileActive {
            try {
                val outputPathFileUri = outputPathPicking.awaitOneCall()
                value = true
                val outputDir = Paths.get(URI.create(outputPathFileUri)).toFile()
                check(outputDir.isDirectory) { "The path must be a directory." }
                check(outputDir.canWrite()) { "The directory must be writable." }
                recordComposableAsVideo(
                    size = defaultSize,
                    outputDir = outputDir,
                    outputFileNameWithoutExtension = outputName,
                    duration = 10.seconds,
                    progressHandler = { total, getWrittenFrames ->
                        onEachFrame {
                            writtenFrames = getWrittenFrames()
                            totalFrames = total
                        }
                    },
                    convertingWebpsToVideo = { terminalOutput ->
                        terminalOutput.collect { progressLine ->
                            println(progressLine)
                            //TODO: Surface the progress and errors in the UI.
                        }
                    }
                ) {
                    contentToRecord()
                }
            } catch (e: Exception) {
                println(e.message ?: e.toString())
                e.printStackTrace()
            }
            value = false
        }
    }
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            var isDragging by mutableStateOf(false)
            override fun onDrop(event: DragAndDropEvent): Boolean = when (val data = event.dragData()) {
                is DragData.FilesList -> {
                    val file = data.readFiles().singleOrNull()
                    println("File: $file")
                    if (file != null) outputPathPicking(file)
                    file != null
                }
                else -> println("Unsupported drag data: $data").let { false }
            }

            override fun onEntered(event: DragAndDropEvent) {
                super.onEntered(event)
                isDragging = true
            }

            override fun onExited(event: DragAndDropEvent) {
                super.onExited(event)
                isDragging = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isDragging = false
            }
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Box(
                Modifier.dragAndDropTarget(
                    shouldStartDragAndDrop = { isRecording.not() },
                    target = dragAndDropTarget
                ).size(48.dp).background(if (dragAndDropTarget.isDragging) Color.Green else Color.Gray)
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRecording) {
                Text("Recording...")
                if (writtenFrames < totalFrames) {
                    Text("Writing frame $writtenFrames/$totalFrames...")
                }
            }
        }
    }
}
