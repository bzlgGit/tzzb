package com.example.myapplication

import android.app.Application
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hexin.screenlog.R as ScreenLogR
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogKit
import com.hexin.screenlog.ui.ScreenLogAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenLogLimitScrollTest {

    @Test
    fun evictingOldLogsKeepsVisibleEntryInPlace() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", android.content.Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        val activity = instrumentation.startActivitySync(
            Intent(application, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        try {
            instrumentation.runOnMainSync {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
                ScreenLogKit.open(activity)
            }
            repeat(1_000) { ScreenLog.i("LimitScrollTest", "entry-$it") }
            instrumentation.runOnMainSync {
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_entry))!!.performClick()
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_panel_visible))!!.performClick()
            }

            val list = activity.findViewById<RecyclerView>(ScreenLogR.id.screenLogList)
            waitFor(instrumentation) { list.adapter?.itemCount == 1_000 }
            var oldestMessage = ""
            instrumentation.runOnMainSync {
                oldestMessage = (list.adapter as ScreenLogAdapter).currentList.first().message
            }

            var visibleMessage = ""
            var visibleTop = 0
            instrumentation.runOnMainSync {
                val manager = list.layoutManager as LinearLayoutManager
                manager.scrollToPositionWithOffset(500, 0)
            }
            waitFor(instrumentation) {
                (list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() >= 0
            }
            instrumentation.runOnMainSync {
                val manager = list.layoutManager as LinearLayoutManager
                val position = manager.findFirstVisibleItemPosition()
                visibleMessage = (list.adapter as ScreenLogAdapter).currentList[position].message
                visibleTop = manager.findViewByPosition(position)!!.top
                val index = manager.findViewByPosition(position)!!
                    .findViewById<TextView>(ScreenLogR.id.screenLogIndex).text.toString()
                assertEquals((position + 1).toString(), index)
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
            }
        } finally {
            instrumentation.runOnMainSync {
                ScreenLogKit.close()
                activity.finish()
            }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
        }
    }

    private fun waitFor(instrumentation: android.app.Instrumentation, condition: () -> Boolean) {
        repeat(50) {
            instrumentation.waitForIdleSync()
            var matched = false
            instrumentation.runOnMainSync { matched = condition() }
            if (matched) return
            Thread.sleep(100)
        }
        throw AssertionError("等待日志列表更新超时")
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
}
