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

@Composable
fun rememberIncrementCounter(sortedTriggersNanos: LongArray): IntState = remember {
    mutableIntStateOf(0)
}.also {
    if (sortedTriggersNanos.isEmpty()) return@also
    var number by it
    LaunchedEffect(Unit) {
        var lastTriggerIndex = -1
        do {
            val expectedTriggerNanos = sortedTriggersNanos[lastTriggerIndex + 1]
            val keepGoing = withFrameNanos { nanos ->
                if (nanos >= expectedTriggerNanos) {
                    lastTriggerIndex++
                    val nextTriggerNanos = sortedTriggersNanos.getOrElse(lastTriggerIndex + 1) {
                        return@withFrameNanos false
                    }
                    check(nextTriggerNanos >= expectedTriggerNanos)
                    number++
                }
                true
            }
        } while (keepGoing)
    }
}
