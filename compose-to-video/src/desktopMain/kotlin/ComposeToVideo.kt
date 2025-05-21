@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.use
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.withIndex
import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skiko.MainUIDispatcher
import splitties.coroutines.raceOf
import splitties.coroutines.repeatWhileActive
import java.io.File
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
): Unit = withContext(MainUIDispatcher) {
    outputDir.mkdirs()
    val outputFileRelativePath = "000_output.mov"
    Dispatchers.IO {
        outputDir.list()!!.forEach {
            if (it.endsWith(".webp")) outputDir.resolve(it).delete()
        }
        outputDir.resolve(outputFileRelativePath).delete()
    }
    ImageComposeScene(
        width = size.width,
        height = size.height,
        coroutineContext = coroutineContext[ContinuationInterceptor]!!
    ).use { scene ->
        scene.setContent(content)
        val interval = 1.seconds / 60
        val expectedImagesCount = expectedImagesCount(duration, interval)
        val images = scene.images(cutAt = duration, interval = interval)
        val imagesWrittenCount = AtomicInt(0)
        val time = measureTime {
            raceOf({
                images.recordAllParallelizedInto(outputDir, onImageWritten = {
                    imagesWrittenCount.incrementAndFetch()
                })
            }, {
                repeatWhileActive {
                    withFrameNanos {
                        onFrameWritten(imagesWrittenCount.load(), expectedImagesCount)
                    }
                }
            })
        }
        println("Took $time to generate and write WEBPs")
        // This helped: https://ottverse.com/ffmpeg-convert-to-apple-prores-422-4444-hq/
        val ffmpeg = try {
            //TODO: Support Windows properly
            runInterruptible(Dispatchers.IO) { "which ffmpeg".executeBlocking() }
            "ffmpeg"
        } catch (e: NonZeroExitCodeException) {
            Loader.load(ffmpeg::class.java)
        }
        val outputFileRelativePath = "output.mov"
        val conversionCommand = "$ffmpeg -framerate 60 -i %d.webp -c:v prores_ks -profile:v 4 " +
                                "-pix_fmt yuva444p10le -alpha_bits 16 -r 60 -movflags +faststart $outputFileRelativePath"
        measureTime {
            convertingWebpsToVideo(conversionCommand.commandExecutionLines(workingDir = outputDir))
        }.also { println("Took $it to build video from WEBPs") }
        outputDir.list()!!.forEach {
            if (it.endsWith(".webp")) outputDir.resolve(it).delete()
        }
    }
}

private fun expectedImagesCount(
    duration: Duration,
    interval: Duration
): Int {
    val intervalNanos = interval.inWholeNanoseconds
    val limitNanos = duration.inWholeNanoseconds
    return (limitNanos / intervalNanos).toInt()
}

private fun ImageComposeScene.images(
    cutAt: Duration,
    interval: Duration
): Flow<Image> = flow {
    val intervalNanos = interval.inWholeNanoseconds
    val limitNanos = cutAt.inWholeNanoseconds
    var lastTimeNanos = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        if (lastTimeNanos > limitNanos) return@flow
        // We render twice because the first render is sometimes not settled as it should.
        // See this: https://kotlinlang.slack.com/archives/C01D6HTPATV/p1746482154335809
        render(lastTimeNanos)
        yield()
        emit(render(lastTimeNanos))
        lastTimeNanos += intervalNanos
    }
}.flowOn(MainUIDispatcher) // Wrong frames can be shown otherwise…
// See https://kotlinlang.slack.com/archives/C01D6HTPATV/p1747657487445149?thread_ts=1746482154.335809&cid=C01D6HTPATV

private suspend fun Flow<Image>.recordAllInto(outputDir: File) {
    var imgNumber = 0
    measure { upstreamDuration, downstreamDuration ->
        println("UP :$upstreamDuration")
        println("DOWN :$downstreamDuration")
    }.collect { skiaImage ->
        skiaImage.use {
            outputDir.resolve("$imgNumber.png").writeBytes(it.encodeToData()!!.bytes)
            imgNumber++
        }
    }
}

private suspend fun Flow<Image>.recordAllParallelizedInto(
    outputDir: File,
    onImageWritten: () -> Unit
) {
    val generatedImages = withIndex().measure { upstreamDuration, downstreamDuration ->
        println("UP  :$upstreamDuration")
        println("DOWN:$downstreamDuration")
    }

    val encodeTimes = Channel<Duration>()
    val writeTimes = Channel<Duration>()

    Dispatchers.Default {
        raceOf({
            printAvgDurationUpdates(encodeTimes, "encode")
        }, {
            printAvgDurationUpdates(writeTimes, "write")
        }, {
            generatedImages.collectParallel(maxParallelism = 64) { (index, image) ->
                val bytes: ByteArray
                measureTime {
                    bytes = image.use { it.encodeToData(EncodedImageFormat.WEBP)!!.bytes }
                }.also { encodeTimes.send(it) }
                val file = outputDir.resolve("$index.webp")
                Dispatchers.IO {
                    measureTime {
                        file.writeBytes(bytes)
                        onImageWritten()
                    }.also { writeTimes.send(it) }
                }
            }
        })
    }
}

private suspend fun printAvgDurationUpdates(durations: ReceiveChannel<Duration>, what: String) {
    var n = 0
    var totalDuration = Duration.ZERO
    for (duration in durations) {
        n++
        totalDuration += duration
        val avg = totalDuration / n
        println("avg $what duration is $avg")
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

