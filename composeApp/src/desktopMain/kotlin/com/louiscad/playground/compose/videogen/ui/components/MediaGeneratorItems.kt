package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.components.ProvideRelativeDensity
import com.louiscad.playground.compose.videogen.core.helpers.rememberAutoAdvancingNumber
import com.louiscad.playground.compose.videogen.core.helpers.rememberAutoAdvancingStepsState
import com.louiscad.playground.compose.videogen.core.helpers.toStepState
import com.louiscad.playground.compose.videogen.extensions.toAspectRatio
import com.louiscad.playground.compose.videogen.library.CounterOverlay
import com.louiscad.playground.compose.videogen.library.MercedesVsBacchetta
import kotlin.time.Duration.Companion.seconds

val mediaGeneratorItems: List<MediaGeneratorItem> = listOf(
    MediaGeneratorItem(
        name = "Counter overlay",
        defaultSize = IntSize(100, 100),
        defaultDensity = 1f,
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter ->
            ProvideTextStyle(
                MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Medium)
            ) {
                CounterOverlay(counter ?: rememberAutoAdvancingNumber(.5.seconds, maxValue = 22))
            }
        }
    ),
    MediaGeneratorItem(
        name = "Mercedes vs Bacchetta Scoreboard overlay",
        defaultSize = IntSize(width = 720, height = 160),
        defaultDensity = 3f,
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter ->
            ProvideRelativeDensity(Density(2.5f)) {
                MercedesVsBacchetta(counter?.toStepState() ?: rememberAutoAdvancingStepsState())
            }
        }
    ),
    MediaGeneratorItem(
        name = "Mercedes vs Bacchetta Scoreboard overlay (full screen)",
        defaultSize = IntSize(width = 2160, height = 3840),
        defaultDensity = 1f, //TODO: Do we need an explicit, or inferred preview density?
        previewScale = 1 / 8f,
        content = MediaGeneratorItem.Content.CounterBasedVideo { counter ->
            ProvideRelativeDensity(Density(7f)) {
                MercedesVsBacchetta(counter?.toStepState() ?: rememberAutoAdvancingStepsState())
            }
        }
    ),
)

data class MediaGeneratorItem(
    val name: String,
    val defaultSize: IntSize,
    val defaultDensity: Float,
    val previewScale: Float = 1f,
    val content: Content,
) {

    val preview: @Composable () -> Unit = when (content) {
        is Content.CounterBasedVideo -> { -> content.content(null) }
    }

    sealed interface Content {
        data class CounterBasedVideo(
            val content: @Composable (counter: IntState?) -> Unit
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
) = Column(modifier) {
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.padding(horizontal = 16.dp),) {
        Text(text = data.name, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sizeText = remember(data.defaultSize) { with(data.defaultSize) { "$width × $height" } }
            val aspectRatio = remember(data.defaultSize) { data.defaultSize.toAspectRatio() }
            val aspectRatioText = remember(aspectRatio) { with(aspectRatio) { "$width:$height" } }
            Text(sizeText, style = MaterialTheme.typography.bodyMedium)
            Text(aspectRatioText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            val previewHeight = 72f
            Box(
                Modifier.background(
                    color = Color.LightGray.copy(alpha = .3f),
                    shape = RoundedCornerShape(4.dp)
                ).height(previewHeight.dp).aspectRatio(
                    ratio = aspectRatio.run { width.toFloat() / height },
                    matchHeightConstraintsFirst = true
                ),
                contentAlignment = Alignment.Center
            ) {
                ProvideRelativeDensity(Density(previewHeight / data.defaultSize.height)) {
                    data.preview()
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(Modifier.fillMaxWidth())
}
