package com.louiscad.playground.compose.videogen.extensions

import androidx.compose.ui.unit.IntSize

/**
 * Returns the simplified aspect ratio as a Pair of integers
 */
internal fun IntSize.toAspectRatio(): IntSize {
    val gcd = findGcd(width, height)
    return IntSize(
        width = (width / gcd),
        height = (height / gcd)
    )
}

/** Calculates the Greatest Common Divisor using the Euclidean Algorithm */
private tailrec fun findGcd(a: Int, b: Int): Int {
    return if (b == 0) a else findGcd(b, a % b)
}
