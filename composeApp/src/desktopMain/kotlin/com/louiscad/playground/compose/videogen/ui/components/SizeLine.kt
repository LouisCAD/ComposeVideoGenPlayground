package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.ui.InputTransformations

@Composable
internal fun SizeLine(
    widthFieldState: TextFieldState,
    heightFieldState: TextFieldState,
    densityFieldState: TextFieldState
) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    val modifier = Modifier.width(90.dp)
    OutlinedTextField(
        state = widthFieldState,
        modifier = modifier,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformations.digitsOnly,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text("width") }
    )
    OutlinedTextField(
        state = heightFieldState,
        modifier = modifier,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformations.digitsOnly,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text("height") }
    )
    OutlinedTextField(
        state = densityFieldState,
        modifier = modifier,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformations.decimalsOnly,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text("density") }
    )
    //TODO: Allow changing fps
}
