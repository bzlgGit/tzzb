package com.hexin.screenlog.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import androidx.core.content.ContextCompat
import com.hexin.screenlog.R
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
        updateTouchButton(binding.screenLogTouchLogPanel, touchMode == ScreenLogTouchMode.LOG_PANEL)
        updateTouchButton(binding.screenLogTouchUnderlyingPage, touchMode == ScreenLogTouchMode.UNDERLYING_PAGE)
    }

    private fun updateTouchButton(button: Button, selected: Boolean) {
        button.isSelected = selected
        button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        button.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) android.R.color.white else R.color.screen_log_accent
            )
        )
        button.background = ContextCompat.getDrawable(
            this,
            if (selected) R.drawable.screen_log_segment_selected_bg else R.drawable.screen_log_segment_unselected_bg
        )
    }
}
