package com.hexin.screenlog.api

object ScreenLog {

    /** 仅在请求日志允许记录时调用 provider。 */
    fun request(
        api: String,
        level: ScreenLogLevel = ScreenLogLevel.DEBUG,
        prettyJson: Boolean = false,
        extras: Map<String, String>? = null,
        messageProvider: RequestLogMessageProvider
    ) {
        require(api.isNotBlank()) { "api must not be blank" }
        if (!ScreenLogManager.canRecordRequest(api, level)) return
        val requestExtras = extras.orEmpty() + ("api" to api.trim())
        val message = try {
            messageProvider.provide()
        } catch (error: Exception) {
            ScreenLogManager.write(
                ScreenLogLevel.ERROR,
                "Request",
                "请求日志内容生成失败：${error.message ?: error.javaClass.simpleName}",
                error,
                false,
                requestExtras
            )
            return
        }
        ScreenLogManager.write(level, "Request", message, null, prettyJson, requestExtras)
    }

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
