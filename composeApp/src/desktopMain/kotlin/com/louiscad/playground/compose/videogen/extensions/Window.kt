package com.louiscad.playground.compose.videogen.extensions

import java.awt.Insets
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Detects inset changes by observing when the [Window] is affected
 * by screen/hierarchy bounds changes (e.g., Dock show/hide).
 */
fun Window.screenInsetsFlow(): Flow<Insets> = callbackFlow {
    val toolkit = Toolkit.getDefaultToolkit()

    // Helper to get current insets for the window's current screen
    fun getCurrentInsets(): Insets = toolkit.getScreenInsets(graphicsConfiguration)

    val listener = object : HierarchyBoundsAdapter() {
        override fun ancestorMoved(e: HierarchyEvent) {
            trySend(getCurrentInsets())
        }

        override fun ancestorResized(e: HierarchyEvent) {
            trySend(getCurrentInsets())
        }
    }

    // This listener catches changes to the desktop "work area"
    // that affect the window's hierarchy
    addHierarchyBoundsListener(listener)

    // Initial emission
    trySend(getCurrentInsets())

    awaitClose {
        removeHierarchyBoundsListener(listener)
    }
}
