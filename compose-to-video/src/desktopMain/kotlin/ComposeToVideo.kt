@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.core.extensions.compose.onEachFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.invoke
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
    outputDir: File,
    duration: Duration,
    onFrameWritten: (writtenFrames: Int, totalFrames: Int) -> Unit,
    convertingWebpsToVideo: suspend (terminalOutput: Flow<String>) -> Unit,
    content: @Composable () -> Unit
) {
    outputDir.mkdirs()
    val outputFileRelativePath = "000_output.mov"
    Dispatchers.IO {
        outputDir.list()!!.forEach {
            if (it.endsWith(".webp")) outputDir.resolve(it).delete()
        }
        outputDir.resolve(outputFileRelativePath).delete()
    }
    val generationDuration: Duration = measureTime {
        recordComposableAsImages(
            size = size,
            outputDir = outputDir,
            duration = duration,
            progressListener = { totalFrames, getWrittenFrames ->
                onEachFrame { onFrameWritten(getWrittenFrames(), totalFrames) }
            },
            content = content
        )
    }
    println("Took $generationDuration to generate and write WEBPs")
    // This helped: https://ottverse.com/ffmpeg-convert-to-apple-prores-422-4444-hq/
    val ffmpeg = try {
        val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        runInterruptible(Dispatchers.IO) {
            val checkCommand = if (isWindows) "WHERE ffmpeg" else "which ffmpeg"
            checkCommand.executeBlocking()
        }
        "ffmpeg"
    } catch (_: NonZeroExitCodeException) {
        Loader.load(ffmpeg::class.java)
    }
    val conversionCommand = "$ffmpeg -framerate 60 -i %d.webp -c:v prores_ks -profile:v 4 " +
            "-pix_fmt yuva444p10le -alpha_bits 16 -r 60 -movflags +faststart $outputFileRelativePath"
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

