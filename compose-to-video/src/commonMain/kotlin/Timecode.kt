package com.louiscad.playground.compose.videogen.core

data class Timecode(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frame: Int
) : Comparable<Timecode> {

    init {
        require(minutes in 0..59) { "minutes must be between 0 and 59: $minutes" }
        require(seconds in 0..59) { "seconds must be between 0 and 59: $seconds" }
    }

    fun humanReadable(): String {
        val hh = hours.toDoubleDigits()
        val mm = minutes.toDoubleDigits()
        val ss = seconds.toDoubleDigits()
        val ff = frame.toDoubleDigits()
        return "$hh:$mm:$ss:$ff"
    }

    override fun compareTo(other: Timecode): Int {
        if (hours != other.hours) return hours.compareTo(other.hours)
        if (minutes != other.minutes) return minutes.compareTo(other.minutes)
        if (seconds != other.seconds) return seconds.compareTo(other.seconds)
        return frame.compareTo(other.frame)
    }
}

private fun Int.toDoubleDigits(): String = toString().padStart(length = 2, padChar = '0')
