package com.louiscad.playground.compose.videogen.lib

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.*
import kotlin.time.Duration.Companion.minutes

private val executionDir = File(".")

class NonZeroExitCodeException(val value: Int) : Exception("Non zero exit value: $value")

fun String.execute(
    workingDir: File = executionDir,
    timeout: Duration = 60.minutes
): String = processBuilder(
    rawCommand = this,
    workingDir = workingDir
).execute(timeout)

fun String.executeAndPrint(
    workingDir: File = executionDir,
    timeout: Duration = 60.minutes
) {
    processBuilder(rawCommand = this, workingDir = workingDir).executeAndPrint(timeout)
}

fun List<String>.execute(
    workingDir: File = executionDir,
    timeout: Duration = 60.minutes
): String = processBuilder(
    command = this,
    workingDir = workingDir
).execute(timeout)

fun List<String>.executeAndPrint(
    workingDir: File = executionDir,
    timeout: Duration = 60.minutes
) {
    processBuilder(command = this, workingDir = workingDir).executeAndPrint(timeout)
}

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

private fun ProcessBuilder.execute(timeout: Duration): String {
    val proc = start()
    proc.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    return proc.inputStream.use { it.bufferedReader().readText() }.also {
        val exitValue = proc.exitValue()
        if (exitValue != 0) {
            throw NonZeroExitCodeException(exitValue)
        }
    }
}

private fun ProcessBuilder.executeAndPrint(timeout: Duration) {
    val proc = redirectInput(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    proc.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    val exitValue = proc.exitValue()
    if (exitValue != 0) {
        throw NonZeroExitCodeException(exitValue)
    }
}

private fun processBuilder(
    command: List<String>,
    workingDir: File = executionDir
): ProcessBuilder = ProcessBuilder(command)
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
