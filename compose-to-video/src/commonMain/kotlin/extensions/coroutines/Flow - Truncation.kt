package com.louiscad.playground.compose.videogen.core.extensions.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import splitties.coroutines.raceOf

fun <T> Flow<T>.takeUntil(keepRunning: suspend () -> Unit): Flow<T> = channelFlow {
    raceOf(
        { keepRunning() },
        { collect { send(it) } }
    )
    close()
}.buffer(capacity = Channel.RENDEZVOUS)
