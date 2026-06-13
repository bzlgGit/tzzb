package com.hexin.screenlog.storage

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScreenLogCleaner(private val logDir: File) {

    fun clean(keepDays: Int) {
        if (!logDir.exists()) return
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(keepDays.coerceAtLeast(1) - 1))
        val minDate = formatter.parse(formatter.format(calendar.time)) ?: return
        logDir.listFiles()?.forEach { file ->
            val dateText = file.name.removePrefix("screen_log_").removeSuffix(".log")
            val fileDate = runCatching { formatter.parse(dateText) }.getOrNull()
            if (file.isFile && fileDate != null && fileDate.before(minDate)) {
                file.delete()
            }
        }
    }
}
