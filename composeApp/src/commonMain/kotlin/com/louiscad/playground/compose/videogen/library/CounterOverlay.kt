package com.louiscad.playground.compose.videogen.library

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.louiscad.playground.compose.videogen.core.helpers.rememberAutoAdvancingNumber
import kotlinx.coroutines.delay
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun CounterOverlay() {
    CounterOverlay(rememberAutoAdvancingNumber(interval = 1.seconds))
}

@Composable
fun CounterOverlay(numberState: IntState) {
    //TODO: Support relative timecodes somehow,
    // so cutting a part doesn't require to redo it all.
    // One way would be to add the offset for each absolute timecode,
    // and allow giving a new timecode somewhere that will lead to readjust everything that follows.
    val number by numberState
    BoxWithConstraints {
        val offset = with(LocalDensity.current) { maxHeight.roundToPx() / 2 }
        val transition = updateTransition(targetState = number, label = "Counter")
        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val enter: EnterTransition
                val exit: ExitTransition
                val xOffset = 0
                val yOffset = offset
                if (targetState > initialState) {
                    enter = slideIn(initialOffset = { IntOffset(xOffset, yOffset) }) + scaleIn()
                    exit = slideOut(targetOffset = { IntOffset(-xOffset, -yOffset) }) + scaleOut()
                } else {
                    enter = slideIn(initialOffset = { IntOffset(-xOffset, -yOffset) }) + scaleIn()
                    exit = slideOut(targetOffset = { IntOffset(xOffset, yOffset) }) + scaleOut()
                }
                enter togetherWith exit
            }
        ) { num ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "$num".padStart(length = 2, padChar = '0'),
                    fontSize = 50.sp,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
fun CoolCounterPreview() {
    val numberState = remember { mutableIntStateOf(0) }
    var number by numberState
    var isResetting by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = { number++ }) {
            Text("+")
        }
        Button(onClick = { number-- }) {
            Text("-")
        }
        Button(onClick = { isResetting = true }) {
            Text("Reset")
        }
        LaunchedEffect(isResetting) {
            if (isResetting) {
                var lastNumber = number
                val increment = -number.sign
                do {
                    number += increment
                    delay(.20.seconds)
                    if (number != lastNumber + increment) {
                        isResetting = false
                        return@LaunchedEffect
                    }
                    lastNumber = number
                } while (lastNumber != 0)
                number = 0
                isResetting = false
            }
        }
        CounterOverlay(numberState)
    }
}
