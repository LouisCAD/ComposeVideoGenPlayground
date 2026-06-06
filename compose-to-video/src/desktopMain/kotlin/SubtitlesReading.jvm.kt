package com.louiscad.playground.compose.videogen.core

import okio.FileSystem
import okio.Path

fun readSubtitles(sourceFile: Path, fps: Int): List<SubtitleItem> {
    return readSubtitles(sourceFile, fileSystem = FileSystem.SYSTEM, fps = fps)
}
