package com.hexin.screenlog.core

import com.hexin.screenlog.api.ScreenLogCopyOptions
import com.hexin.screenlog.api.ScreenLogLevel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenLogFormatter {

    private const val JSON_INDENT_SPACES = 2

    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }

    fun display(entry: ScreenLogEntry): String {
        return buildString {
            append("[")
            append(formatTime(entry.timeMillis))
            append("][")
            append(entry.level.name)
            append("][")
            append(entry.tag)
            append("] ")
            append(formatMessage(entry))
            if (!entry.extras.isNullOrEmpty()) {
                append(" extras=")
                append(entry.extras)
            }
        }
    }

    internal fun formatForCopy(entry: ScreenLogEntry, options: ScreenLogCopyOptions): String {
        return buildString {
            if (options.includeTime) appendField(formatTime(entry.timeMillis))
            if (options.includeLevel) appendField(entry.level.name)
            if (options.includeTag) appendField(entry.tag)
            if (options.includeThreadName && !entry.threadName.isNullOrBlank()) {
                appendField(entry.threadName)
            }
            if (isNotEmpty()) append(' ')
            append(formatMessage(entry))
            if (options.includeExtras && !entry.extras.isNullOrEmpty()) {
                append(" extras=")
                append(entry.extras)
            }
        }
    }

    fun formatMessage(entry: ScreenLogEntry): String {
        val message = if (entry.prettyJson) prettyPrintJson(entry.message) else entry.message
        return buildString {
            append(message)
            if (!entry.throwableStackTrace.isNullOrBlank()) {
                append('\n')
                append(entry.throwableStackTrace)
            }
        }
    }

    fun formatTime(timeMillis: Long): String {
        return timeFormat.get()?.format(Date(timeMillis)).orEmpty()
    }

    fun serialize(entry: ScreenLogEntry): String {
        val json = JSONObject()
        json.put("timeMillis", entry.timeMillis)
        json.put("level", entry.level.name)
        json.put("tag", entry.tag)
        json.put("message", entry.message)
        json.put("threadName", entry.threadName)
        json.put("prettyJson", entry.prettyJson)
        json.put("throwableStackTrace", entry.throwableStackTrace)
        val extras = JSONObject()
        entry.extras.orEmpty().forEach { (key, value) -> extras.put(key, value) }
        json.put("extras", extras)
        return json.toString()
    }

    fun deserialize(line: String): ScreenLogEntry? {
        return runCatching {
            val json = JSONObject(line)
            val extrasJson = json.optJSONObject("extras")
            val extras = extrasJson?.keys()?.asSequence()?.associateWith { extrasJson.optString(it) }
                ?.takeIf { it.isNotEmpty() }
            ScreenLogEntry(
                timeMillis = json.optLong("timeMillis"),
                level = ScreenLogLevel.valueOf(json.optString("level")),
                tag = json.optString("tag"),
                message = json.optString("message"),
                threadName = json.optString("threadName").takeIf { it.isNotBlank() && it != "null" },
                prettyJson = json.optBoolean("prettyJson", false),
                throwableStackTrace = json.optString("throwableStackTrace")
                    .takeIf { it.isNotBlank() && it != "null" },
                extras = extras
            )
        }.getOrNull()
    }

    private fun prettyPrintJson(message: String): String {
        val trimmed = message.trim()
        return runCatching {
            when (trimmed.firstOrNull()) {
                '{' -> JSONObject(trimmed).toString(JSON_INDENT_SPACES)
                '[' -> JSONArray(trimmed).toString(JSON_INDENT_SPACES)
                else -> message
            }
        }.getOrDefault(message)
    }

    private fun StringBuilder.appendField(value: String) {
        append('[').append(value).append(']')
    }
}
