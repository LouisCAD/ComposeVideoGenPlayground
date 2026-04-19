package com.louiscad.playground.compose.videogen.core

fun parseTimeCodeOrNull(text: String): Timecode? {
    val parts = text.trim().split(':')
    if (parts.size !in 1..4) return null
    val frame = parts.last().toIntOrNull() ?: return null
    val seconds = parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(parts.size - 3)?.toIntOrNull() ?: 0
    val hours = parts.getOrNull(parts.size - 4)?.toIntOrNull() ?: 0
    return Timecode(
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        frame = frame
    )
}
