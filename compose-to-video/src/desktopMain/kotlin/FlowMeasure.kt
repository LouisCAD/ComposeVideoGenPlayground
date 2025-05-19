package com.louiscad.playground.compose.videogen.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

fun <T> Flow<T>.measure(onComplete:(upstreamDuration: Duration, downstreamDuration:Duration) -> Unit): Flow<T> = flow {
    var collectNanos = 0L
    var emitNanos = 0L
    var timeBeforeCollect: Long = System.nanoTime()
    try {
        collect {
            val afterCollectBeforeEmit = System.nanoTime()
            collectNanos += (afterCollectBeforeEmit - timeBeforeCollect)
            emit(it)
            val afterEmit = System.nanoTime()
            emitNanos += (afterEmit - afterCollectBeforeEmit)
            timeBeforeCollect = afterEmit
        }
    } finally {
        onComplete(collectNanos.nanoseconds, emitNanos.nanoseconds)
    }
}
