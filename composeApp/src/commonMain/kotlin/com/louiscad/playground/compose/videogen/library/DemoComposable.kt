package com.louiscad.playground.compose.videogen.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp

/**
 * Demo Composable that shows how to use the frame time to render animations.
 */
@Composable
fun DemoComposable() {
    val color by produceState(Color.White) {
        while (true) withFrameMillis { frameTimeMillis ->
            val fraction = (frameTimeMillis % 2000f) / 2000f
//            println(fraction)
            value = lerp(Color.Magenta, Color.Cyan, fraction)
        }
    }
    val angle by produceState(0f) {
        while (true) withFrameMillis { frameTimeMillis ->
            val fraction = (frameTimeMillis % 2000f) / 2000f
//            println(fraction)
            value = 360f * fraction
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        rotate(angle) {
            drawCircle(color, center = center.div(2f), radius = size.minDimension / 4f)
        }
    }
}
