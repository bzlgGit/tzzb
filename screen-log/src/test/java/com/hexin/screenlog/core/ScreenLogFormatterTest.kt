package com.hexin.screenlog.core

import com.hexin.screenlog.api.ScreenLogCopyOptions
import com.hexin.screenlog.api.ScreenLogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLogFormatterTest {

    @Test
    fun responseShowsUrlAndPhaseOnlyBeforeBody() {
        val entry = entry(mapOf("api" to "/orders", "phase" to "RESPONSE"))
        assertEquals("[RESPONSE 响应结果] /orders\nbody", ScreenLogFormatter.formatMessage(entry))
        assertEquals("[RESPONSE 响应结果] /orders", ScreenLogFormatter.requestLabel(entry))
        assertEquals("body", ScreenLogFormatter.formatBody(entry))
    }

    @Test
    fun oldRequestWithoutPhaseUsesRequestLabel() {
        assertEquals(
            "[REQUEST 请求参数] /orders\nbody",
            ScreenLogFormatter.formatMessage(entry(mapOf("api" to "/orders")))
        )
    }

    @Test
    fun explicitRequestShowsParameterLabel() {
        assertEquals(
            "[REQUEST 请求参数] /orders\nbody",
            ScreenLogFormatter.formatMessage(entry(mapOf("api" to "/orders", "phase" to "REQUEST")))
        )
    }

    @Test
    fun ordinaryLogKeepsOriginalMessage() {
        assertEquals("body", ScreenLogFormatter.formatMessage(entry(null)))
    }

    @Test
    fun requestExtrasHideRepeatedApiAndPhaseButKeepOtherFields() {
        val entry = entry(mapOf("api" to "/orders", "phase" to "REQUEST", "traceId" to "123"))
        assertEquals(mapOf("traceId" to "123"), ScreenLogFormatter.visibleExtras(entry))
        assertEquals("[REQUEST 请求参数] /orders\nbody extras={traceId=123}", ScreenLogFormatter.display(entry).substringAfter("][Request] "))
        assertEquals(
            "[REQUEST 请求参数] /orders\nbody extras={traceId=123}",
            ScreenLogFormatter.formatForCopy(entry, ScreenLogCopyOptions(includeExtras = true))
        )
    }

    @Test
    fun jsonBodyStartsOnTheLineAfterRequestHeader() {
        val entry = entry(mapOf("api" to "/orders", "phase" to "RESPONSE"))
            .copy(message = "{\"ok\":true}")
        assertEquals(
            "[RESPONSE 响应结果] /orders\n{\"ok\":true}",
            ScreenLogFormatter.formatMessage(entry)
        )
    }

    private fun entry(extras: Map<String, String>?) = ScreenLogEntry(
        timeMillis = 0,
        level = ScreenLogLevel.DEBUG,
        tag = "Request",
        message = "body",
        extras = extras
    )
}
