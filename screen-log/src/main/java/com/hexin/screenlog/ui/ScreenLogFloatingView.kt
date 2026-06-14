package com.hexin.screenlog.ui

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode

class ScreenLogFloatingView(context: Context) : FrameLayout(context) {

    private val entryButton = TextView(context)
    private val menuView = ScreenLogMenuView(context)
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0f
    private var startY = 0f
    private var dragging = false

    init {
        elevation = context.dp(12).toFloat()
        layoutParams = LayoutParams(context.dp(52), LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = context.dp(12)
            topMargin = context.dp(120)
        }

        entryButton.text = context.getString(R.string.screen_log_entry)
        entryButton.gravity = Gravity.CENTER
        entryButton.setTextColor(0xFFFFFFFF.toInt())
        entryButton.textSize = 12f
        entryButton.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.screen_log_entry_bg)
        addView(entryButton, LayoutParams(context.dp(52), context.dp(40)))

        menuView.visibility = View.GONE
        addView(menuView, LayoutParams(context.dp(142), LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dp(46)
        })

        entryButton.setOnClickListener {
            if (!dragging) {
                menuView.visibility = if (menuView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                resizeForMenu()
            }
        }

        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun setPanelVisible(visible: Boolean) {
        menuView.setPanelVisible(visible)
    }

    fun setTouchMode(mode: ScreenLogTouchMode) {
        menuView.setTouchMode(mode)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = x
                startY = y
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (kotlin.math.abs(dx) > context.dp(4) || kotlin.math.abs(dy) > context.dp(4)) {
                    dragging = true
                    x = startX + dx
                    y = (startY + dy).coerceAtLeast(0f)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) stickToEdge()
                postDelayed({ dragging = false }, 80)
                return false
            }
        }
        return false
    }

    private fun stickToEdge() {
        val parentWidth = (parent as? View)?.width ?: return
        val targetX = if (x + width / 2f > parentWidth / 2f) parentWidth - entryButton.width - context.dp(8) else context.dp(8)
        animate().x(targetX.toFloat()).setDuration(160).start()
    }

    private fun resizeForMenu() {
        val params = layoutParams
        params.width = if (menuView.visibility == View.VISIBLE) context.dp(142) else context.dp(52)
        params.height = LayoutParams.WRAP_CONTENT
        layoutParams = params
    }
}
