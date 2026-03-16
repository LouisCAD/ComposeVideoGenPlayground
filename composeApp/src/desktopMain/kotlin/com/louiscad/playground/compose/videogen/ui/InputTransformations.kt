package com.louiscad.playground.compose.videogen.ui

import androidx.compose.foundation.text.input.InputTransformation

object InputTransformations {
    val digitsOnly = InputTransformation {
        if (asCharSequence().any { it.isDigit().not() }) revertAllChanges()
    }

    val decimalsOnly = InputTransformation {
        if (asCharSequence().any { it.isDigit().not() && it != '.' }) revertAllChanges()
    }

    val acceptableFilename = InputTransformation {
        val text = asCharSequence()
        if ('/' in text) revertAllChanges()
        if ('\\' in text) revertAllChanges()
        //TODO: Filter filesystem all/most filesystem problematic characters.
    }
}
