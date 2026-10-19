package com.hexin.screenlog.storage

import android.content.ContentResolver
import android.net.Uri
import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.core.ScreenLogFormatter
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class ScreenLogExporter(private val logDir: File) {

    fun export(
        contentResolver: ContentResolver,
        uri: Uri,
        memoryEntries: List<ScreenLogEntry>
    ): Result<Unit> = runCatching {
        val outputStream = contentResolver.openOutputStream(uri)
            ?: error("无法创建导出文件")
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            logDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("screen_log_") }
                ?.sortedBy { it.name }
                ?.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

            zip.putNextEntry(ZipEntry("screen_log_current_memory.log"))
            memoryEntries.forEach { entry ->
                zip.write(ScreenLogFormatter.serialize(entry).toByteArray(Charsets.UTF_8))
                zip.write('\n'.code)
            }
            zip.closeEntry()
        }
    }
}
