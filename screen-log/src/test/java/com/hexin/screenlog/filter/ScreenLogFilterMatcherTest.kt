package com.hexin.screenlog.filter

import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.core.ScreenLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLogFilterMatcherTest {

    private val entries = listOf(
        entry(ScreenLogLevel.INFO, "Login", "success", mapOf("account" to "guest")),
        entry(ScreenLogLevel.ERROR, "Network", "request failed", mapOf("code" to "500")),
        entry(ScreenLogLevel.ERROR, "Login", "token expired")
    )

    @Test
    fun filter_appliesLevelTagAndKeywordTogether() {
        val result = ScreenLogFilterMatcher.filter(
            entries,
            ScreenLogFilter(
                keyword = "TOKEN",
                tag = "Login",
                level = ScreenLogLevel.ERROR
            )
        )

        assertEquals(listOf(entries[2]), result)
    }

    @Test
    fun filter_matchesKeywordInExtrasAndKeepsSourceOrder() {
        val result = ScreenLogFilterMatcher.filter(
            entries,
            ScreenLogFilter(keyword = "account=guest")
        )

        assertEquals(listOf(entries[0]), result)
    }

    @Test
    fun filter_withDefaultFilterReturnsAllEntriesInSourceOrder() {
        assertEquals(entries, ScreenLogFilterMatcher.filter(entries, ScreenLogFilter()))
    }

    private fun entry(
        level: ScreenLogLevel,
        tag: String,
        message: String,
        extras: Map<String, String>? = null
    ) = ScreenLogEntry(
        timeMillis = 1L,
        level = level,
        tag = tag,
        message = message,
        extras = extras
    )
}
