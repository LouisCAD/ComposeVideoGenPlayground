package com.louiscad.playground.compose.videogen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun ProvideDensity(density: Density, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides density, content = content)
}

@Composable
fun ProvideRelativeDensity(density: Density, content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    val newDensity = Density(
        density = currentDensity.density * density.density,
        fontScale = currentDensity.fontScale * density.fontScale,
    )
    CompositionLocalProvider(LocalDensity provides newDensity, content = content)
}
