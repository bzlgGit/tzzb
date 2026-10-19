package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogConfig
import com.hexin.screenlog.api.ScreenLogManager

class MainActivity : Activity() {
    private var testLogIndex = 0
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenLogManager.init(
            ScreenLogConfig(
                application = application,
                enable = true,
                hiddenRequestInterfaces = setOf(LARGE_RESPONSE_API, FREQUENT_POLL_API)
            )
        )
        ScreenLogManager.showFloatingEntry()
        ScreenLog.d("MainActivity", "screen log module initialized")
        ScreenLog.i(
            "WebView",
            "load url = https://example.com",
            extras = mapOf("url" to "https://example.com")
        )
        ScreenLog.e("AccountDelete", "delete account failed, code = 500", extras = mapOf("code" to "500"))
        ScreenLog.i(
            tag = "Json",
            message = """{"code":0,"data":{"name":"screen-log"}}""",
            prettyJson = true
        )
        setContentView(createTestEntryView())
    }

    override fun onResume() {
        super.onResume()
        ScreenLogManager.attachTo(this)
        ScreenLogManager.showFloatingEntry()
    }

    private fun createTestEntryView(): View {
        val root = ConstraintLayout(this).apply {
            id = View.generateViewId()
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        val title = TextView(this).apply {
            id = View.generateViewId()
            text = "Screen Log Test"
            textSize = 20f
        }
        val requestStatus = TextView(this).apply {
            id = View.generateViewId()
            text = "请求日志测试：先在设置中开启“记录日志”"
        }
        val buttons = listOf(
            button("Show floating entry") { ScreenLogManager.showFloatingEntry() },
            button("Hide floating entry") { ScreenLogManager.hideFloatingEntry() },
            button("Show panel") { ScreenLogManager.showPanel() },
            button("Add one test log") {
                testLogIndex += 1
                appendTestLog(testLogIndex)
            },
            button("Add 50 test logs") {
                repeat(50) {
                    testLogIndex += 1
                    appendTestLog(testLogIndex)
                }
            },
            button("Test large response API") {
                appendRequestTestLog(LARGE_RESPONSE_API, requestStatus)
            },
            button("Test frequent poll API") {
                appendRequestTestLog(FREQUENT_POLL_API, requestStatus)
            },
            button("Test provider exception") {
                appendFailingRequestLog(LARGE_RESPONSE_API, requestStatus)
            },
            button("Pause auto scroll") { ScreenLogManager.pauseAutoScroll() },
            button("Resume auto scroll") { ScreenLogManager.resumeAutoScroll() },
            button("Clear logs") { ScreenLogManager.clearLogs() }
        )

        root.addView(title)
        root.addView(requestStatus)
        buttons.forEach(root::addView)

        ConstraintSet().apply {
            clone(root)
            constrainWidth(title.id, ConstraintSet.WRAP_CONTENT)
            constrainHeight(title.id, ConstraintSet.WRAP_CONTENT)
            connect(title.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(title.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            constrainWidth(requestStatus.id, ConstraintSet.MATCH_CONSTRAINT)
            constrainHeight(requestStatus.id, ConstraintSet.WRAP_CONTENT)
            connect(requestStatus.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(requestStatus.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            connect(requestStatus.id, ConstraintSet.TOP, title.id, ConstraintSet.BOTTOM, dp(8))

            buttons.forEachIndexed { index, button ->
                constrainWidth(button.id, dp(220))
                constrainHeight(button.id, dp(44))
                connect(button.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(
                    button.id,
                    ConstraintSet.TOP,
                    if (index == 0) requestStatus.id else buttons[index - 1].id,
                    ConstraintSet.BOTTOM,
                    if (index == 0) dp(18) else dp(12)
                )
            }
            applyTo(root)
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            id = View.generateViewId()
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    private fun appendTestLog(index: Int) {
        val tag = when (index % 5) {
            0 -> "WebView"
            1 -> "JSBridge"
            2 -> "Network"
            3 -> "Login"
            else -> "Page"
        }
        when (index % 4) {
            0 -> ScreenLog.d(
                tag,
                "debug test log #$index",
                extras = mapOf("index" to "$index", "case" to "debug")
            )
            1 -> ScreenLog.i(
                tag,
                "info test log #$index",
                extras = mapOf("index" to "$index", "keyword" to "screen-log")
            )
            2 -> ScreenLog.w(
                tag,
                "warn test log #$index",
                extras = mapOf("index" to "$index", "retry" to "${index % 3}")
            )
            else -> ScreenLog.e(tag, "error test log #$index", extras = mapOf("index" to "$index", "code" to "E$index"))
        }
    }

    private fun appendRequestTestLog(api: String, status: TextView) {
        testLogIndex += 1
        val responseBody = """{"request":$testLogIndex,"api":"$api","items":[1,2,3]}"""
        var providerCalled = false
        ScreenLog.request(api = api, prettyJson = true) {
            providerCalled = true
            gson.fromJson(responseBody, JsonObject::class.java).toString()
        }
        status.text = if (providerCalled) {
            "$api：已记录请求日志 #$testLogIndex"
        } else {
            "$api：未调用 provide，已跳过解析和日志"
        }
    }

    private fun appendFailingRequestLog(api: String, status: TextView) {
        var providerCalled = false
        ScreenLog.request(api = api) {
            providerCalled = true
            throw IllegalStateException("模拟请求日志解析失败")
        }
        status.text = if (providerCalled) {
            "$api：provide 已抛异常，请查看 Request 错误日志"
        } else {
            "$api：未调用 provide，已跳过异常测试"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val LARGE_RESPONSE_API = "/mock/large-response"
        const val FREQUENT_POLL_API = "/mock/frequent-poll"
    }
}
