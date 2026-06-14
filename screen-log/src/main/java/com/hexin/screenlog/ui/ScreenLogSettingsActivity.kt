package com.hexin.screenlog.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode
import com.hexin.screenlog.databinding.ScreenLogSettingsActivityBinding

class ScreenLogSettingsActivity : Activity() {

    private lateinit var binding: ScreenLogSettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenLogSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        refreshSummary()
        binding.screenLogTouchLogPanel.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.LOG_PANEL)
            refreshSummary()
        }
        binding.screenLogTouchUnderlyingPage.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.UNDERLYING_PAGE)
            refreshSummary()
        }
        binding.screenLogClearAll.setOnClickListener {
            ScreenLogManager.clearLogs()
            refreshSummary()
        }
    }

    private fun refreshSummary() {
        binding.screenLogSettingsSummary.text = ScreenLogManager.settingsSummary()
        val touchMode = ScreenLogManager.getTouchMode()
        binding.screenLogTouchLogPanel.isSelected = touchMode == ScreenLogTouchMode.LOG_PANEL
        binding.screenLogTouchUnderlyingPage.isSelected = touchMode == ScreenLogTouchMode.UNDERLYING_PAGE
        binding.screenLogTouchLogPanel.typeface = if (touchMode == ScreenLogTouchMode.LOG_PANEL) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        binding.screenLogTouchUnderlyingPage.typeface = if (touchMode == ScreenLogTouchMode.UNDERLYING_PAGE) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
