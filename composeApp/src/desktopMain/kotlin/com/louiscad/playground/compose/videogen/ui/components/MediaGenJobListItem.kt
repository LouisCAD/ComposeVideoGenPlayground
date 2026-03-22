package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.AppDefaults
import com.louiscad.playground.compose.videogen.MediaGenJob

@Composable
fun MediaGenJobListItem(
    job: MediaGenJob,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(job.title)
            Spacer(Modifier.weight(1f))
            Text("${job.status.timeSpent.inWholeSeconds}s")
        }
        StatusLine(job.status)
        job.statusRow()
    }
}

@Composable
private fun StatusLine(
    status: MediaGenJob.Status,
    modifier: Modifier = Modifier
) = Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    when (status) {
        is MediaGenJob.Status.Done -> Text("Done!")
        is MediaGenJob.Status.Enqueued -> {
            Text("Enqueued")
            IconButton(onClick = status.startNow) {
                Icon(AppDefaults.icons.PlayCircle, contentDescription = "Start now")
            }
        }
        is MediaGenJob.Status.Paused -> {
            Text("Paused")
            IconButton(onClick = status.resume) {
                Icon(AppDefaults.icons.PlayCircle, contentDescription = "Resume")
            }
        }
        is MediaGenJob.Status.Running -> {
            Text("Running")
            CircularProgressIndicator(progress = status.completionRatio)
            IconButton(onClick = status.pause ?: {}, enabled = status.pause != null) {
                Icon(AppDefaults.icons.PauseCircle, contentDescription = "Pause")
            }
        }
    }
}
