package com.louiscad.playground.compose.videogen.core

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

//TODO: Provide ways to convert between absolute timecodes and relative offsets from middle timecodes.

fun Timecode.toNanosOffset(framesPerSecond: Int): Long {
    val frameOffset = (1.seconds.inWholeNanoseconds * frame / framesPerSecond).nanoseconds
    return (hours.hours + minutes.minutes + seconds.seconds + frameOffset).inWholeNanoseconds
}
