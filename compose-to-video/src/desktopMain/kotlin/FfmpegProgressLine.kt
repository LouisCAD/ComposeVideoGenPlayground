package com.louiscad.playground.compose.videogen.core

data class FfmpegProgressLine(
    val frameNumber: Int,
    val fps: Double,
    val quality: Double,
    val sizeKiB: Long,
    val time: Time,
    val bitrateKpbs: Double,
    val speedFactor: Float,
) {

    data class Time(
        val hours: Int,
        val minutes: Int,
        val seconds: Double,
    )

    companion object {
        /**
         * Can parse lines like this example:
         * `frame=  558 fps= 74 q=-0.0 size=   18176KiB time=00:00:09.30 bitrate=16010.5kbits/s speed=1.23x`
         */
        fun parseOrNull(line: String): FfmpegProgressLine? {
            if (line.startsWith("frame=").not()) return null
            val rawValues = line.replace(
                regex = Regex("=\\s*"),
                replacement = "="
            ).splitToSequence(' ').associateTo(mutableMapOf()) { text ->
                text.substringBefore('=') to text.substringAfter('=')
            }
            return FfmpegProgressLine(
                frameNumber = rawValues.remove("frame")!!.toInt(),
                fps = rawValues.remove("fps")!!.toDouble(),
                quality = rawValues.remove("q")!!.toDouble(),
                sizeKiB = rawValues.let {
                    it.remove("size") ?: it.remove("Lsize")
                }!!.replace("KiB", "").toLong(),
                time = rawValues.remove("time")!!.let { timeText ->
                    Time(
                        hours = timeText.substring(0, 2).toInt(),
                        minutes = timeText.substring(3, 5).toInt(),
                        seconds = timeText.substring(6).toDouble()
                    )
                },
                bitrateKpbs = rawValues.remove("bitrate")!!.replace("kbits/s", "").toDouble(),
                speedFactor = rawValues.remove("speed")!!.replace("x", "").toFloat(),
            )
        }
    }
}
