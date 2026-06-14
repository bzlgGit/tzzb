package com.hexin.screenlog.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode
import com.hexin.screenlog.core.ScreenLogDispatcher

class ScreenLogMenuView(context: Context) : LinearLayout(context), ScreenLogDispatcher.Listener {

    private val panelItem = menuItem("Show panel")
    private val scrollItem = menuItem("Pause scroll")
    private val touchModeItem = menuItem("Touch: Log")

    init {
        orientation = VERTICAL
        setBackgroundColor(0xF8FFFFFF.toInt())
        elevation = context.dp(8).toFloat()
        setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(6))

        addView(panelItem)
        addView(scrollItem)
        addView(touchModeItem)
        addView(menuItem("Clear logs").apply {
            setOnClickListener { ScreenLogManager.clearLogs() }
        })
        addView(menuItem("Settings").apply {
            setOnClickListener { ScreenLogManager.openSettings() }
        })
        addView(menuItem("Collapse").apply {
            setOnClickListener { this@ScreenLogMenuView.visibility = View.GONE }
        })

        panelItem.setOnClickListener { ScreenLogManager.togglePanel() }
        scrollItem.setOnClickListener {
            if (ScreenLogDispatcher.isAutoScrollPaused()) {
                ScreenLogManager.resumeAutoScroll()
            } else {
                ScreenLogManager.pauseAutoScroll()
            }
        }
        touchModeItem.setOnClickListener { ScreenLogManager.toggleTouchMode() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScreenLogDispatcher.addListener(this)
        onAutoScrollChanged(ScreenLogDispatcher.isAutoScrollPaused())
    }

    override fun onDetachedFromWindow() {
        ScreenLogDispatcher.removeListener(this)
        super.onDetachedFromWindow()
    }

    fun setPanelVisible(visible: Boolean) {
        panelItem.text = if (visible) "Hide panel" else "Show panel"
    }

    fun setTouchMode(mode: ScreenLogTouchMode) {
        touchModeItem.text = if (mode == ScreenLogTouchMode.LOG_PANEL) {
            "Touch: Log"
        } else {
            "Touch: Page"
        }
    }

    override fun onLogsChanged() = Unit

    override fun onAutoScrollChanged(paused: Boolean) {
        scrollItem.text = if (paused) "Resume scroll" else "Pause scroll"
    }

    private fun menuItem(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(0xFF111827.toInt())
            minHeight = context.dp(34)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(context.dp(8), 0, context.dp(8), 0)
        }
    }
}
