package com.louiscad.playground.compose.videogen.extensions

import java.awt.Dimension
import java.awt.Toolkit
import java.beans.PropertyChangeListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Insets

/**
 * Returns a Flow that emits the current screen [Dimension] 
 * immediately upon collection and whenever the screen size changes.
 */
fun Toolkit.screenSizeFlow(): Flow<Dimension> = callbackFlow {
    val screenBoundaryProperty = "desktopSBC"

    val listener = PropertyChangeListener { event ->
        if (event.propertyName == screenBoundaryProperty) {
            trySend(screenSize)
        }
    }

    // Register the listener
    addPropertyChangeListener(screenBoundaryProperty, listener)

    // Emit the current size immediately after the listener is active
    trySend(screenSize)

    awaitClose {
        removePropertyChangeListener(screenBoundaryProperty, listener)
    }
}.conflate().distinctUntilChanged()

/**
 * Returns a Flow that emits the current screen [Insets] (taskbar/dock space)
 * whenever they change.
 */
fun Toolkit.screenInsetsFlow(
    getGraphicsConfiguration: () -> GraphicsConfiguration
): Flow<Insets> = callbackFlow {
    // This property key covers general desktop environment changes, including insets
    val insetsProperty = "desktopProps"

    val listener = PropertyChangeListener { event ->
        // When desktop properties change, re-calculate insets
        if (event.propertyName == insetsProperty) {
            trySend(getScreenInsets(getGraphicsConfiguration()))
        }
    }

    addPropertyChangeListener(insetsProperty, listener)

    trySend(getScreenInsets(getGraphicsConfiguration()))

    awaitClose {
        removePropertyChangeListener(insetsProperty, listener)
    }
}
