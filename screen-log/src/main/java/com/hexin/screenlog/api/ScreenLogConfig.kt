package com.hexin.screenlog.api

import android.app.Application

internal data class ScreenLogConfig(
    val application: Application,
    val enable: Boolean,
    val enablePersist: Boolean = false,
    val keepDays: Int = 1,
    val maxFileSizeMb: Int = 10,
    val minLevel: ScreenLogLevel = ScreenLogLevel.DEBUG,
    val maxMemoryEntries: Int = 1000,
    val copyOptions: ScreenLogCopyOptions = ScreenLogCopyOptions(),
    val hiddenRequestInterfaces: Set<String> = emptySet(),
    val commonTags: List<String> = emptyList()
)
