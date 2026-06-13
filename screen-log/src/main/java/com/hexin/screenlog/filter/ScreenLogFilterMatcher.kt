package com.hexin.screenlog.filter

import com.hexin.screenlog.core.ScreenLogEntry

object ScreenLogFilterMatcher {

    fun match(entry: ScreenLogEntry, filter: ScreenLogFilter): Boolean {
        if (filter.level != null && entry.level != filter.level) return false
        if (!filter.tag.isNullOrBlank() && entry.tag != filter.tag) return false
        val keyword = filter.keyword.trim()
        if (keyword.isBlank()) return true
        val haystack = buildString {
            append(entry.tag)
            append('\n')
            append(entry.message)
            append('\n')
            entry.extras.orEmpty().forEach { (key, value) ->
                append(key).append('=').append(value).append('\n')
            }
        }
        return haystack.contains(keyword, ignoreCase = true)
    }
}
