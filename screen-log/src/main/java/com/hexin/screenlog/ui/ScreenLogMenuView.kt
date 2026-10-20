package com.hexin.screenlog.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogKit
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode
import com.hexin.screenlog.core.ScreenLogDispatcher

internal class ScreenLogMenuView(context: Context) : LinearLayout(context), ScreenLogDispatcher.Listener {

    var onCollapse: (() -> Unit)? = null

    private val panelSwitch = menuSwitch(R.string.screen_log_panel_visible)
    private val recordingSwitch = menuSwitch(R.string.screen_log_recording)
    private val autoScrollSwitch = menuSwitch(R.string.screen_log_auto_scroll)
    private val touchLogPanel = segmentItem(R.string.screen_log_touch_log_panel)
    private val touchUnderlyingPage = segmentItem(R.string.screen_log_touch_underlying_page)
    private var suppressSwitchCallbacks = false

    init {
        orientation = VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.screen_log_menu_bg)
        elevation = context.dp(8).toFloat()
        setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(6))

        addView(panelSwitch)
        addView(recordingSwitch)
        addView(autoScrollSwitch)
        addView(menuLabel(context.getString(R.string.screen_log_touch_target)))
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(touchLogPanel, LayoutParams(0, context.dp(40), 1f).apply {
                marginEnd = context.dp(3)
            })
            addView(touchUnderlyingPage, LayoutParams(0, context.dp(40), 1f).apply {
                marginStart = context.dp(3)
            })
        })
        addView(menuItem(R.string.screen_log_clear_logs, R.drawable.screen_log_ic_clear).apply {
            setOnClickListener { confirmClearLogs() }
        })
        addView(menuItem(R.string.screen_log_settings, R.drawable.screen_log_ic_settings).apply {
            setOnClickListener { ScreenLogManager.openSettings() }
        })
        addView(menuItem(R.string.screen_log_close, R.drawable.screen_log_ic_close).apply {
            setOnClickListener { ScreenLogKit.close() }
        })
        addView(menuItem(R.string.screen_log_collapse, R.drawable.screen_log_ic_expand).apply {
            setOnClickListener { onCollapse?.invoke() }
        })

        panelSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            if (checked) ScreenLogManager.showPanel() else ScreenLogManager.hidePanel()
        }
        recordingSwitch.setOnCheckedChangeListener { _, checked ->
            if (!suppressSwitchCallbacks) ScreenLogManager.setRecordingEnabled(checked)
        }
        autoScrollSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            if (checked) ScreenLogManager.resumeAutoScroll() else ScreenLogManager.pauseAutoScroll()
        }
        touchLogPanel.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.LOG_PANEL)
        }
        touchUnderlyingPage.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.UNDERLYING_PAGE)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScreenLogDispatcher.addListener(this)
        onAutoScrollChanged(ScreenLogDispatcher.isAutoScrollPaused())
        onRecordingChanged(ScreenLogManager.isRecordingEnabled())
    }

    override fun onDetachedFromWindow() {
        ScreenLogDispatcher.removeListener(this)
        super.onDetachedFromWindow()
    }

    fun setPanelVisible(visible: Boolean) {
        updateSwitch(panelSwitch, visible)
    }

    fun setTouchMode(mode: ScreenLogTouchMode) {
        updateSegment(touchLogPanel, mode == ScreenLogTouchMode.LOG_PANEL)
        updateSegment(touchUnderlyingPage, mode == ScreenLogTouchMode.UNDERLYING_PAGE)
    }

    override fun onLogsChanged() = Unit

    override fun onRecordingChanged(enabled: Boolean) {
        updateSwitch(recordingSwitch, enabled)
    }

    override fun onAutoScrollChanged(paused: Boolean) {
        updateSwitch(autoScrollSwitch, !paused)
    }

    private fun updateSwitch(view: Switch, checked: Boolean) {
        if (view.isChecked == checked) return
        suppressSwitchCallbacks = true
        view.isChecked = checked
        suppressSwitchCallbacks = false
    }

    private fun updateSegment(view: TextView, selected: Boolean) {
        view.isSelected = selected
        view.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) android.R.color.white else R.color.screen_log_accent
            )
        )
        view.background = ContextCompat.getDrawable(
            context,
            if (selected) R.drawable.screen_log_segment_selected_bg else R.drawable.screen_log_segment_unselected_bg
        )
    }

    private fun menuSwitch(textRes: Int): Switch {
        return Switch(context).apply {
            text = context.getString(textRes)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.screen_log_text_primary))
            minHeight = context.dp(48)
            gravity = Gravity.CENTER_VERTICAL
            switchMinWidth = context.dp(40)
            switchPadding = context.dp(8)
            thumbTintList = ContextCompat.getColorStateList(context, R.color.screen_log_switch_thumb)
            trackTintList = ContextCompat.getColorStateList(context, R.color.screen_log_switch_track)
            setPadding(context.dp(8), 0, context.dp(4), 0)
        }
    }

    private fun menuLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.screen_log_text_secondary))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(8), context.dp(4), context.dp(8), context.dp(2))
        }
    }

    private fun segmentItem(textRes: Int): TextView {
        return TextView(context).apply {
            text = context.getString(textRes)
            textSize = 12f
            gravity = Gravity.CENTER
        }
    }

    private fun menuItem(textRes: Int, iconRes: Int): TextView {
        return TextView(context).apply {
            text = context.getString(textRes)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.screen_log_text_primary))
            minHeight = context.dp(48)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(8), 0, context.dp(8), 0)
            setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
            compoundDrawablePadding = context.dp(12)
        }
    }

    private fun confirmClearLogs() {
        AlertDialog.Builder(context)
            .setTitle(R.string.screen_log_clear_memory_title)
            .setMessage(R.string.screen_log_clear_memory_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.screen_log_clear) { _, _ -> ScreenLogManager.clearMemoryLogs() }
            .show()
    }
}
