package com.louiscad.playground.compose.videogen.core

data class SubtitleItem(
    val text: String,
    val startTime: Timecode,
    val endTime: Timecode,
)
