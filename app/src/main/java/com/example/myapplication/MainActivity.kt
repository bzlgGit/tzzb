package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogKit
import com.hexin.screenlog.api.ScreenLogRequestPhase

class MainActivity : Activity() {
    private var testLogIndex = 0
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            text = "请求日志测试：先点击开启，再切换接口开关"
        }
        val buttons = listOf(
            button("Open screen log") {
                ScreenLogKit.open(this)
                requestStatus.text = "日志已开启，可从悬浮入口进入设置"
            },
            button("Test status bar compatibility") {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val content = findViewById<ViewGroup>(android.R.id.content)
                var overlayCount = 0
                for (index in 0 until content.childCount) {
                    val child = content.getChildAt(index)
                    if (child is ViewGroup) {
                        // 模拟业务 App 对 content 所有子 ViewGroup 的统一状态栏适配。
                        child.fitsSystemWindows = true
                        child.clipToPadding = true
                        if (child.javaClass.name.startsWith("com.hexin.screenlog.ui.")) {
                            overlayCount++
                            check(!child.fitsSystemWindows) { "日志浮层接受了宿主的状态栏偏移" }
                        }
                    }
                }
                requestStatus.text = "已模拟状态栏适配，验证日志浮层 $overlayCount 个"
            },
            button("Close screen log") {
                ScreenLogKit.close()
                requestStatus.text = "日志已关闭"
            },
            button("Open second page") {
                startActivity(Intent(this, SecondPageActivity::class.java))
            },
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
            button("Emit FutureFeature log") {
                testLogIndex += 1
                ScreenLog.i("FutureFeature", "late common tag log #$testLogIndex")
                requestStatus.text = "已输出 FutureFeature 日志 #$testLogIndex"
            },
            button("Test large response API") {
                appendRequestTestLog(LARGE_RESPONSE_API, requestStatus)
            },
            button("Test frequent poll API") {
                appendRequestTestLog(FREQUENT_POLL_API, requestStatus)
            },
            button("Test long API wrapping") {
                appendRequestTestLog(LONG_PATH_API, requestStatus)
                appendRequestTestLog(LONG_QUERY_API, requestStatus)
                requestStatus.text = "已写入 2 组长 API 请求日志，请在屏幕日志中查看完整换行"
            },
            button("Test provider exception") {
                appendFailingRequestLog(LARGE_RESPONSE_API, requestStatus)
            },
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
        ScreenLog.request(api = api, phase = ScreenLogRequestPhase.REQUEST, prettyJson = true) {
            """{"request":$testLogIndex}"""
        }
        ScreenLog.request(api = api, phase = ScreenLogRequestPhase.RESPONSE, prettyJson = true) {
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
        const val LONG_PATH_API = "/api/v2/portfolio/positions/history/transactions/reconciliation/details/very-long-resource-identifier-123456789012345678901234567890"
        const val LONG_QUERY_API = "https://example.com/api/v2/accounts/12345678901234567890/holdings/valuation/history?startDate=2026-01-01&endDate=2026-12-31&includePending=true"
    }
}
