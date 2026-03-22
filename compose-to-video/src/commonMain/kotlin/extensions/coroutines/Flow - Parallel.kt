package com.louiscad.playground.compose.videogen.core.extensions.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

suspend fun <T> Flow<T>.collectParallel(
    maxParallelism: Int,
    bufferCapacity: Int = maxParallelism,
    transform: suspend (T) -> Unit
) {
    coroutineScope {
        val inputChannel = Channel<T>(capacity = bufferCapacity)
        launch {
            collect { inputChannel.send(it) }
            inputChannel.close()
        }
        for (i in 0..<maxParallelism) launch {
            for (element in inputChannel) transform(element)
        }
    }
}
