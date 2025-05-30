package com.louiscad.playground.compose.videogen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget

class DragNDropTarget(
    private val onDropEvent: (DragAndDropEvent) -> Boolean
) : DragAndDropTarget {
    var isDragging by mutableStateOf(false); private set
    var canDrop by mutableStateOf(false); private set

    override fun onStarted(event: DragAndDropEvent) {
        isDragging = true
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        isDragging = false
        canDrop = false
        return this.onDropEvent(event)
    }

    override fun onEntered(event: DragAndDropEvent) {
        canDrop = true
    }

    override fun onChanged(event: DragAndDropEvent) {
    }

    override fun onMoved(event: DragAndDropEvent) {
    }

    override fun onExited(event: DragAndDropEvent) {
        canDrop = false
    }

    override fun onEnded(event: DragAndDropEvent) {
        isDragging = false
        canDrop = false
    }
}
