package com.louiscad.playground.compose.videogen.lib

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.use

fun copyWithFeatheredBorders(
    source: File,
    pixels: Float,
    getOutputFile: (input: File) -> File
) {
    val bitmap = featherImageScene(source, pixels).use {
        Bitmap.makeFromImage(it.render())
    }
    val data = Image.makeFromBitmap(bitmap).use { image ->
        image.encodeToData(EncodedImageFormat.PNG)!!
    }
    val out = getOutputFile(source)
    out.writeBytes(data.bytes)
}

fun featherImageScene(imgSource: File, pixels: Float): ImageComposeScene {
    return featherImageScene(imgSource.readBytes(), pixels)
}
