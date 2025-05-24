@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.invoke
import kotlinx.coroutines.yield
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skiko.MainUIDispatcher
import splitties.coroutines.raceOf
import java.io.File
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlin.use
import androidx.compose.runtime.withFrameNanos

fun interface FramesWritingProgressListener {

    /**
     * This is deliberately not an observable type, because updates are expected to arrive faster
     * than a human could perceive, and faster than the screen refresh-rate.
     *
     * The right way is therefore to poll [getCurrentWrittenFrames] on each frame,
     * with a loop and [withFrameNanos], for a UI made with Compose, that is.
     */
    suspend fun handleProgress(
        totalFrames: Int,
        getCurrentWrittenFrames: () -> Int
    ): Nothing
}

/**
 * Note that [frameWrittenCountUpdate] is deliberately not named `onFrameWritten` because
 * it is not called on each written frame, since it could be higher than the screen
 * refresh-rate. It is instead called on each frame (see [androidx.compose.runtime.withFrameNanos]),
 * for display in UI.
 */
suspend fun recordComposableAsImages(
    size: IntSize,
    outputDir: File,
    framesPerSecond: Int = 60,
    duration: Duration,
    progressListener: FramesWritingProgressListener = FramesWritingProgressListener { _, _ -> awaitCancellation() },
    content: @Composable () -> Unit
) {
    val expectedImagesCount = framesCountFor(duration, framesPerSecond).also {
        require(it <= Int.MAX_VALUE) {
            "Insane! You probably don't want to actually record for this long ($duration) at ${framesPerSecond}fps"
        }
    }.toInt()

    // Using `MainUiDispatcher` is needed to work around threading issues in `ImageComposeScene`.
    // See these issues:
    // - https://issuetracker.google.com/issues/283162626
    // - https://issuetracker.google.com/issues/283216580
    // - https://github.com/JetBrains/compose-multiplatform/issues/1396
    //
    // They were mentioned in this thread on Kotlin's Slack:
    //   https://kotlinlang.slack.com/archives/C01D6HTPATV/p1747657487445149?thread_ts=1746482154.335809&cid=C01D6HTPATV
    MainUIDispatcher {
        ImageComposeScene(
            width = size.width,
            height = size.height,
            coroutineContext = coroutineContext[ContinuationInterceptor]!!
        ).use { scene ->
            scene.setContent(content)
            val interval = 1.seconds / framesPerSecond
            val images = scene.images(cutAt = duration, interval = interval)
            val imagesWrittenCount = AtomicInt(0)
            raceOf({
                images.recordAllParallelizedInto(
                    outputDir = outputDir,
                    onImageWritten = { imagesWrittenCount.incrementAndFetch() }
                )
            }, {
                progressListener.handleProgress(
                    totalFrames = expectedImagesCount,
                    getCurrentWrittenFrames = { imagesWrittenCount.load() }
                )
            })
        }
    }
}

fun framesCountFor(
    duration: Duration,
    framesPerSecond: Int
): Long {
    // We separate whole seconds to avoid cumulating approximations
    // that could lead to an inaccurate result.
    val wholeSeconds = duration.inWholeSeconds
    return wholeSeconds * framesPerSecond + run {
        val wholeSecondsPart = wholeSeconds.seconds
        if (duration == wholeSecondsPart) return@run 0L
        val remainder = duration - wholeSecondsPart
        (remainder * framesPerSecond / 1.seconds).toLong()
    }
}

fun framesCountFor(
    duration: Duration,
    framesPerSecond: Double
): Long {
    return (duration * framesPerSecond / 1.seconds).toLong()
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
}.flowOn(MainUIDispatcher) // Wrong frames can be shown otherwise, see the comments below:
// Using `MainUiDispatcher` is needed to work around threading issues in `ImageComposeScene`.
// See these issues:
// - https://issuetracker.google.com/issues/283162626
// - https://issuetracker.google.com/issues/283216580
// - https://github.com/JetBrains/compose-multiplatform/issues/1396
//
// They were mentioned in this thread on Kotlin's Slack:
//   https://kotlinlang.slack.com/archives/C01D6HTPATV/p1747657487445149?thread_ts=1746482154.335809&cid=C01D6HTPATV


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
                image.use { img ->
                    val webpData: Data
                    measureTime {
                        webpData = img.encodeToData(EncodedImageFormat.WEBP)!!
                    }.also { encodeTimes.send(it) }
                    webpData.use {
                        val bytes: ByteArray = it.bytes
                        Dispatchers.IO {
                            measureTime {
                                val file = outputDir.resolve("$index.webp")
                                file.writeBytes(bytes)
                                onImageWritten()
                            }.also { writeTimes.send(it) }
                        }
                    }
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
//        println("avg $what duration is $avg")
    }
}
