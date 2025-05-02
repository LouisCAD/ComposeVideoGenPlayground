package com.louiscad.playground.compose.videogen.lib

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A simple composable component from the library module.
 */
@Composable
fun LibraryComponent(message: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Library Component")
        Text(message)
    }
}
