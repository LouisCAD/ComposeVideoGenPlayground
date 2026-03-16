package com.louiscad.playground.compose.videogen.ui

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle

object OutputTransformations {

    @Stable
    @Composable
    fun suffix(suffix: String): OutputTransformation {
        val textColor = LocalTextStyle.current
        return remember(suffix) {
            val color = textColor.color.copy(alpha = .5f)
            OutputTransformation {
                val preTransformLength = length
                append(suffix)
                addStyle(
                    spanStyle = SpanStyle(color = color),
                    start = preTransformLength,
                    end = preTransformLength + suffix.length
                )
            }
        }
    }
}
