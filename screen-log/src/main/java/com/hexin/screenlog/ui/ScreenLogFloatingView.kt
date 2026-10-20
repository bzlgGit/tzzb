package com.hexin.screenlog.ui

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode

internal class ScreenLogFloatingView(
    context: Context
) : FrameLayout(context) {

    override fun setFitsSystemWindows(fitSystemWindows: Boolean) {
        // 与日志面板保持同一坐标系，避免宿主统一设置状态栏适配时入口发生偏移。
        super.setFitsSystemWindows(false)
    }

    private val entryButton = TextView(context)
    private val menuView = ScreenLogMenuView(context)
    private val menuScroll = ScrollView(context)
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0f
    private var startY = 0f
    private var dragging = false
    private var buttonX: Float? = null
    private var buttonY: Float? = null

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
        entryButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.screen_log_ic_expand, 0, 0, 0)
        entryButton.compoundDrawablePadding = context.dp(4)
        addView(entryButton, LayoutParams(context.dp(52), context.dp(48)))

        menuView.visibility = View.GONE
        menuView.onCollapse = {
            menuView.visibility = View.GONE
            resizeForMenu()
        }
        menuScroll.isFillViewport = false
        menuScroll.addView(menuView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        menuScroll.visibility = View.GONE
        addView(menuScroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dp(54)
        })

        entryButton.setOnClickListener {
            if (!dragging) {
                menuView.visibility = if (menuView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                resizeForMenu()
            }
        }

        entryButton.setOnTouchListener { _, event -> handleTouch(event) }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> constrainPosition() }
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
                startX = buttonX ?: x
                startY = buttonY ?: y
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (kotlin.math.abs(dx) > context.dp(4) || kotlin.math.abs(dy) > context.dp(4)) {
                    if (!dragging && menuView.visibility == View.VISIBLE) {
                        menuView.visibility = View.GONE
                        resizeForMenu()
                    }
                    dragging = true
                    buttonX = startX + dx
                    buttonY = startY + dy
                    constrainPosition()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) stickToEdge()
                savePosition()
                if (!dragging && event.actionMasked == MotionEvent.ACTION_UP) entryButton.performClick()
                dragging = false
                return true
            }
        }
        return false
    }

    fun savePosition() {
        ScreenLogManager.setFloatingPosition((buttonX ?: x).toInt(), (buttonY ?: y).toInt())
    }

    private fun stickToEdge() {
        val parentWidth = (parent as? View)?.width ?: return
        val targetX = if ((buttonX ?: x) + entryButton.width / 2f > parentWidth / 2f) {
            parentWidth - entryButton.width - context.dp(8)
        } else context.dp(8)
        buttonX = targetX.toFloat()
        constrainPosition()
        savePosition()
    }

    /** 只移动菜单容器来避让边界，入口始终保持在用户拖动的位置。 */
    private fun constrainPosition() {
        val host = parent as? View ?: return
        if (host.width == 0 || host.height == 0) return
        val anchorX = (buttonX ?: x).coerceIn(0f, (host.width - entryButton.width).coerceAtLeast(0).toFloat())
        val anchorY = (buttonY ?: y).coerceIn(0f, (host.height - entryButton.height).coerceAtLeast(0).toFloat())
        buttonX = anchorX
        buttonY = anchorY
        val horizontal = ScreenLogFloatingPosition.horizontal(anchorX, width, host.width)
        x = horizontal.containerLeft
        entryButton.translationX = horizontal.buttonOffset
        y = anchorY - (entryButton.layoutParams as LayoutParams).topMargin
    }

    private fun resizeForMenu() {
        val params = layoutParams
        val host = parent as? View
        params.width = if (menuView.visibility == View.VISIBLE) {
            context.dp(216).coerceAtMost(host?.width?.takeIf { it > 0 } ?: context.dp(216))
        } else context.dp(52)
        menuScroll.visibility = menuView.visibility
        params.height = LayoutParams.WRAP_CONTENT
        layoutParams = params
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val host = parent as? View
        if (menuView.visibility == View.VISIBLE && host != null && host.height > 0) {
            val anchorY = (buttonY ?: y).coerceIn(0f, (host.height - context.dp(48)).coerceAtLeast(0).toFloat())
            val above = (anchorY.toInt() - context.dp(6)).coerceAtLeast(0)
            val below = (host.height - anchorY.toInt() - context.dp(54)).coerceAtLeast(0)
            // 向空间更大的一侧展开，空间不足时滚动菜单。
            val showAbove = above > below
            menuScroll.layoutParams.height = maxOf(above, below).coerceAtLeast(1)
            menuScroll.measure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(menuScroll.layoutParams.height, MeasureSpec.AT_MOST)
            )
            menuScroll.layoutParams.height = menuScroll.measuredHeight
            (menuScroll.layoutParams as LayoutParams).topMargin = if (showAbove) 0 else context.dp(54)
            (entryButton.layoutParams as LayoutParams).topMargin = if (showAbove) menuScroll.measuredHeight + context.dp(6) else 0
        } else {
            (entryButton.layoutParams as LayoutParams).topMargin = 0
        }
        // 初始挂载的 topMargin 不能压缩向上展开菜单的测量空间。
        val availableHeightSpec = if (menuView.visibility == View.VISIBLE && host != null && host.height > 0) {
            MeasureSpec.makeMeasureSpec(host.height, MeasureSpec.AT_MOST)
        } else heightMeasureSpec
        super.onMeasure(widthMeasureSpec, availableHeightSpec)
    }
}
