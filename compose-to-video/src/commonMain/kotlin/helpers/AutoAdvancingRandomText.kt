package com.louiscad.playground.compose.videogen.core.helpers

import androidx.compose.runtime.*
import kotlin.random.Random
import kotlin.time.Duration

@Composable
fun rememberAutoAdvancingRandomText(
    interval: Duration,
): State<String?> = remember {
    mutableStateOf<String?>(null)
}.also { state ->
    var text by state
    LaunchedEffect(state) {
        val intervalNanos = interval.inWholeNanoseconds
        var nanosOfLastBump = 0L
        while (true) {
            withFrameNanos { nanos ->
                val nanosSinceLastBump = nanos - nanosOfLastBump
                if (nanosSinceLastBump >= intervalNanos) {
                    text = when (text) {
                        null -> "whatever is the word".repeat(Random.nextInt(1, 5))
                            .toList().shuffled().joinToString(separator = "")
                        else -> null
                    }
                    nanosOfLastBump = nanos
                }
            }
        }
    }
}
