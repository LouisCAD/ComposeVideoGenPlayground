package com.louiscad.playground.compose.videogen.core

import com.louiscad.playground.compose.videogen.core.extensions.okio.useLines
import okio.FileSystem
import okio.Path

fun readStrictTimecodes(
    sourceFile: Path,
    fileSystem: FileSystem
): List<Timecode> = fileSystem.source(sourceFile).useLines { lines ->
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

fun readTimecodes(
    sourceFile: Path,
    fileSystem: FileSystem,
    fps: Int = 60,
): List<Timecode> = fileSystem.source(sourceFile).useLines { lines ->
    readTimecodes(lines, fps)
}

fun readTimecodes(lines: Sequence<String>, fps: Int): List<Timecode> {
    var lastTimecode = Timecode(0, 0, 0, 0)
    return lines.mapIndexedNotNull { lineIndex, line ->
        if (line.isBlank() || line.isComment()) return@mapIndexedNotNull null
        val lineNumber = lineIndex + 1
        if (line.startsWith('+')) {
            val parts = line.substring(startIndex = 1).substringBefore("//").split(':').toMutableList()
            check(parts.size in 1..4) {
                "Relative timecode at $lineNumber must have 1 to 4 colon separated parts."
            }
            lastTimecode.plus(
                frame = parts.removeLast().trimEnd().toInt(),
                seconds = parts.removeLastOrNull()?.toIntOrNull(),
                minutes = parts.removeLastOrNull()?.toIntOrNull(),
                hours = parts.removeLastOrNull()?.toIntOrNull(),
                fps = fps,
            )
        } else {
            val parts = line.substringBefore("//").split(':').toMutableList()
            check(parts.size in 1..4) {
                "Timecode at $lineNumber must have 1 to 4 colon separated parts."
            }
            lastTimecode.withUpdate(
                frame = parts.removeLast().trimEnd().toInt(),
                seconds = parts.removeLastOrNull()?.toIntOrNull(),
                minutes = parts.removeLastOrNull()?.toIntOrNull(),
                hours = parts.removeLastOrNull()?.toIntOrNull(),
            )
        }.also { lastTimecode = it }
    }.toList()
}

internal fun Timecode.plus(
    hours: Int? = null,
    minutes: Int? = null,
    seconds: Int? = null,
    frame: Int,
    fps: Int,
): Timecode {
    if (minutes != null) require(minutes in 0..59) { "minutes must be between 0 and 59: $minutes" }
    if (seconds != null) require(seconds in 0..59) { "seconds must be between 0 and 59: $seconds" }
    require(frame < fps) { "frame($frame) must be below fps($fps)" }
    val totalFrames = this.frame + frame
    val actualFrame = totalFrames % fps

    val totalSeconds = this.seconds + (seconds ?: 0) + (totalFrames / fps)
    val actualSeconds = totalSeconds % 60

    val totalMinutes = this.minutes + (minutes ?: 0) + (totalSeconds / 60)
    val actualMinutes = totalMinutes % 60
    return Timecode(
        hours = this.hours + (hours ?: 0) + (totalMinutes / 60),
        minutes = actualMinutes,
        seconds = actualSeconds,
        frame = actualFrame
    )
}

private fun Timecode.withUpdate(
    hours: Int? = null,
    minutes: Int? = null,
    seconds: Int? = null,
    frame: Int,
): Timecode = Timecode(
    hours = hours ?: this.hours,
    minutes = minutes ?: this.minutes,
    seconds = seconds ?: this.seconds,
    frame = frame
)

private fun String.isComment(): Boolean {
    return startsWith('#') || startsWith("//")
}
