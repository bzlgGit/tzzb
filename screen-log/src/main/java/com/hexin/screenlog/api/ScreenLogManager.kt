package com.hexin.screenlog.api

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import android.view.ViewGroup
import com.hexin.screenlog.core.ScreenLogDispatcher
import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.storage.ScreenLogCleaner
import com.hexin.screenlog.storage.ScreenLogFileReader
import com.hexin.screenlog.storage.ScreenLogFileWriter
import com.hexin.screenlog.ui.ScreenLogFloatingView
import com.hexin.screenlog.ui.ScreenLogPanelView
import com.hexin.screenlog.ui.ScreenLogPanelState
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object ScreenLogManager {

    private const val TAG = "ScreenLogManager"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initialized = AtomicBoolean(false)
    private var config: ScreenLogConfig? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var floatingView: ScreenLogFloatingView? = null
    private var panelView: ScreenLogPanelView? = null
    private var floatingVisible = false
    private var panelVisible = false
    private var touchMode = ScreenLogTouchMode.LOG_PANEL
    private val panelState = ScreenLogPanelState()

    fun init(config: ScreenLogConfig) {
        this.config = config
        if (!config.enable) return
        val logDir = logDir(config)
        ScreenLogCleaner(logDir).clean(config.keepDays)
        ScreenLogDispatcher.configure(config.maxMemoryEntries)
        ScreenLogDispatcher.replace(ScreenLogFileReader(logDir).readRecent(config.keepDays))
        ScreenLogFileWriter.init(logDir, config)
        if (initialized.compareAndSet(false, true)) {
            config.application.registerActivityLifecycleCallbacks(activityCallbacks)
        }
    }

    fun showFloatingEntry() {
        floatingVisible = true
        runOnMain { currentActivity()?.let { attachFloating(it) } }
    }

    fun attachTo(activity: Activity) {
        runOnMain {
            currentActivityRef = WeakReference(activity)
            if (floatingVisible) attachFloating(activity)
            if (panelVisible) attachPanel(activity)
        }
    }

    fun hideFloatingEntry() {
        floatingVisible = false
        runOnMain {
            hidePanel()
            floatingView?.savePosition()
            floatingView?.let { removeFromParent(it) }
            floatingView = null
        }
    }

    fun showPanel() {
        Log.i(TAG, "showPanel currentActivity=${currentActivity()?.javaClass?.simpleName}, touchMode=$touchMode")
        panelVisible = true
        runOnMain { currentActivity()?.let { attachPanel(it) } }
    }

    fun hidePanel() {
        panelVisible = false
        runOnMain {
            panelView?.let { removeFromParent(it) }
            panelView = null
            floatingView?.setPanelVisible(false)
        }
    }

    fun clearLogs() {
        ScreenLogDispatcher.clear()
        ScreenLogFileWriter.clearAll()
    }

    fun pauseAutoScroll() {
        ScreenLogDispatcher.setAutoScrollPaused(true)
    }

    fun resumeAutoScroll() {
        ScreenLogDispatcher.setAutoScrollPaused(false)
    }

    fun getTouchMode(): ScreenLogTouchMode = touchMode

    fun setTouchMode(mode: ScreenLogTouchMode) {
        Log.i(TAG, "setTouchMode from=$touchMode to=$mode")
        touchMode = mode
        runOnMain {
            panelView?.setTouchMode(mode)
            floatingView?.setTouchMode(mode)
        }
    }

    fun toggleTouchMode() {
        setTouchMode(
            if (touchMode == ScreenLogTouchMode.LOG_PANEL) {
                ScreenLogTouchMode.UNDERLYING_PAGE
            } else {
                ScreenLogTouchMode.LOG_PANEL
            }
        )
    }

    internal fun togglePanel() {
        if (panelVisible) hidePanel() else showPanel()
    }

    internal fun openSettings() {
        val activity = currentActivity() ?: return
        activity.startActivity(android.content.Intent(activity, com.hexin.screenlog.ui.ScreenLogSettingsActivity::class.java))
    }

    internal fun write(
        level: ScreenLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        extras: Map<String, String>?
    ) {
        val cfg = config ?: return
        if (!cfg.enable || level.ordinal < cfg.minLevel.ordinal) return
        val fullMessage = if (throwable == null) message else "$message\n${throwable.stackTraceToString()}"
        val entry = ScreenLogEntry(
            timeMillis = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = fullMessage,
            threadName = Thread.currentThread().name,
            extras = extras
        )
        ScreenLogDispatcher.append(entry)
        ScreenLogFileWriter.write(entry)
    }

    internal fun settingsSummary(): String {
        val cfg = config ?: return "ScreenLog is not initialized."
        val files = logDir(cfg).listFiles()?.filter { it.isFile }?.sortedByDescending { it.name }.orEmpty()
        val fileInfo = files.joinToString(separator = "\n") { "${it.name}  ${it.length() / 1024}KB" }
        return buildString {
            appendLine("Enabled: ${cfg.enable}")
            appendLine("Persist: ${cfg.enablePersist}")
            appendLine("Keep days: ${cfg.keepDays}")
            appendLine("Max file: ${cfg.maxFileSizeMb}MB")
            appendLine("Min level: ${cfg.minLevel}")
            appendLine("Touch target: ${if (touchMode == ScreenLogTouchMode.LOG_PANEL) "log panel" else "underlying page"}")
            appendLine("Memory entries: ${ScreenLogDispatcher.snapshot().size}")
            appendLine()
            append(if (fileInfo.isBlank()) "No log files." else fileInfo)
        }
    }

    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            currentActivityRef = WeakReference(activity)
            if (floatingVisible) attachFloating(activity)
            if (panelVisible) attachPanel(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            if (currentActivity() === activity) {
                floatingView?.savePosition()
                floatingView?.let { removeFromParent(it) }
                panelView?.let { removeFromParent(it) }
                floatingView = null
                panelView = null
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity() === activity) currentActivityRef = null
        }
    }

    private fun attachFloating(activity: Activity) {
        if (!activity.isUsable()) return
        val container = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (floatingView?.parent === container) return
        floatingView?.let { removeFromParent(it) }
        floatingView = ScreenLogFloatingView(activity, panelState).also { view ->
            view.setPanelVisible(panelVisible)
            val left = panelState.floatingX ?: activity.dp(12)
            val top = panelState.floatingY ?: activity.dp(120)
            container.addView(
                view,
                FrameLayout.LayoutParams(
                    activity.dp(52),
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.TOP or android.view.Gravity.START
                ).apply {
                    leftMargin = left
                    topMargin = top
                }
            )
            view.bringToFront()
            view.translationZ = activity.dp(24).toFloat()
            view.setTouchMode(touchMode)
        }
    }

    private fun attachPanel(activity: Activity) {
        if (!activity.isUsable()) return
        val container = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (panelView?.parent === container) return
        panelView?.let { removeFromParent(it) }
        panelView = ScreenLogPanelView(activity, panelState).also { view ->
            val panelHeight = if (panelState.large) {
                activity.resources.displayMetrics.heightPixels * 2 / 3
            } else {
                activity.resources.displayMetrics.heightPixels / 3
            }
            Log.i(TAG, "attachPanel container=${container.javaClass.simpleName}, childCount=${container.childCount}, height=$panelHeight, touchMode=$touchMode")
            container.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    panelHeight,
                    android.view.Gravity.BOTTOM
                )
            )
            view.bringToFront()
            view.translationZ = activity.dp(32).toFloat()
            view.setTouchMode(touchMode)
            Log.i(TAG, "attachPanel added parent=${view.parent?.javaClass?.simpleName}, width=${view.width}, height=${view.height}, layoutHeight=${view.layoutParams?.height}, translationZ=${view.translationZ}")
            floatingView?.bringToFront()
            floatingView?.setPanelVisible(true)
        }
    }

    private fun removeFromParent(view: android.view.View) {
        (view.parent as? ViewGroup)?.removeView(view)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun logDir(config: ScreenLogConfig): File {
        return File(config.application.filesDir, "screen-log")
    }

    private fun Activity.dp(value: Int): Int {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun currentActivity(): Activity? {
        val activity = currentActivityRef?.get() ?: return null
        return activity.takeIf { it.isUsable() }
    }

    private fun Activity.isUsable(): Boolean {
        return !isFinishing && !isDestroyed
    }
}
