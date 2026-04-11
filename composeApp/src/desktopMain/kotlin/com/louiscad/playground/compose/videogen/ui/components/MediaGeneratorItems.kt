package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import com.louiscad.playground.compose.videogen.MediaGenApp
import com.louiscad.playground.compose.videogen.core.helpers.toStepState
import com.louiscad.playground.compose.videogen.core.rememberIncrementCounter
import com.louiscad.playground.compose.videogen.extensions.toAspectRatio
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.library.MercedesVsBacchetta

val mediaGeneratorItems: List<MediaGeneratorItem> = listOf(
    MediaGeneratorItem(
        name = "Counter overlay",
        defaultSize = IntSize(100, 100),
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter ->
            ProvideTextStyle(
                MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Medium)
            ) {
                CounterOverlay(counter)
            }
        }
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
fun MediaGeneratorList(
    modifier: Modifier = Modifier,
    onItemClicked: (MediaGeneratorItem) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        items(items = mediaGeneratorItems) { item ->
            MediaGeneratorListItem(
                data = item,
                modifier = Modifier.clickable(
                    onClick = { onItemClicked(item) }
                ).fillMaxWidth()
            )
        }
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
        val sizeText = remember(data.defaultSize) { with(data.defaultSize) { "$width × $height" } }
        val aspectRatio = remember(data.defaultSize) { data.defaultSize.toAspectRatio() }
        val aspectRatioText = remember(aspectRatio) { with(aspectRatio) { "$width:$height" } }
        Text(text = data.name, style = MaterialTheme.typography.titleMedium)
        Text(sizeText, style = MaterialTheme.typography.bodyMedium)
        Text(aspectRatioText, style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier.background(Color.Magenta).aspectRatio(
                ratio = aspectRatio.run { width.toFloat() / height },
                matchHeightConstraintsFirst = true
            )
        ) {
            //TODO: Show a preview with the right aspect ratio
        }
    }
}
