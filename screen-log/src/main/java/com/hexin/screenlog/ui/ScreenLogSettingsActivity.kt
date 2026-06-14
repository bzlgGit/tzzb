package com.hexin.screenlog.ui

import android.app.Activity
import android.os.Bundle
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.databinding.ScreenLogSettingsActivityBinding

class ScreenLogSettingsActivity : Activity() {

    private lateinit var binding: ScreenLogSettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenLogSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.screenLogSettingsSummary.text = ScreenLogManager.settingsSummary()
        binding.screenLogClearAll.setOnClickListener {
            ScreenLogManager.clearLogs()
            binding.screenLogSettingsSummary.text = ScreenLogManager.settingsSummary()
        }
    }
}
