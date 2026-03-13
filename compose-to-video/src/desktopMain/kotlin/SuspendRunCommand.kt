package com.louiscad.playground.compose.videogen.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import splitties.coroutines.raceOf
import java.io.File
import java.util.regex.Pattern
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

private val executionDir = File(".")

// For Kotlin/Native, see the posix version: https://stackoverflow.com/a/64311102/4433326

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

private suspend fun Process.killOrForceKill(waitForForceKill: suspend () -> Unit) {
    val neededToBeDestroyedForcibly = raceOf({
        destroy()
        runInterruptible(Dispatchers.IO) { waitFor() }
        false
    }, {
        waitForForceKill()
        destroyForcibly()
        true
    })
    if (neededToBeDestroyedForcibly) {
        // Wait for the process termination to complete.
        runInterruptible(Dispatchers.IO) { waitFor() }
    }
}

private fun ProcessBuilder.execute(waitForForceKill: suspend () -> Unit): Flow<String> = channelFlow {
    val process by lazy { redirectErrorStream(true).start() }
    // We use a lazy delegate because we want the shutdown hook to be registered before
    // the start call actually takes place.
    // That way, even if the JVM is shutdown at the worst time,
    // we are still able to have access to the process and kill it.
    val shutdownHook = thread(start = false) {
        runBlocking { process.killOrForceKill(waitForForceKill) }
    }
    val runtime = Runtime.getRuntime()
    runtime.addShutdownHook(shutdownHook)
    launch {
        process.inputStream.use { stream ->
            stream.buffered().reader().useLines { lines ->
                lines.forEach { line -> send(line) }
            }
        }
    }
    val exitValue = try {
        runInterruptible { process.waitFor() }
    } catch (e: Throwable) {
        withContext(NonCancellable) {
            process.killOrForceKill(waitForForceKill)
        }
        throw e
    } finally {
        runtime.removeShutdownHook(shutdownHook)
    }
    if (exitValue != 0) throw NonZeroExitCodeException(exitValue)
}.flowOn(Dispatchers.IO).buffer(Channel.UNLIMITED)

private fun processBuilder(
    command: List<String>,
    workingDir: File = executionDir
): ProcessBuilder = ProcessBuilder(command)
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
