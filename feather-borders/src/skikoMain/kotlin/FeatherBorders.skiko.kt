package com.louiscad.playground.compose.videogen.lib

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Image

fun featherImageScene(imgBytes: ByteArray, pixels: Float): ImageComposeScene {
    val image = Image.makeFromEncoded(imgBytes)
    val bitmap = image.toComposeImageBitmap()
    return ImageComposeScene(
        width = image.width,
        height = image.height,
        density = Density(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(bitmap)
            featherBorder(pixels)
        }
    }
}
