package com.hexin.screenlog.filter

import com.hexin.screenlog.api.ScreenLogLevel

data class ScreenLogFilter(
    val keyword: String = "",
    val tag: String? = null,
    val level: ScreenLogLevel? = null
)
