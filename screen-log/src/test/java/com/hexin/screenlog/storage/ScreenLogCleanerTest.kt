package com.hexin.screenlog.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScreenLogCleanerTest {

    @Test
    fun clean_keepOneDay_deletesExpiredLogFiles() {
        val logDir = Files.createTempDirectory("screen-log-cleaner-test").toFile()
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val expiredFile = logDir.resolve("screen_log_${formatter.format(yesterday.time)}.log")
                .apply { writeText("expired") }
            val currentFile = logDir.resolve("screen_log_${formatter.format(Calendar.getInstance().time)}.log")
                .apply { writeText("current") }

            ScreenLogCleaner(logDir).clean(keepDays = 1)

            assertFalse(expiredFile.exists())
            assertTrue(currentFile.exists())
        } finally {
            logDir.deleteRecursively()
        }
    }
}
