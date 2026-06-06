package com.louiscad.playground.compose.videogen.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SubtitleOverlay(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = Color.White.copy(alpha = 1f),
            shadow = Shadow(color = Color(0xFF__FF00FF).copy(alpha = .3f), offset = Offset(0f, 0f), blurRadius = 4f),
            fontWeight = FontWeight.SemiBold
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}
