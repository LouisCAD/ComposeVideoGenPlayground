package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.VideoGeneratorUi.FramesGenerationProgress
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Composable
fun VideoGenProgressLine(
    framesGenerationProgress: FramesGenerationProgress,
    videoEncodingProgress: Flow<FfmpegProgressLine>,
    modifier: Modifier = Modifier,
) = Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    FramesGenerationProgressLine(framesGenerationProgress, Modifier.weight(1f))
    VideoEncodingGenerationProgressLine(framesGenerationProgress, videoEncodingProgress, Modifier.weight(1f))
}

@Composable
private fun FramesGenerationProgressLine(
    framesGenerationProgress: FramesGenerationProgress,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    Row {
        Text("Frames Generation")
        Spacer(Modifier.weight(1f))
        Text(text = with(framesGenerationProgress) { "$writtenFrames/$totalFrames" }, textAlign = TextAlign.End)
    }
    LinearProgressIndicator(
        progress = framesGenerationProgress.let { it.writtenFrames.toFloat() / it.totalFrames },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VideoEncodingGenerationProgressLine(
    framesGenerationProgress: FramesGenerationProgress,
    videoEncodingProgress: Flow<FfmpegProgressLine>,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    val encodingProgressLine: FfmpegProgressLine? by videoEncodingProgress.collectAsState(initial = null)
    val ratio = encodingProgressLine?.let {
        it.frameNumber.toFloat() / framesGenerationProgress.totalFrames
    } ?: 0f
    Row {
        Text("Video Encoding")
        Spacer(Modifier.weight(1f))
        val encodedFramesCount = encodingProgressLine?.frameNumber ?: 0
        Text(text = with(framesGenerationProgress) { "$encodedFramesCount/$totalFrames" }, textAlign = TextAlign.End)
    }
    LinearProgressIndicator(
        progress = ratio,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun VideoGenProgressCards(
    framesGenerationProgress: FramesGenerationProgress,
    videoEncodingProgress: Flow<FfmpegProgressLine>,
    modifier: Modifier = Modifier,
): Unit = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    val encodingProgressLine: FfmpegProgressLine? by videoEncodingProgress.collectAsState(initial = null)
    Text("Progress", style = MaterialTheme.typography.h2)
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Frames generation", style = MaterialTheme.typography.h2)
            LinearProgressIndicator(
                progress = framesGenerationProgress.let {
                    it.writtenFrames.toFloat() / it.totalFrames
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Text("Frames:", style = MaterialTheme.typography.h6)
                Spacer(Modifier.weight(1f))
                Text(
                    text = with(framesGenerationProgress) { "$writtenFrames/$totalFrames" },
                    textAlign = TextAlign.End
                )
            }
        }
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Encoding", style = MaterialTheme.typography.h2)
            val ratio = encodingProgressLine?.let {
                it.frameNumber.toFloat() / framesGenerationProgress.totalFrames
            } ?: 0f
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Text("Speed:", style = MaterialTheme.typography.h6)
                Text(text = encodingProgressLine?.let { "${it.speedFactor}x" } ?: "-")
            }
            Row {
                Text("Frames throughput:", style = MaterialTheme.typography.h6)
                Spacer(Modifier.weight(1f))
                Text(text = encodingProgressLine?.let { "${it.fps}fps" } ?: "-", textAlign = TextAlign.End)
            }
        }
    }
}

@Preview
@Composable
private fun VideoGenProgressLinePreview() {
    VideoGenProgressLine(
        framesGenerationProgress = PreviewParams.framesGenerationProgress,
        videoEncodingProgress = PreviewParams.videoEncodingProgress,
    )
}

@Preview
@Composable
private fun VideoGenProgressCardsPreview() {
    VideoGenProgressCards(
        framesGenerationProgress = PreviewParams.framesGenerationProgress,
        videoEncodingProgress = PreviewParams.videoEncodingProgress,
    )
}

private object PreviewParams {
    val framesGenerationProgress = FramesGenerationProgress.invoke(
        totalFrames = 60 * 10,
        writtenFramesState = mutableIntStateOf(60 * 10)
    )
    val videoEncodingProgress = flow {
        val line = FfmpegProgressLine(
            frameNumber = 65,
            fps = 112.0,
            quality = 1.0,
            sizeKiB = 1024L,
            time = FfmpegProgressLine.Time(hours = 0, minutes = 0, seconds = .5),
            bitrateKpbs = 100.0,
            speedFactor = 1.92f
        )
        emit(line)
    }
}
