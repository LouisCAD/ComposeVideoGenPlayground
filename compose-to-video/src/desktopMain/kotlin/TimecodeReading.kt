package com.louiscad.playground.compose.videogen.core

import java.io.File

fun readStrictTimecodes(sourceFile: File): List<Timecode> = sourceFile.useLines { lines ->
    lines.mapIndexed { lineIndex, line ->
        val lineNumber = lineIndex + 1
        val parts = line.split(':')
        check(parts.size == 4) {
            "Timecode at $lineNumber must have 4 colon separated parts."
        }
        Timecode(
            hours = parts[0].toInt(),
            minutes = parts[1].toInt(),
            seconds = parts[2].toInt(),
            frame = parts[3].toInt()
        )
    }.toList()
}

fun readTimecodes(sourceFile: File): List<Timecode> = sourceFile.useLines { lines ->
    var lastHours = 0
    var lastMinutes = 0
    var lastSeconds = 0
    lines.mapIndexedNotNull { lineIndex, line ->
        if (line.isBlank() || line.isComment()) return@mapIndexedNotNull null
        val lineNumber = lineIndex + 1
        val parts = line.split(':').toMutableList()
        check(parts.size in 1..4) {
            "Timecode at $lineNumber must have 1 to 4 colon separated parts."
        }
        val frame: Int
        if (line.startsWith('+')) {
            TODO("Relative timecodes are not supported yet.")
        } else {
            frame = parts.removeLast().toInt()
            lastSeconds = parts.removeLastOrNull()?.toIntOrNull() ?: lastSeconds
            lastMinutes = parts.removeLastOrNull()?.toIntOrNull() ?: lastMinutes
            lastHours = parts.removeLastOrNull()?.toIntOrNull() ?: lastHours
        }
        Timecode(
            hours = lastHours,
            minutes = lastMinutes,
            seconds = lastSeconds,
            frame = frame
        )
    }.toList()
}

private fun String.isComment(): Boolean {
    return startsWith('#') || startsWith("//")
}
