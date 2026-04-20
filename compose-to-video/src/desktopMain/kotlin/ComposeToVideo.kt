@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
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
    density: Density = Density(1f),
    framesPerSecond: Int = 60,
    outputDir: File,
    outputFileNameWithoutExtension: String,
    duration: Duration,
    progressHandler: FramesWritingProgressHandler = FramesWritingProgressHandler { _, _ -> awaitCancellation() },
    convertingWebpsToVideo: suspend (terminalOutput: Flow<FfmpegProgressLine>) -> Unit,
    content: @Composable () -> Unit
) {
    val outputFileRelativePath = "$outputFileNameWithoutExtension.mov"
    val freshDir: Boolean
    val tmpDirForWebps: File
    withContext(Dispatchers.IO) {
        freshDir = outputDir.mkdirs()
        tmpDirForWebps = outputDir.resolve("tmp_webps").also {
            val wasCreated = it.mkdirs()
            if (wasCreated.not()) {
                it.deleteRecursively()
                it.mkdirs()
            }
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
            density = density,
            framesPerSecond = framesPerSecond,
            outputDir = tmpDirForWebps,
            duration = duration,
            progressHandler = progressHandler,
            content = content
        )
    }
    run {
        println("Took $generationDuration to generate and write WEBPs")
        val difference = generationDuration - recordingDurationSummary.let {
            it.framesGeneration + it.encodingAndWritingGeneration
        }
        println("Time calculation difference: $difference")
    }
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
        add("\"$outputFileRelativePath\"")
    }.joinToString(separator = " ")
    measureTime {
        val progressLines = conversionCommand.commandExecutionLines(
            workingDir = outputDir
        ).mapNotNull { rawLine ->
            runCatching {
                FfmpegProgressLine.parseOrNull(rawLine)
            }.onFailure {
                println("Failed to parse line: $rawLine")
                it.printStackTrace()
            }.getOrNull()
        }
        convertingWebpsToVideo(progressLines)
    }.also { println("Took $it to build video from WEBPs") }

}
