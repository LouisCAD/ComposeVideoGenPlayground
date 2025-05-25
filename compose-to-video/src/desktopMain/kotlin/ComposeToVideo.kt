@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.core.extensions.compose.onEachFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import java.io.File
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Example usage:
 * ```kotlin
 * recordComposableAsVideo(
 *     size = IntSize(width = 1920, height = 1080),
 *     outputDir = File("videos/something").also {
 *         it.parentFile.mkdirs()
 *     },
 *     duration = 10.seconds
 * ) {
 *     DemoComposable()
 * }
 * ```
 */
suspend fun recordComposableAsVideo(
    size: IntSize,
    framesPerSecond: Int = 60,
    outputDir: File,
    outputFileNameWithoutExtension: String,
    duration: Duration,
    onFrameWritten: (writtenFrames: Int, totalFrames: Int) -> Unit,
    convertingWebpsToVideo: suspend (terminalOutput: Flow<String>) -> Unit,
    content: @Composable () -> Unit
) {
    val outputFileRelativePath = "$outputFileNameWithoutExtension.mov"
    val freshDir: Boolean
    val tmpDirForWebps: File
    withContext(Dispatchers.IO) {
        freshDir = outputDir.mkdirs()
        tmpDirForWebps = outputDir.resolve("tmp_webps").also {
            val wasCreated = it.mkdirs()
            if (wasCreated.not()) it.deleteRecursively()
            it.deleteOnExit()
        }
        if (freshDir.not()) {
            val outputFile = outputDir.resolve(outputFileRelativePath)
            if (outputFile.exists()) outputFile.delete()
        }
    }
    val recordingDurationSummary: FramesRecordingDurationSummary
    val generationDuration: Duration = measureTime {
        recordingDurationSummary = recordComposableAsImages(
            size = size,
            framesPerSecond = framesPerSecond,
            outputDir = tmpDirForWebps,
            duration = duration,
            progressHandler = { totalFrames, getWrittenFrames ->
                onEachFrame { onFrameWritten(getWrittenFrames(), totalFrames) }
            },
            content = content
        )
    }
    println("Took $generationDuration to generate and write WEBPs")
    // This helped: https://ottverse.com/ffmpeg-convert-to-apple-prores-422-4444-hq/
    val ffmpeg: String = try {
        val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        runInterruptible(Dispatchers.IO) {
            val checkCommand = if (isWindows) "WHERE ffmpeg" else "which ffmpeg"
            checkCommand.executeBlocking()
        }
        "ffmpeg"
    } catch (_: NonZeroExitCodeException) {
        Loader.load(ffmpeg::class.java)
    }
    val fileSeparator = File.separatorChar
    val conversionCommand: String = buildList {
        add(ffmpeg)
        add("-framerate $framesPerSecond")
        add("-i ${tmpDirForWebps.name}$fileSeparator%d.webp")
        add("-c:v prores_ks")
        add("-profile:v 4")
        add("-pix_fmt yuva444p10le")
        add("-alpha_bits 16")
        add("-r $framesPerSecond")
        add("-movflags +faststart")
        add(outputFileRelativePath)
    }.joinToString(separator = " ")
    measureTime {
        convertingWebpsToVideo(conversionCommand.commandExecutionLines(workingDir = outputDir))
    }.also { println("Took $it to build video from WEBPs") }
    if (false) outputDir.list()!!.forEach {
        if (it.endsWith(".webp")) outputDir.resolve(it).delete()
    }
}

suspend fun <T> Flow<T>.collectParallel(
    maxParallelism: Int,
    bufferCapacity: Int = maxParallelism,
    transform: suspend (T) -> Unit
) {
    coroutineScope {
        val inputChannel = Channel<T>(capacity = bufferCapacity)
        launch {
            collect { inputChannel.send(it) }
            inputChannel.close()
        }
        for (i in 0..<maxParallelism) launch {
            for (element in inputChannel) transform(element)
        }
    }
}

