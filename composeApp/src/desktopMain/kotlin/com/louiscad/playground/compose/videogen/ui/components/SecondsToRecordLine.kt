@file:OptIn(ExperimentalFoundationApi::class)

package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.ui.InputTransformations
import composevideogenplayground.composeapp.generated.resources.Res
import composevideogenplayground.composeapp.generated.resources.info_24dp
import org.jetbrains.compose.resources.painterResource

@Composable
fun SecondsToRecordLine(textFieldState: TextFieldState) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    OutlinedTextField(
        state = textFieldState,
        modifier = Modifier.width(150.dp),
        lineLimits = TextFieldLineLimits.SingleLine,
        trailingIcon = {
            TooltipArea(
                tooltip = {
                    Surface(shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Keep blank to use last timecode",
                            Modifier.padding(4.dp)
                        )
                    }
                },
                delayMillis = 0,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd
                )
            ) {
                Icon(painterResource(Res.drawable.info_24dp), contentDescription = "Info")
            }
        },
        inputTransformation = InputTransformations.decimalsOnly,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text("seconds") },
    )
}
