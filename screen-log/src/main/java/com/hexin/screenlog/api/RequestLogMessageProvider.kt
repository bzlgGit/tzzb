package com.hexin.screenlog.api

/** 仅在请求日志允许记录时生成日志内容。 */
fun interface RequestLogMessageProvider {
    fun provide(): String
}
