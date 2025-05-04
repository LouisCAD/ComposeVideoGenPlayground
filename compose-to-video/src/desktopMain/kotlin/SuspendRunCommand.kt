package com.louiscad.playground.compose.videogen.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import splitties.coroutines.raceOf
import java.io.File
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds

private val executionDir = File(".")

// For Kotlin/Native, see posix version: https://stackoverflow.com/a/64311102/4433326

fun String.commandExecutionLines(
    workingDir: File = executionDir,
    waitForForceKill: suspend () -> Unit = { delay(10.seconds) }
): Flow<String> {
    return processBuilder(rawCommand = this, workingDir = workingDir).execute(waitForForceKill)
}

fun List<String>.commandExecutionLines(
    workingDir: File = executionDir,
    waitForForceKill: suspend () -> Unit = { delay(10.seconds) }
): Flow<String> {
    return processBuilder(command = this, workingDir = workingDir).execute(waitForForceKill)
}

//TODO: Maybe replace with code based on this: https://github.com/leonschreuder/kotlin-exec/blob/da9389e0e99fbda6acb495aee91a0131322537cf/src/main/kotlin/io/github/leonschreuder/kotlinexec/WordSplitter.kt
private val rawCommandPattern = Pattern.compile("\"([^\"]*)\"|(\\S+)")

private fun decodeRawCommand(rawCommand: String): List<String> {
    return rawCommandPattern.matcher(rawCommand).let { m ->
        generateSequence {
            when {
                m.find() -> if (m.group(1) != null) m.group(1) else m.group(2)
                else -> null
            }
        }
    }.toList()
}

private fun processBuilder(
    rawCommand: String,
    workingDir: File = executionDir
): ProcessBuilder = processBuilder(
    command = decodeRawCommand(rawCommand),
    workingDir = workingDir
)

private fun ProcessBuilder.execute(waitForForceKill: suspend () -> Unit): Flow<String> = channelFlow {
    val process = redirectErrorStream(true).start()
    try {
        process.inputStream.use { stream ->
            stream.buffered().reader().useLines { lines ->
                lines.forEach { line -> send(line) }
            }
        }
    } catch (e: Throwable) {
        withContext(NonCancellable) {
            raceOf({
                process.destroy()
                process.onExit().await()
            }, {
                waitForForceKill()
                process.destroyForcibly()
            })
        }
        throw e
    }
    val exitValue = runInterruptible { process.waitFor() }
    if (exitValue != 0) throw NonZeroExitCodeException(exitValue)
}.flowOn(Dispatchers.IO).buffer(Channel.UNLIMITED)

private fun processBuilder(
    command: List<String>,
    workingDir: File = executionDir
): ProcessBuilder = ProcessBuilder(command)
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
