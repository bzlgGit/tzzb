package com.hexin.screenlog.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ScreenLogFileNames {
    private val dayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun today(): String = "screen_log_${dayFormat.get()?.format(Date()).orEmpty()}.log"
}
