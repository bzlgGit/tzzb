package com.hexin.screenlog.api

import android.app.Activity
import android.app.Application
import com.hexin.screenlog.core.ScreenLogPreferences

/** 业务方接入屏幕日志的统一入口。 */
object ScreenLogKit {

    private var config: ScreenLogConfig? = null

    fun init(application: Application, hiddenRequestInterfaces: Set<String> = emptySet()) {
        val restoreRecording = ScreenLogPreferences(application).recordingEnabled(false)
        val cfg = ScreenLogConfig(
            application = application,
            enable = true,
            hiddenRequestInterfaces = hiddenRequestInterfaces
        )
        config = cfg
        ScreenLogManager.init(cfg.copy(enable = restoreRecording))
        if (restoreRecording) ScreenLogManager.showFloatingEntry()
    }

    fun open(activity: Activity) {
        val cfg = config ?: return
        ScreenLogManager.init(cfg)
        ScreenLogManager.setRecordingEnabled(true)
        ScreenLogManager.attachTo(activity)
        ScreenLogManager.showFloatingEntry()
    }

    fun close() {
        val cfg = config ?: return
        ScreenLogManager.setRecordingEnabled(false)
        ScreenLogManager.hideFloatingEntry()
        ScreenLogManager.init(cfg.copy(enable = false))
    }
}
