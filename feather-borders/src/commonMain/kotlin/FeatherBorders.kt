package com.louiscad.playground.compose.videogen.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

class FeatherBorderColors(color: Color) {
    val colors: List<Color> = listOf(color, color.copy(alpha = 0f))
    val cornerColors: List<Color> = listOf(color.copy(0f), color, color)
}

fun DrawScope.featherBorder(
    pixels: Float,
    featherBorderColors: FeatherBorderColors = FeatherBorderColors(Color.White)
) {
    val topLeftCenter = Offset(pixels, pixels)
    val diameter = pixels * 2f
    val colors = featherBorderColors.colors
    val cornerColors = featherBorderColors.cornerColors
    val cornerGradientsRadius = pixels * 2f
    val cornersArcSize = Size(diameter, diameter) * 2f
    val topLeftGradient = Brush.radialGradient(
        colors = cornerColors,
        center = topLeftCenter,
        radius = cornerGradientsRadius
    )
    drawArc(
        brush = topLeftGradient,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset.Zero - pixels,
        size = cornersArcSize,
        blendMode = BlendMode.Xor
    )
    val topGradient = Brush.verticalGradient(
        colors = colors,
        startY = 0f,
        endY = pixels
    )
    drawRect(
        brush = topGradient,
        topLeft = Offset(x = pixels, y = 0f),
        size = Size(width = size.width - diameter, height = pixels),
        blendMode = BlendMode.Xor
    )
    val topRightCenter = Offset(size.width - pixels, pixels)
    val topRightGradient = Brush.radialGradient(
        colors = cornerColors,
        center = topRightCenter,
        radius = cornerGradientsRadius
    )
    drawArc(
        brush = topRightGradient,
        startAngle = -90f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = topRightCenter.centerAsTopLeft(cornersArcSize),
        size = cornersArcSize,
        blendMode = BlendMode.Xor
    )
    val rightGradient = Brush.horizontalGradient(
        colors = colors,
        startX = size.width,
        endX = size.width - pixels
    )
    drawRect(
        brush = rightGradient,
        topLeft = Offset(x = size.width - pixels, y = pixels),
        size = Size(width = pixels, height = size.height - diameter),
        blendMode = BlendMode.Xor
    )
    val bottomRightCenter = Offset(size.width, size.height) - pixels
    val bottomRightGradient = Brush.radialGradient(
        colors = cornerColors,
        center = bottomRightCenter,
        radius = cornerGradientsRadius
    )
    drawArc(
        brush = bottomRightGradient,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = bottomRightCenter.centerAsTopLeft(cornersArcSize),
        size = cornersArcSize,
        blendMode = BlendMode.Xor
    )
    val bottomGradient = Brush.verticalGradient(
        colors = colors,
        startY = size.height,
        endY = size.height - pixels
    )
    drawRect(
        brush = bottomGradient,
        topLeft = Offset(x = pixels, y = size.height - pixels),
        size = Size(size.width - diameter, pixels),
        blendMode = BlendMode.Xor
    )
    val bottomLeftCenter = Offset(pixels, size.height - pixels)
    val bottomLeftGradient = Brush.radialGradient(
        colors = cornerColors,
        center = bottomLeftCenter,
        radius = cornerGradientsRadius
    )
    drawArc(
        brush = bottomLeftGradient,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = bottomLeftCenter.centerAsTopLeft(cornersArcSize),
        size = cornersArcSize,
        blendMode = BlendMode.Xor
    )
    val leftGradient = Brush.horizontalGradient(
        colors = colors,
        startX = 0f,
        endX = pixels
    )
    drawRect(
        brush = leftGradient,
        topLeft = Offset(0f, pixels),
        size = Size(pixels, size.height - diameter),
        blendMode = BlendMode.Xor
    )
}

private operator fun Offset.plus(amount: Float): Offset = copy(x = x + amount, y = y + amount)
private operator fun Offset.plus(size: Size): Offset = copy(x = x + size.width, y = y + size.height)
private operator fun Offset.minus(amount: Float): Offset = copy(x = x - amount, y = y - amount)

private operator fun Size.minus(pixels: Float): Size {
    return Size(width - pixels, height - pixels)
}


private fun Offset.centerAsTopLeft(size: Size): Offset {
    return copy(x = x - size.width / 2f, y = y - size.height / 2f)
}
