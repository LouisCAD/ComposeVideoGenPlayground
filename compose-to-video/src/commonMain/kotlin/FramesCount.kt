package com.louiscad.playground.compose.videogen.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun framesCountFor(
    duration: Duration,
    framesPerSecond: Int
): Long {
    // We separate whole seconds to avoid cumulating approximations
    // that could lead to an inaccurate result.
    val wholeSeconds = duration.inWholeSeconds
    return wholeSeconds * framesPerSecond + run {
        val wholeSecondsPart = wholeSeconds.seconds
        if (duration == wholeSecondsPart) return@run 0L
        val remainder = duration - wholeSecondsPart
        (remainder * framesPerSecond / 1.seconds).toLong()
    }
}

fun framesCountFor(
    duration: Duration,
    framesPerSecond: Double
): Long {
    return (duration * framesPerSecond / 1.seconds).toLong()
}
