package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

internal class MediaGenAppImpl(private val scope: CoroutineScope) : MediaGenApp() {

    private val jobsStateList = SnapshotStateList<Deferred<Result<Unit>>>()

    override fun addJob(
        jobInfo: MediaGenJob,
        block: suspend () -> Unit
    ) {
        jobsStateList += scope.async { runCatching { block() } }
        mediaGenJobsStateList += jobInfo
    }
}
