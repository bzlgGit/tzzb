package com.hexin.screenlog.api

object ScreenLog {

    fun v(tag: String, message: String, extras: Map<String, String>? = null) {
        ScreenLogManager.write(ScreenLogLevel.VERBOSE, tag, message, null, extras)
    }

    fun d(tag: String, message: String, extras: Map<String, String>? = null) {
        ScreenLogManager.write(ScreenLogLevel.DEBUG, tag, message, null, extras)
    }

    fun i(tag: String, message: String, extras: Map<String, String>? = null) {
        ScreenLogManager.write(ScreenLogLevel.INFO, tag, message, null, extras)
    }

    fun w(tag: String, message: String, extras: Map<String, String>? = null) {
        ScreenLogManager.write(ScreenLogLevel.WARN, tag, message, null, extras)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.ERROR, tag, message, throwable, extras)
    }
}
