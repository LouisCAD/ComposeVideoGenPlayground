package com.louiscad.playground.compose.videogen.storage

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

val appDataDir: Path = getAppDataDirectory(appName = "com.louiscad.playground.compose.videogen")

fun getAppDataDirectory(
    fs: FileSystem = FileSystem.SYSTEM,
    appName: String,
): Path {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home").toPath()

    val baseDir: Path = when {
        "win" in os -> System.getenv("AppData")?.toPath() ?: (userHome / "AppData" / "Roaming")
        "mac" in os -> userHome / "Library" / "Application Support"
        else -> System.getenv("XDG_DATA_HOME")?.toPath() ?: (userHome / ".local" / "share")
    }

    return (baseDir / appName).also { appPath ->
        if (fs.exists(appPath).not()) fs.createDirectories(appPath)
    }
}
