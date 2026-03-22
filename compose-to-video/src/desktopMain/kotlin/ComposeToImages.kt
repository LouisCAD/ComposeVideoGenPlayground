@file:OptIn(ExperimentalAtomicApi::class)

package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.use
import com.louiscad.playground.compose.videogen.core.extensions.coroutines.collectParallel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
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
import kotlinx.coroutines.CompletableDeferred

suspend fun recordComposableAsImages(
    size: IntSize,
    density: Density,
    framesPerSecond: Int = 60,
    outputDir: File,
    duration: Duration,
    progressHandler: FramesWritingProgressHandler = FramesWritingProgressHandler { _, _ -> awaitCancellation() },
    content: @Composable () -> Unit
): FramesRecordingDurationSummary {

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
    return MainUIDispatcher {
        ImageComposeScene(
            width = size.width,
            height = size.height,
            density = density,
            coroutineContext = coroutineContext[ContinuationInterceptor]!!,
            content = content
        ).use { scene ->
            val images = scene.images(cutAt = duration, framesPerSecond = framesPerSecond)
            val imagesWrittenCount = AtomicInt(0)
            Dispatchers.Default {
                raceOf({
                    images.recordAllParallelizedInto(
                        outputDir = outputDir,
                        progressListener = { _, _ -> imagesWrittenCount.incrementAndFetch() },
                    )
                }, {
                    progressHandler.handleProgress(
                        totalFrames = expectedImagesCount,
                        getCurrentWrittenFrames = { imagesWrittenCount.load() }
                    )
                })
            }
        }
    }
}

private fun ImageComposeScene.images(
    cutAt: Duration,
    framesPerSecond: Int
): Flow<Image> = flow {
    val limitNanos = cutAt.inWholeNanoseconds
    var frameIndex = 0
    val oneSecondNanos = 1.seconds.inWholeNanoseconds
    var currentSecond = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        val nanosForFrameIndex: Long = oneSecondNanos * frameIndex / framesPerSecond
        val currentNanos = currentSecond.seconds.inWholeNanoseconds + nanosForFrameIndex
        if (currentNanos >= limitNanos) return@flow
        // We render twice because the first render is sometimes not settled as it should.
        // See this: https://kotlinlang.slack.com/archives/C01D6HTPATV/p1746482154335809
        render(currentNanos)
        yield()
        emit(render(currentNanos))
        frameIndex++
        if (frameIndex == framesPerSecond) {
            frameIndex = 0
            currentSecond++
        }
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


private suspend fun Flow<Image>.recordAllParallelizedInto(
    outputDir: File,
    progressListener: FramesRecordingProgressListener = FramesRecordingProgressListener { _, _ -> }
): FramesRecordingDurationSummary = Dispatchers.Default {

    val durationSummaryCompletable = CompletableDeferred<FramesRecordingDurationSummary>()

    val generatedImages = withIndex().measure { upstreamDuration, downstreamDuration ->
        val summary = FramesRecordingDurationSummary(
            framesGeneration = upstreamDuration,
            encodingAndWritingGeneration = downstreamDuration
        )
        durationSummaryCompletable.complete(summary)
    }

    generatedImages.collectParallel(maxParallelism = 64) { (index, skiaImage) ->
        skiaImage.use { image ->
            val webpData: Data
            val encodingDuration = measureTime { webpData = image.encodeToData(EncodedImageFormat.WEBP)!! }
            progressListener.onFrameEncoded(index, encodingDuration)
            webpData.use { webpData ->
                val bytes: ByteArray = webpData.bytes
                Dispatchers.IO {
                    val writeDuration = measureTime { outputDir.resolve("$index.webp").writeBytes(bytes) }
                    progressListener.onFrameWritten(index, writeDuration)
                }
            }
        }
    }
    durationSummaryCompletable.await()
}
