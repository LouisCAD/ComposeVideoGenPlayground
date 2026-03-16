package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import kotlin.time.Duration

@Stable
data class MediaGenJob(
    val title: String,
    val status: State<Status>,
    val statusRow: @Composable () -> Unit
) {
    sealed interface Status {
        val timeSpent: Duration

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
