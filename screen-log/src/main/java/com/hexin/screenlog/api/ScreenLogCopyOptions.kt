package com.hexin.screenlog.api

data class ScreenLogCopyOptions(
    val includeTime: Boolean = false,
    val includeLevel: Boolean = false,
    val includeTag: Boolean = false,
    val includeThreadName: Boolean = false,
    val includeExtras: Boolean = false
)
