package com.louiscad.playground.compose.videogen.core.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.time.Duration

@Composable
fun rememberAutoAdvancingNumber(interval: Duration): IntState = remember {
    mutableIntStateOf(0)
}.also {
    var number by it
    LaunchedEffect(Unit) {
        val intervalNanos = interval.inWholeNanoseconds
        var nanosOfLastBump = 0L
        while (true) {
            withFrameNanos { nanos ->
                val nanosSinceLastBump = nanos - nanosOfLastBump
                if (nanosSinceLastBump >= intervalNanos) {
                    nanosOfLastBump = nanos
                    number++
                }
            }
        }
    }
}
