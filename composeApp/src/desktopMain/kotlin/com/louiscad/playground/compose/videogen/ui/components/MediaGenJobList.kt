package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.louiscad.playground.compose.videogen.MediaGenApp

@Composable
fun MediaGenJobList(app: MediaGenApp) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(app.mediaGenJobs) { item ->
            MediaGenJobListItem(item, Modifier.fillMaxWidth())
        }
    }
}
