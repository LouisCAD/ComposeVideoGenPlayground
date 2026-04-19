package com.louiscad.playground.compose.videogen.core

import okio.FileSystem
import okio.Path

fun readTimecodes(sourceFile: Path, fps: Int): List<Timecode> {
    return readTimecodes(sourceFile, fileSystem = FileSystem.SYSTEM, fps = fps)
}
