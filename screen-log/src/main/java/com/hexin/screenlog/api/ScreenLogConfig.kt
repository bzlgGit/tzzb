package com.hexin.screenlog.api

import android.app.Application

data class ScreenLogConfig(
    val application: Application,
    val enable: Boolean,
    val enablePersist: Boolean = true,
    val keepDays: Int = 3,
    val maxFileSizeMb: Int = 10,
    val minLevel: ScreenLogLevel = ScreenLogLevel.DEBUG,
    val maxMemoryEntries: Int = 2000
)
