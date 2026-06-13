package com.hexin.screenlog.core

import com.hexin.screenlog.api.ScreenLogLevel

data class ScreenLogEntry(
    val timeMillis: Long,
    val level: ScreenLogLevel,
    val tag: String,
    val message: String,
    val threadName: String? = null,
    val extras: Map<String, String>? = null
)
