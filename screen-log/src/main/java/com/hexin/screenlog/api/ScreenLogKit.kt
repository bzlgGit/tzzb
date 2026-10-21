package com.hexin.screenlog.api

import android.app.Activity
import android.app.Application
import com.hexin.screenlog.core.ScreenLogPreferences

/** 业务方接入屏幕日志的统一入口。 */
object ScreenLogKit {

    private var config: ScreenLogConfig? = null

    @JvmStatic
    @JvmOverloads
    fun init(application: Application, hiddenRequestInterfaces: Set<String> = emptySet()) {
        val restoreEntry = ScreenLogPreferences(application).entryEnabled()
        val cfg = ScreenLogConfig(
            application = application,
            enable = true,
            hiddenRequestInterfaces = hiddenRequestInterfaces
        )
        config = cfg
        ScreenLogManager.init(cfg.copy(enable = restoreEntry))
        if (restoreEntry) ScreenLogManager.showFloatingEntry()
    }

    /** 总开关状态：入口是否启用；录制可在组件菜单中单独暂停。 */
    @JvmStatic
    fun isOpen(): Boolean = config?.let { ScreenLogPreferences(it.application).entryEnabled() } ?: false

    @JvmStatic
    fun open(activity: Activity) {
        val cfg = config ?: return
        ScreenLogPreferences(cfg.application).setEntryEnabled(true)
        ScreenLogManager.init(cfg)
        ScreenLogManager.setRecordingEnabled(true)
        ScreenLogManager.attachTo(activity)
        ScreenLogManager.showFloatingEntry()
    }

    @JvmStatic
    fun close() {
        val cfg = config ?: return
        val preferences = ScreenLogPreferences(cfg.application)
        preferences.setEntryEnabled(false)
        preferences.setRecordingEnabled(false)
        ScreenLogManager.setRecordingEnabled(false)
        ScreenLogManager.hideFloatingEntry()
        ScreenLogManager.init(cfg.copy(enable = false))
    }
}
