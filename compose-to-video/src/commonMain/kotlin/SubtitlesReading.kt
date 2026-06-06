package com.louiscad.playground.compose.videogen.core

import androidx.compose.ui.util.lerp
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

fun readSubtitles(
    sourceFile: Path,
    fileSystem: FileSystem,
    fps: Int = 60,
): List<SubtitleItem> = fileSystem.source(sourceFile).use { source ->
    val format = SubtitleFormat.entries.first { it.extension == sourceFile.name.substringAfterLast('.') }
    readSubtitles(source.buffer().readUtf8(), format, fps)
}

internal fun readSubtitles(fileContent: String, format: SubtitleFormat, fps: Int): List<SubtitleItem> = buildList {
    val lines = fileContent.lines().map { it.trim() }
    var lineIndex = 0
    val mustHaveCounterLine = when (format) {
        SubtitleFormat.SubRip -> true
        SubtitleFormat.YouTubeSbv -> false
    }
    var counter = 0
    while (lineIndex < lines.size) {
        if (lines[lineIndex].isEmpty()) {
            lineIndex++
            continue
        }
        counter++
        if (mustHaveCounterLine) {
            require(lines[lineIndex].toInt() == counter)
            lineIndex++
        }
        val (start, end) = parseTimeCodeRange(format, fps, lines[lineIndex++])
        val text = buildList {
            while (lineIndex < lines.size && lines[lineIndex].isNotEmpty()) add(lines[lineIndex++])
        }.joinToString("\n")
        add(SubtitleItem(text, start, end))
    }
}

private fun parseTimeCodeRange(
    format: SubtitleFormat,
    fps: Int,
    line: String
): Pair<Timecode, Timecode> {
    val startEndSeparator = when (format) {
        SubtitleFormat.SubRip -> " --> "
        SubtitleFormat.YouTubeSbv -> ","
    }
    val unparsedStartTimecode = line.substringBefore(startEndSeparator)
    val unparsedEndTimecode = line.substringAfter(startEndSeparator)
    val startTimecode = parseTimeCode(format, fps, unparsedStartTimecode)
    val endTimecode = parseTimeCode(format, fps, unparsedEndTimecode)
    return startTimecode to endTimecode
}

private fun parseTimeCode(
    format: SubtitleFormat,
    fps: Int,
    line: String
): Timecode {
    val millisSeparator = when (format) {
        SubtitleFormat.SubRip -> ','
        SubtitleFormat.YouTubeSbv -> '.'
    }
    val parts = line.split(':')
    check(parts.size == 3) {
        "Timecode must have 3 colon separated parts."
    }
    val (seconds, millis) = parts[2].split(millisSeparator)
    return Timecode(
        hours = parts[0].toInt(),
        minutes = parts[1].toInt(),
        seconds = seconds.toInt(),
        frame = lerp(start = 0, stop = fps, fraction = millis.toFloat() / 1000f)
    )
}
