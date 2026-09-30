package com.hexin.screenlog.api

object ScreenLog {

    fun v(
        tag: String,
        message: String,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.VERBOSE, tag, message, null, prettyJson, extras)
    }

    fun d(
        tag: String,
        message: String,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.DEBUG, tag, message, null, prettyJson, extras)
    }

    fun i(
        tag: String,
        message: String,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.INFO, tag, message, null, prettyJson, extras)
    }

    fun w(
        tag: String,
        message: String,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.WARN, tag, message, null, prettyJson, extras)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null
    ) {
        ScreenLogManager.write(ScreenLogLevel.ERROR, tag, message, throwable, prettyJson, extras)
    }
}
