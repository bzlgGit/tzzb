package com.hexin.screenlog.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogManager

class ScreenLogSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_log_settings_activity)
        findViewById<TextView>(R.id.screenLogSettingsSummary).text = ScreenLogManager.settingsSummary()
        findViewById<Button>(R.id.screenLogClearAll).setOnClickListener {
            ScreenLogManager.clearLogs()
            findViewById<TextView>(R.id.screenLogSettingsSummary).text = ScreenLogManager.settingsSummary()
        }
    }
}
