package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.core.helpers.toStepState
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.library.MercedesVsBacchetta

val mediaGeneratorItems: List<MediaGeneratorItem> = listOf(
    MediaGeneratorItem(
        name = "Counter overlay",
        defaultSize = IntSize(100, 100),
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter -> CounterOverlay(counter) }
    ),
    MediaGeneratorItem(
        name = "Mercedes vs Bacchetta Scoreboard overlay",
        defaultSize = IntSize(width = 2160, height = 480),
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter -> MercedesVsBacchetta(counter.toStepState()) }
    ),
)

data class MediaGeneratorItem(
    val name: String,
    val defaultSize: IntSize,
    val content: Content,
) {
    sealed interface Content {
        data class CounterBasedVideo(
            val content: @Composable (counter: IntState) -> Unit
        ) : Content
        //TODO: Support still images, music…
    }
}

@Composable
fun MediaGeneratorListItem(
    data: MediaGeneratorItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        Text(text = data.name, style = MaterialTheme.typography.titleMedium)
        //TODO: Show the default size (text)
        //TODO: Show a preview with the right aspect ratio
        //TODO: Allow opening the video gen setup UI
    }
}
