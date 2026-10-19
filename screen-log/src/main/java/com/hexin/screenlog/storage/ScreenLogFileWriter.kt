package com.hexin.screenlog.storage

import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.core.ScreenLogFormatter
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object ScreenLogFileWriter {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val limitWarned = AtomicBoolean(false)
    @Volatile
    private var enabled = false
    @Volatile
    private var logDir: File? = null
    @Volatile
    private var maxFileSizeMb = 10

    fun configure(logDir: File, maxFileSizeMb: Int, enabled: Boolean) {
        this.logDir = logDir
        this.maxFileSizeMb = maxFileSizeMb
        this.enabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun write(entry: ScreenLogEntry) {
        if (!enabled) return
        val dir = logDir ?: return
        executor.execute {
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, ScreenLogFileNames.today())
            val maxBytes = maxFileSizeMb.toLong() * 1024L * 1024L
            if (file.exists() && file.length() >= maxBytes) {
                appendLimitWarning(file, maxBytes)
                return@execute
            }
            file.appendText(ScreenLogFormatter.serialize(entry) + "\n")
        }
    }

    fun clearAll() {
        val dir = logDir ?: return
        executor.execute {
            if (!dir.exists()) return@execute
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("screen_log_")) file.delete()
            }
            limitWarned.set(false)
        }
    }

    private fun appendLimitWarning(file: File, maxBytes: Long) {
        if (!limitWarned.compareAndSet(false, true)) return
        val warning = ScreenLogEntry(
            timeMillis = System.currentTimeMillis(),
            level = ScreenLogLevel.WARN,
            tag = "ScreenLog",
            message = "current log file reached ${maxBytes / 1024 / 1024}MB limit, new logs ignored today"
        )
        if (!file.exists() || file.length() < maxBytes) {
            file.appendText(ScreenLogFormatter.serialize(warning) + "\n")
        }
    }
}
