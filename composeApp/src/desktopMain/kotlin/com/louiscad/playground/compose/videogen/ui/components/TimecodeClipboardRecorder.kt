package com.louiscad.playground.compose.videogen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.unit.dp
import com.louiscad.playground.compose.videogen.core.Timecode
import com.louiscad.playground.compose.videogen.core.extensions.coroutines.takeUntil
import com.louiscad.playground.compose.videogen.core.parseTimeCodeOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.isActive
import splitties.coroutines.raceOf
import splitties.coroutines.rememberCallableState
import splitties.coroutines.repeatWhileActive
import java.awt.datatransfer.DataFlavor
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimecodeClipboardRecorder(
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val start = rememberCallableState<Unit>()
    val stop = rememberCallableState<Unit>()
    val recordedTimecodes: List<Timecode> by produceState(emptyList()) {
        repeatWhileActive {
            start.awaitOneCall()
            value = emptyList()
            timeCodesFromClipboard(clipboard).takeUntil {
                stop.awaitOneCall()
            }.onEach { timecode ->

            }.runningFold(emptyList<Timecode>()) { all, newTimeCode ->
                all + newTimeCode
            }.collect { timecodes ->
                value = timecodes
            }
        }
    }
    Column(modifier = modifier) {
        Row {
            Button(onClick = start, enabled = start.isAwaitingCall) { Text(text = "Start") }
            Button(onClick = stop, enabled = stop.isAwaitingCall) { Text(text = "Stop") }
        }
        LazyColumn {
            items(recordedTimecodes.sorted(), key = { it }) { timeCode ->
                Text(
                    text = timeCode.humanReadable(),
                    modifier = Modifier.animateItem().padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }
        }
    }
}

fun timeCodesFromClipboard(
    clipboard: Clipboard,
    waitForNextPolling: suspend () -> Unit = { delay(.1.seconds) },
): Flow<Timecode> = flow {
    var lastTimecode: Timecode? = null
    while (currentCoroutineContext().isActive) {
        val entry = clipboard.getClipEntry()
        waitForNextPolling()
        val timecode = entry?.extractTimecodeOrNull()?.takeUnless { it == lastTimecode } ?: continue
        lastTimecode = timecode
        emit(timecode)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ClipEntry.extractTimecodeOrNull(): Timecode? {
    val transferable = asAwtTransferable?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) } ?: return null
    val text = transferable.getTransferData(DataFlavor.stringFlavor) as? String ?: return null
    return parseTimeCodeOrNull(text)
}
