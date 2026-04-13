package com.louiscad.playground.compose.videogen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.louiscad.playground.compose.videogen.core.helpers.StepBase
import com.louiscad.playground.compose.videogen.core.helpers.rememberAutoAdvancingStepsState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun MercedesVsBacchetta(stepState: State<SportScoreStep>) {
    SportsStyleScoreOverlay(
        player1 = Opponent("Mercedes", 0),
        player2 = Opponent("Bacchetta", 1),
        stepState = stepState,
        textColor = Color.Black,
        borderColor = Color.White
    )
}

data class Opponent(
    val name: String,
    val score: Int,
)

@Composable
private fun SportsStyleScoreOverlay(
    player1: Opponent,
    player2: Opponent,
    stepState: State<SportScoreStep>,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val step by stepState
    val scoreboardShape = RoundedCornerShape(
        bottomStartPercent = 50,
        bottomEndPercent = 50,
        topStartPercent = 25,
        topEndPercent = 25,
    )

    Row(
        Modifier.padding(16.dp)
            .graphicsLayer {
                shape = scoreboardShape
                shadowElevation = 10.dp.toPx()
                ambientShadowColor = Color.Red
                spotShadowColor = Color(0xFF_FF00FF)
            }.background(borderColor, scoreboardShape)
            .height(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val namesTextStyle = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontWeight = FontWeight.Bold,
        )
        val scoreTextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontSize = 20.sp)

        val scoresAlpha by animateFloatAsState(targetValue = if (step.isAtLeast(SportScoreStep.ShowScore)) 1f else 0f)
        val middleSeparatorAlpha by animateFloatAsState(targetValue = if (step.isAtLeast(SportScoreStep.Split)) 1f else 0f)
        val playerNamesAlpha by animateFloatAsState(targetValue = if (step.isAtLeast(SportScoreStep.ShowOpponentNames)) 1f else 0f)

        val charWidth = with(LocalDensity.current) { scoreTextStyle.fontSize.toDp() }
        val padding = 4.dp

        val playerNameModifier = Modifier.width(100.dp).padding(padding).graphicsLayer { alpha = playerNamesAlpha }
        val scoreModifier = Modifier.padding(padding).width(charWidth).graphicsLayer { alpha = scoresAlpha }
        val middleSeparatorModifier = Modifier.graphicsLayer { alpha = middleSeparatorAlpha }

        AnimatedVisibility(visible = step.isAtLeast(SportScoreStep.ExpandForOpponents)) {
            Text(player1.name, playerNameModifier, style = namesTextStyle, textAlign = TextAlign.End)
        }
        Text(player1.score.toString(), scoreModifier, style = scoreTextStyle, textAlign = TextAlign.Center)

        VerticalLightSeparator(1.dp, Color.Black.copy(alpha = .3f), middleSeparatorModifier)

        Text(player2.score.toString(), scoreModifier, style = scoreTextStyle, textAlign = TextAlign.Center)
        AnimatedVisibility(visible = step.isAtLeast(SportScoreStep.ExpandForOpponents)) {
            Text(player2.name, playerNameModifier, style = namesTextStyle, textAlign = TextAlign.Start)
        }
    }
}


enum class SportScoreStep(override val durationBeforeNextStep: Duration = .3.seconds) : StepBase {
    Initial(1.seconds),
    Split(.1.seconds),
    ExpandForOpponents(.3.seconds),
    ShowOpponentNames(.1.seconds),
    ShowScore(1.seconds),
    ;

    fun isAtLeast(step: SportScoreStep): Boolean = ordinal >= step.ordinal
}

@Composable
private fun VerticalLightSeparator(borderSize: Dp, borderColor: Color, modifier: Modifier = Modifier) {
    val transparent = borderColor.copy(alpha = 0f)
    val brush = Brush.verticalGradient(
        .1f to transparent,
        .3f to borderColor,
        .7f to borderColor,
        .9f to transparent,
    )
    Spacer(modifier.background(brush).width(borderSize).fillMaxHeight())
}

@Preview
@Composable
fun MercedesVsBacchettaPreview() {
    MercedesVsBacchetta(rememberAutoAdvancingStepsState())
}
