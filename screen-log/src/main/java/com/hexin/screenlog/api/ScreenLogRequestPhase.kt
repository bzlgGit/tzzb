package com.hexin.screenlog.api

/** 请求日志所属阶段，供 Kotlin 和 Java 调用方显式区分。 */
enum class ScreenLogRequestPhase {
    REQUEST,
    RESPONSE
}
