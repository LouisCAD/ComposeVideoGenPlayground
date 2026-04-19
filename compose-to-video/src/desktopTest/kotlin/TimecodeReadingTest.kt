package com.louiscad.playground.compose.videogen.core

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test

class TimecodeReadingTest {

    private val resources: FileSystem = FileSystem.RESOURCES
    private val timecodesDir = "timecodes".toPath()

    @Test
    fun `Test per line reading of basic timecodes`() {
        val timecodes = readTimecodes(timecodesDir.resolve("overtakes_cars_only.timecodes.txt"), resources)
        timecodes.size shouldBe 23
    }

    @Test
    fun `Test relative timecodes parsing`() {
        val lines = """
            +2:59:59:24
            +5
            +1 // 3 hours
            +29
            +3
            4:00:00:00 // Absolute timecode
            +1
            +01:59:00
            +29
            +58:00:00
        """.trimIndent().lineSequence()
        readTimecodes(lines, 30) shouldBe listOf(
            Timecode(2, 59, 59, 24),
            Timecode(2, 59, 59, 29),
            Timecode(3, 0, 0, 0),
            Timecode(3, 0, 0, 29),
            Timecode(3, 0, 1, 2),
            Timecode(4, 0, 0, 0),
            Timecode(4, 0, 0, 1),
            Timecode(4, 1, 59, 1),
            Timecode(4, 2, 0, 0),
            Timecode(5, 0, 0, 0),
        )
    }

    @Test
    fun `Test timecode plus`() {
        val fps = 30
        val zero = Timecode(0, 0, 0, 0)
        val almost3Hours = zero.plus(hours = 2, minutes = 59, seconds = 59, frame = 29, fps = fps)
        almost3Hours shouldBe Timecode(hours = 2, minutes = 59, seconds = 59, frame = 29)
        almost3Hours.plus(frame = 1, fps = fps) shouldBe Timecode(hours = 3, minutes = 0, seconds = 0, frame = 0)
    }
}
