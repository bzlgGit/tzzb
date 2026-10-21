package com.example.myapplication

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hexin.screenlog.R as ScreenLogR
import com.hexin.screenlog.api.ScreenLog
import com.hexin.screenlog.api.ScreenLogKit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenLogKitLifecycleTest {

    @Test
    fun floatingEntryWorksAcrossActivities() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        val mainActivity = launchActivity(instrumentation, application)
        val monitor = instrumentation.addMonitor(SecondPageActivity::class.java.name, null, false)
        var secondActivity: Activity? = null

        try {
            instrumentation.runOnMainSync {
                ScreenLogKit.close()
                ScreenLogKit.open(mainActivity)
                val content = mainActivity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue("主页面应显示悬浮入口", findTextView(content, mainActivity.getString(ScreenLogR.string.screen_log_entry)) != null)
                findTextView(content, "Open second page")!!.performClick()
            }

            secondActivity = instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
            assertTrue("应打开独立的第二个 Activity", secondActivity is SecondPageActivity)
            instrumentation.waitForIdleSync()

            instrumentation.runOnMainSync {
                val content = secondActivity!!.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, secondActivity.getString(ScreenLogR.string.screen_log_entry))!!.performClick()
                findTextView(content, secondActivity.getString(ScreenLogR.string.screen_log_panel_visible))!!.performClick()
                assertTrue("第二页面应能打开日志面板", content.childCount > 2)
                findTextView(content, "Add log from second page")!!.performClick()
                secondActivity.finish()
            }
            instrumentation.waitForIdleSync()

            instrumentation.runOnMainSync {
                val content = mainActivity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue("返回主页面后悬浮入口应恢复", findTextView(content, mainActivity.getString(ScreenLogR.string.screen_log_entry)) != null)
                assertTrue("返回主页面后日志面板应恢复", content.childCount > 2)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            instrumentation.runOnMainSync {
                ScreenLogKit.close()
                secondActivity?.finish()
                mainActivity.finish()
            }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
        }
    }

    @Test
    fun floatingMenuCloseStopsRecordingAndHidesEntryAfterRestart() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        var activity = launchActivity(instrumentation, application)

        try {
            instrumentation.runOnMainSync {
                ScreenLogKit.open(activity)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_entry))!!.performClick()
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_close))!!.performClick()
            }
            instrumentation.waitForIdleSync()
            assertFalse(providerWasCalled())

            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
            ScreenLogKit.init(application)
            activity = launchActivity(instrumentation, application)
            instrumentation.waitForIdleSync()

            var entryVisible = false
            instrumentation.runOnMainSync {
                entryVisible = activity.findViewById<ViewGroup>(android.R.id.content).childCount > 1
            }
            assertFalse("菜单关闭后重启不应显示悬浮入口", entryVisible)
        } finally {
            instrumentation.runOnMainSync { ScreenLogKit.close() }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test
    fun floatingEntryRestoresWhenRecordingWasOpen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        var activity = launchActivity(instrumentation, application)

        try {
            instrumentation.runOnMainSync { ScreenLogKit.close() }
            preferences.edit().putBoolean("recording_enabled", true)
                .putBoolean("entry_enabled", true).commit()
            ScreenLogKit.init(application)

            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
            activity = launchActivity(instrumentation, application)
            instrumentation.waitForIdleSync()

            var entryVisible = false
            instrumentation.runOnMainSync {
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                entryVisible = content.childCount > 1
            }
            assertTrue("录制恢复后应自动显示悬浮入口", entryVisible)
        } finally {
            instrumentation.runOnMainSync { ScreenLogKit.close() }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test
    fun recordingIsOffUntilOpenedAndRestoresAfterReinitialization() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        val intent = Intent(application, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val activity = instrumentation.startActivitySync(intent) as Activity

        try {
            preferences.edit().putBoolean("recording_enabled", false)
                .putBoolean("entry_enabled", false).commit()
            ScreenLogKit.init(application)
            assertFalse(providerWasCalled())

            instrumentation.runOnMainSync { ScreenLogKit.open(activity) }
            assertTrue(providerWasCalled())

            ScreenLogKit.init(application)
            assertTrue(providerWasCalled())

            instrumentation.runOnMainSync { ScreenLogKit.close() }
            ScreenLogKit.init(application)
            assertFalse(providerWasCalled())
        } finally {
            instrumentation.runOnMainSync { ScreenLogKit.close() }
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test
    fun pausedRecordingKeepsTheEntryEnabledAfterReinitialization() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)
        val activity = launchActivity(instrumentation, application)

        try {
            instrumentation.runOnMainSync {
                ScreenLogKit.open(activity)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_entry))!!.performClick()
                findTextView(content, activity.getString(ScreenLogR.string.screen_log_recording))!!.performClick()
                assertTrue(ScreenLogKit.isOpen())
            }
            assertFalse(providerWasCalled())

            instrumentation.runOnMainSync {
                ScreenLogKit.init(application)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(findTextView(content, activity.getString(ScreenLogR.string.screen_log_entry)) != null)
                assertTrue(ScreenLogKit.isOpen())
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

    @Test
    fun legacyRecordingStateMigratesToTheEntrySwitchOnlyOnce() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val preferences = application.getSharedPreferences("screen_log_preferences", Context.MODE_PRIVATE)
        val previousRecording = preferences.getBoolean("recording_enabled", false)
        val previousEntry = preferences.getBoolean("entry_enabled", false)

        try {
            preferences.edit().remove("entry_enabled").putBoolean("recording_enabled", true).commit()
            ScreenLogKit.init(application)
            assertTrue(ScreenLogKit.isOpen())

            preferences.edit().putBoolean("recording_enabled", false).commit()
            ScreenLogKit.init(application)
            assertTrue("录制暂停后，已迁移的总开关不应被覆盖", ScreenLogKit.isOpen())
        } finally {
            ScreenLogKit.close()
            preferences.edit().putBoolean("recording_enabled", previousRecording)
                .putBoolean("entry_enabled", previousEntry).commit()
            ScreenLogKit.init(application)
        }
    }

    private fun providerWasCalled(): Boolean {
        var called = false
        ScreenLog.request(api = "/test/unlisted") {
            called = true
            "test"
        }
        return called
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

    private fun launchActivity(
        instrumentation: android.app.Instrumentation,
        application: Application
    ): Activity {
        val intent = Intent(application, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return instrumentation.startActivitySync(intent) as Activity
    }
}
