package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList

abstract class MediaGenApp {

    abstract fun addJob(
        jobInfo: MediaGenJob,
        block: suspend () -> Unit
    )

    protected val mediaGenJobsStateList = SnapshotStateList<MediaGenJob>()

    @Stable
    val mediaGenJobs: List<MediaGenJob> get() = mediaGenJobsStateList

    @Stable
    val isGeneratingMedia by derivedStateOf { mediaGenJobs.all { it.status is MediaGenJob.Status.Done } }
}
