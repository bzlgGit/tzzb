package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogConfig
import com.hexin.screenlog.api.ScreenLogManager

class MainActivity : Activity() {
    private var testLogIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenLogManager.init(
            ScreenLogConfig(
                application = application,
                enable = true
            )
        )
        ScreenLogManager.showFloatingEntry()
        ScreenLog.d("MainActivity", "screen log module initialized")
        ScreenLog.i("WebView", "load url = https://example.com", mapOf("url" to "https://example.com"))
        ScreenLog.e("AccountDelete", "delete account failed, code = 500", extras = mapOf("code" to "500"))
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
            button("Pause auto scroll") { ScreenLogManager.pauseAutoScroll() },
            button("Resume auto scroll") { ScreenLogManager.resumeAutoScroll() },
            button("Clear logs") { ScreenLogManager.clearLogs() }
        )

        root.addView(title)
        buttons.forEach(root::addView)

        ConstraintSet().apply {
            clone(root)
            constrainWidth(title.id, ConstraintSet.WRAP_CONTENT)
            constrainHeight(title.id, ConstraintSet.WRAP_CONTENT)
            connect(title.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(title.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            buttons.forEachIndexed { index, button ->
                constrainWidth(button.id, dp(220))
                constrainHeight(button.id, dp(44))
                connect(button.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(
                    button.id,
                    ConstraintSet.TOP,
                    if (index == 0) title.id else buttons[index - 1].id,
                    if (index == 0) ConstraintSet.BOTTOM else ConstraintSet.BOTTOM,
                    if (index == 0) dp(18) else dp(12)
                )
            }
            applyTo(root)
        }
        return root
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
            0 -> ScreenLog.d(tag, "debug test log #$index", mapOf("index" to "$index", "case" to "debug"))
            1 -> ScreenLog.i(tag, "info test log #$index", mapOf("index" to "$index", "keyword" to "screen-log"))
            2 -> ScreenLog.w(tag, "warn test log #$index", mapOf("index" to "$index", "retry" to "${index % 3}"))
            else -> ScreenLog.e(tag, "error test log #$index", extras = mapOf("index" to "$index", "code" to "E$index"))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
