package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import kotlin.time.Duration

@Stable
data class MediaGenJob(
    val title: String,
    val statusState: State<Status>,
    val statusRow: @Composable () -> Unit
) {

    val status: Status by statusState

    sealed interface Status {
        val timeSpent: Duration
        //TODO: Introduce creation time, once we support persistence.

        data object Enqueued : Status {
            override val timeSpent: Duration get() = Duration.ZERO
        }

        data class Paused(
            override val timeSpent: Duration,
        ) : Status

        data class Running(
            override val timeSpent: Duration,
            val estimatedTimeRemaining: Duration?,
            val completionRatio: Float,
        ) : Status

        data class Done(
            override val timeSpent: Duration,
        ) : Status
    }
}
