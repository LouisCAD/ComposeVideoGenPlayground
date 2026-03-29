package com.louiscad.playground.compose.videogen.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos


//TODO: Finish or replace this class
class TimecodesTrigger(
    val triggersNanos: LongArray,
    val action: (index: Int) -> Unit
)

/**
 * Returns an [IntState] that increments from 0 when [withFrameNanos] reaches the values in [sortedTriggersNanos].
 */
@Composable
fun rememberIncrementCounter(sortedTriggersNanos: LongArray): IntState = remember {
    mutableIntStateOf(0)
}.also {
    if (sortedTriggersNanos.isEmpty()) return@also
    LaunchedEffect(sortedTriggersNanos) {
        var number by it
        var lastTriggerIndex = -1
        do {
            val expectedTriggerNanos = sortedTriggersNanos[lastTriggerIndex + 1]
            val keepGoing = withFrameNanos { nanos ->
                if (nanos >= expectedTriggerNanos) {
                    lastTriggerIndex++
                    number++
                    val nextTriggerNanos = sortedTriggersNanos.getOrElse(lastTriggerIndex + 1) {
                        return@withFrameNanos false
                    }
                    check(nextTriggerNanos >= expectedTriggerNanos)
                }
                true
            }
        } while (keepGoing)
    }
}
