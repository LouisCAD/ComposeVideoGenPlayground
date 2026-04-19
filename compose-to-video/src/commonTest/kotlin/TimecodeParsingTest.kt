package com.louiscad.playground.compose.videogen.core

import kotlin.test.Test

class TimecodeParsingTest {

    @Test
    fun testTimecodeParsing() {
        parseTimeCodeOrNull("01:13:49:06") shouldBe Timecode(1, 13, 49, 6)
    }
}
