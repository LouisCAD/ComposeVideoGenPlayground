package com.louiscad.playground.compose.videogen.lib.experiments

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.use
import com.louiscad.playground.compose.videogen.lib.measure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import org.jetbrains.skia.Image
import org.jetbrains.skiko.toImage
import splitties.coroutines.repeatWhileActive
import java.awt.image.BufferedImage
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * **Experimental**
 * Using intermediary WEBP images to generate video seems to currently be as fast, if not faster.
 *
 * Example usage:
 *
 * ```kotlin
 * recordComposable(
 *     IntSize(width = 1920, height = 1080),
 *     output = File("videos/input4.webm").also {
 *         it.parentFile.mkdirs()
 *     },
 *     duration = 2.seconds
 * ) {
 *     DemoComposable()
 * }
 * ```
 */
suspend fun recordComposable(
    size: IntSize,
    output: File,
    duration: Duration,
    content: @Composable () -> Unit
) {
    ImageComposeScene(
        width = size.width,
        height = size.height
    ).use { scene ->
        scene.setContent(content)
        val bytes = scene.render().toComposeImageBitmap().toAwtImage().toImage().encodeToData()!!.bytes
        output.resolveSibling("sample_img.png").writeBytes(bytes)
//        val ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg::class.java)
//        val pb = ProcessBuilder(ffmpeg, "-allow_sw", "1", "-alpha_quality", "0.75", "-vtag", "hvc1", "-movflags", "+faststart");
//        val process = pb.inheritIO().start()
        FFmpegFrameRecorder.createDefault(output, size.width, size.height).use { frameRecorder ->
            frameRecorder.frameRate = 60.0
            //TODO: Record in ProRes 4444
//            AV_PROFILE_PRORES_4444
            frameRecorder.sampleRate
            // To auto-select pixel format, see: https://github.com/b005t3r/JavaCV-cli/blob/011f5f9c75e6fd762389af6e179447aa68806d71/src/main/java/com/lazarecki/Main.java#L107-L128
            
//            frameRecorder.pixelFormat = avutil.AV_PIX_FMT_YUVA444P10LE
//            frameRecorder.videoCodec = avcodec.AV_CODEC_ID_PRORES
//            frameRecorder.pixelFormat = avutil.AV_PIX_FMT_BGRA
//            frameRecorder.videoCodec = avcodec.AV_CODEC_ID_VP9
            frameRecorder.also {
                it.format = "webm"
                it.videoCodec = avcodec.AV_CODEC_ID_VP9
                it.pixelFormat = avutil.AV_PIX_FMT_YUVA420P
//                it.videoQuality = 0.0
            }
            if (false) frameRecorder.also {
                it.format = "mov"
                it.videoCodec = avcodec.AV_CODEC_ID_HEVC
                it.pixelFormat = avutil.AV_PIX_FMT_BGRA
                it.videoBitrate = size.width * size.height * 4 * 8 * frameRecorder.frameRate.toInt()
            }
            frameRecorder.start()
            val images = scene.images(
                cutAt = duration,
                interval = (1f / frameRecorder.frameRate).seconds
            )
            val time = measureTime {
                frameRecorder.recordAll(images)
            }
            println("Took $time")
            frameRecorder.stop()
        }
//        process.waitFor()
    }
}

private fun ImageComposeScene.images(
    cutAt: Duration,
    interval: Duration
): Flow<Image> = flow {
    val intervalNanos = interval.inWholeNanoseconds
    val limitNanos = cutAt.inWholeNanoseconds
    var lastTimeNanos = 0L
    repeatWhileActive {
        if (lastTimeNanos > limitNanos) return@flow
        emit(render(lastTimeNanos))
        lastTimeNanos += intervalNanos
    }
}

private fun Image.toBufferedImage(): BufferedImage {
    val composeBitmap = toComposeImageBitmap()
    return composeBitmap.toAwtImage()
}

private suspend fun FFmpegFrameRecorder.recordAll(images: Flow<Image>) {
    val converter = Java2DFrameConverter()
    var imgNumber = 0
    images.measure { upstreamDuration, downstreamDuration ->
        println("UP :$upstreamDuration")
        println("DOWN :$downstreamDuration")
    }.collect { skiaImage ->
        skiaImage.use {
            converter.getFrame(it.toBufferedImage()).use { frame ->
                frameNumber = imgNumber++
                record(frame, avutil.AV_PIX_FMT_ARGB)
            }
        }
    }
}
