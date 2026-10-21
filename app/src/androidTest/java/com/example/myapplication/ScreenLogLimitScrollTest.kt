package com.example.myapplication

import android.app.Application
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hexin.screenlog.R as ScreenLogR
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogKit
import com.hexin.screenlog.core.ScreenLogDispatcher
import com.hexin.screenlog.ui.ScreenLogAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ScreenLogLimitScrollTest {

    @Test
    fun evictingOldLogsKeepsVisibleEntryInPlace() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", android.content.Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        val previousAutoScrollPaused = ScreenLogDispatcher.isAutoScrollPaused()
        val activity = instrumentation.startActivitySync(
            Intent(application, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        try {
            instrumentation.runOnMainSync {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ScreenLogKit.open(activity)
            }
            repeat(1_000) { ScreenLog.i("LimitScrollTest", "entry-$it") }
            instrumentation.runOnMainSync {
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_entry))!!.performClick()
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_panel_visible))!!.performClick()
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                for (index in 0 until content.childCount) {
                    (content.getChildAt(index) as? ViewGroup)?.apply {
                        fitsSystemWindows = true
                        clipToPadding = true
                    }
                }
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_fullscreen))!!.performClick()
            }

            val list = activity.findViewById<RecyclerView>(ScreenLogR.id.screenLogList)
            waitFor(instrumentation) { list.adapter?.itemCount == 1_000 }
            Thread.sleep(250)
            instrumentation.runOnMainSync { ScreenLogDispatcher.setAutoScrollPaused(true) }
            var oldestMessage = ""
            instrumentation.runOnMainSync {
                oldestMessage = (list.adapter as ScreenLogAdapter).currentList.first().message
            }

            var visibleMessage = ""
            var visibleTop = 0
            instrumentation.runOnMainSync {
                (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(500, 0)
            }
            waitFor(instrumentation, diagnostic = {
                val manager = list.layoutManager as LinearLayoutManager
                "listHeight=${list.height}, first=${manager.findFirstVisibleItemPosition()}, " +
                    "last=${manager.findLastVisibleItemPosition()}, canDown=${list.canScrollVertically(1)}, " +
                    "scrollState=${list.scrollState}, shown=${list.isShown}, windowFocus=${activity.hasWindowFocus()}"
            }) {
                (list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() in 0..998 &&
                    list.canScrollVertically(1) && list.scrollState == RecyclerView.SCROLL_STATE_IDLE
            }
            val jumpToLatest = activity.findViewById<TextView>(ScreenLogR.id.screenLogJumpToLatest)
            waitFor(instrumentation, diagnostic = {
                val buttonLocation = IntArray(2)
                jumpToLatest.getLocationOnScreen(buttonLocation)
                val root = jumpToLatest.rootView
                val insets = ViewCompat.getRootWindowInsets(jumpToLatest)
                "shown=${jumpToLatest.isShown}, button=${buttonLocation.toList()} " +
                    "size=${jumpToLatest.width}x${jumpToLatest.height}, root=${root.width}x${root.height}, " +
                    "nav=${insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom}, " +
                    "panel=${jumpToLatest.parent?.let { (it as View).height }}"
            }) { jumpToLatest.isShown && jumpToLatest.getGlobalVisibleRect(Rect()) }
            instrumentation.runOnMainSync {
                assertJumpToLatestCentered(jumpToLatest)
                val manager = list.layoutManager as LinearLayoutManager
                val position = manager.findFirstVisibleItemPosition()
                visibleMessage = (list.adapter as ScreenLogAdapter).currentList[position].message
                visibleTop = manager.findViewByPosition(position)!!.top
                val index = manager.findViewByPosition(position)!!
                    .findViewById<TextView>(ScreenLogR.id.screenLogIndex).text.toString()
                assertEquals((position + 1).toString(), index)
            }

            instrumentation.runOnMainSync {
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_restore_size))!!.performClick()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { assertJumpToLatestCentered(jumpToLatest) }
            val smallPanelHeight = (jumpToLatest.parent as View).height
            onView(withId(ScreenLogR.id.screenLogTitle)).perform(doubleClick())
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertTrue("双击后应进入中屏", (jumpToLatest.parent as View).height > smallPanelHeight)
                assertJumpToLatestCentered(jumpToLatest)
            }
            instrumentation.runOnMainSync {
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_fullscreen))!!.performClick()
            }

            repeat(10) { ScreenLog.i("LimitScrollTest", "overflow-$it") }
            waitFor(instrumentation) {
                (list.adapter as ScreenLogAdapter).currentList.lastOrNull()?.message == "overflow-9"
            }
            instrumentation.runOnMainSync {
                val manager = list.layoutManager as LinearLayoutManager
                val position = manager.findFirstVisibleItemPosition()
                val entries = (list.adapter as ScreenLogAdapter).currentList
                assertEquals(1_000, entries.size)
                assertNotEquals("应确实淘汰最旧日志", oldestMessage, entries.first().message)
                val currentMessage = entries[position].message
                assertEquals("淘汰旧日志时应保留正在看的条目", visibleMessage, currentMessage)
                assertEquals("淘汰旧日志时应保留条目的屏幕位置", visibleTop, manager.findViewByPosition(position)!!.top)
                val index = manager.findViewByPosition(position)!!
                    .findViewById<TextView>(ScreenLogR.id.screenLogIndex).text.toString()
                assertEquals("淘汰后序号应对应新位置", (position + 1).toString(), index)
                assertTrue("浏览旧日志时应显示跳转最新入口", jumpToLatest.isShown)
                assertTrue(list.canScrollVertically(1))
                jumpToLatest.performClick()
            }
            waitFor(instrumentation) { !list.canScrollVertically(1) }
            instrumentation.runOnMainSync { assertFalse("已经在底部时应隐藏跳转入口", jumpToLatest.isShown) }
        } finally {
            instrumentation.runOnMainSync {
                ScreenLogDispatcher.setAutoScrollPaused(previousAutoScrollPaused)
                ScreenLogKit.close()
                activity.finish()
            }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
        }
    }

    private fun waitFor(
        instrumentation: android.app.Instrumentation,
        diagnostic: () -> String = { "" },
        condition: () -> Boolean
    ) {
        repeat(50) {
            instrumentation.waitForIdleSync()
            var matched = false
            instrumentation.runOnMainSync { matched = condition() }
            if (matched) return
            Thread.sleep(100)
        }
        var details = ""
        instrumentation.runOnMainSync { details = diagnostic() }
        throw AssertionError("等待日志列表更新超时：$details")
    }

    private fun findTextView(view: View, text: String): TextView? {
        if (view is TextView && view.text.toString() == text) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findTextView(view.getChildAt(index), text)?.let { return it }
            }
        }
        return null
    }

    private fun assertJumpToLatestCentered(button: TextView) {
        val bounds = Rect()
        assertTrue("跳转最新入口应有可见区域", button.getGlobalVisibleRect(bounds))
        assertEquals("跳转最新入口应完整显示", button.width, bounds.width())
        assertEquals("跳转最新入口应完整显示", button.height, bounds.height())
        val panel = button.parent as View
        val contentCenterY = (panel.paddingTop + panel.height - panel.paddingBottom) / 2
        val buttonCenterY = button.top + button.height / 2
        assertTrue("跳转最新入口应在面板垂直中心", abs(buttonCenterY - contentCenterY) <= 2)
        val navigationBarHeight = ViewCompat.getRootWindowInsets(button)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val root = button.rootView
        val rootLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        assertTrue("跳转最新入口不应与导航栏重叠：$bounds", bounds.bottom <= rootLocation[1] + root.height - navigationBarHeight)
    }
}
