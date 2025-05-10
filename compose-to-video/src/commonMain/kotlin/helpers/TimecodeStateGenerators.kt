package com.louiscad.playground.compose.videogen.core.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.louiscad.playground.compose.videogen.core.Timecode
import com.louiscad.playground.compose.videogen.core.toNanosOffset
import kotlin.time.Duration

@Composable
fun rememberTimecodesCounter(
    framesPerSecond: Int,
    sortedTimecodes: List<Timecode>
): IntState = remember { mutableIntStateOf(0) }.also {
    var number by it
    LaunchedEffect(Unit) {
        while (true) {
            val timecode = sortedTimecodes.getOrElse(number) { return@LaunchedEffect }
            withFrameNanos { nanos ->
                val timeCodeNanos = timecode.toNanosOffset(framesPerSecond = framesPerSecond)
                if (nanos >= timeCodeNanos) number++
            }
        }
    }
}
