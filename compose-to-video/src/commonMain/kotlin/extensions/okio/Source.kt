package com.louiscad.playground.compose.videogen.core.extensions.okio

import okio.Source
import okio.buffer
import okio.use

fun <T> Source.useLines(block: (Sequence<String>) -> T): T = use {
    val sequence = sequence {
        val buffer = it.buffer()
        while (true) yield(buffer.readUtf8Line() ?: return@sequence)
    }
    block(sequence)
}
