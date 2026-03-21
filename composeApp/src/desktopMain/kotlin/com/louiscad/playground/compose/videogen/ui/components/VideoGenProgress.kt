package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.VideoGeneratorUi.FramesGenerationProgress
import com.louiscad.playground.compose.videogen.core.FfmpegProgressLine
import kotlinx.coroutines.flow.Flow

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
