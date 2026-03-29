package com.louiscad.playground.compose.videogen.core.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.enums.enumEntries
import kotlin.time.Duration

@Composable
fun rememberAutoAdvancingNumber(
    interval: Duration,
    startValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE,
): IntState = remember(startValue) {
    mutableIntStateOf(startValue)
}.also { intState ->
    var number by intState
    LaunchedEffect(intState) {
        val intervalNanos = interval.inWholeNanoseconds
        var nanosOfLastBump = 0L
        while (true) {
            withFrameNanos { nanos ->
                val nanosSinceLastBump = nanos - nanosOfLastBump
                if (nanosSinceLastBump >= intervalNanos) {
                    when (number) {
                        maxValue -> number = startValue
                        else -> number++
                    }
                    nanosOfLastBump = nanos
                }
            }
        }
    }
}

@Composable
inline fun <reified T> IntState.toStepState(): State<T> where T : Enum<T>, T : StepBase {
    return remember(this) {
        val entries = enumEntries<T>()
        derivedStateOf { entries.getOrElse(this.value) { entries.last() } }
    }
}

@Composable
inline fun <reified T> rememberAutoAdvancingStepsState(): State<T> where T : Enum<T>, T : StepBase {
    val entries = enumEntries<T>()
    return produceState(initialValue = entries.first()) {
        var nanosOfLastBump = 0L
        while (true) {
            repeat(entries.size) { index ->
                value = entries[index]
                val nanosBeforeNext = entries[index].durationBeforeNextStep.inWholeNanoseconds
                do {
                    val done = withFrameNanos { nanos ->
                        val nanosSinceLastBump = nanos - nanosOfLastBump
                        if (nanosSinceLastBump >= nanosBeforeNext) {
                            nanosOfLastBump = nanos
                            true
                        } else false
                    }
                } while (!done)
            }
        }
    }
}
