package com.hexin.screenlog.storage

import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.core.ScreenLogFormatter
import java.io.File

class ScreenLogFileReader(private val logDir: File) {

    fun readRecent(keepDays: Int): List<ScreenLogEntry> {
        if (!logDir.exists()) return emptyList()
        return logDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("screen_log_") }
            ?.sortedBy { it.name }
            ?.takeLast(keepDays.coerceAtLeast(1))
            ?.flatMap { file -> file.useLines { lines -> lines.mapNotNull(ScreenLogFormatter::deserialize).toList() } }
            .orEmpty()
    }
}
