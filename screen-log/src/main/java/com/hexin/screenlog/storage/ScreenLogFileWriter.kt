package com.hexin.screenlog.storage

import com.hexin.screenlog.api.ScreenLogConfig
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
    private var logDir: File? = null
    private var config: ScreenLogConfig? = null

    fun init(logDir: File, config: ScreenLogConfig) {
        this.logDir = logDir
        this.config = config
        if (!logDir.exists()) logDir.mkdirs()
    }

    fun write(entry: ScreenLogEntry) {
        val cfg = config ?: return
        val dir = logDir ?: return
        if (!cfg.enablePersist) return
        executor.execute {
            val file = File(dir, ScreenLogFileNames.today())
            val maxBytes = cfg.maxFileSizeMb.toLong() * 1024L * 1024L
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
