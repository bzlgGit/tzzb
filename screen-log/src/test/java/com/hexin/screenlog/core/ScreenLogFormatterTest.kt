package com.hexin.screenlog.core

import com.hexin.screenlog.api.ScreenLogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLogFormatterTest {

    @Test
    fun responseShowsUrlAndPhaseBeforeAndAfterBody() {
        val entry = entry(mapOf("api" to "/orders", "phase" to "RESPONSE"))
        assertEquals("[RESPONSE 响应结果] /orders\nbody\n[RESPONSE 响应结果] /orders", ScreenLogFormatter.formatMessage(entry))
    }

    @Test
    fun oldRequestWithoutPhaseUsesRequestLabel() {
        assertEquals(
            "[REQUEST 请求参数] /orders\nbody\n[REQUEST 请求参数] /orders",
            ScreenLogFormatter.formatMessage(entry(mapOf("api" to "/orders")))
        )
    }

    @Test
    fun explicitRequestShowsParameterLabel() {
        assertEquals(
            "[REQUEST 请求参数] /orders\nbody\n[REQUEST 请求参数] /orders",
            ScreenLogFormatter.formatMessage(entry(mapOf("api" to "/orders", "phase" to "REQUEST")))
        )
    }

    @Test
    fun ordinaryLogKeepsOriginalMessage() {
        assertEquals("body", ScreenLogFormatter.formatMessage(entry(null)))
    }

    private fun entry(extras: Map<String, String>?) = ScreenLogEntry(
        timeMillis = 0,
        level = ScreenLogLevel.DEBUG,
        tag = "Request",
        message = "body",
        extras = extras
    )
}
