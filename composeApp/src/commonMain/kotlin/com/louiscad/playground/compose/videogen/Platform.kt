package com.louiscad.playground.compose.videogen

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform