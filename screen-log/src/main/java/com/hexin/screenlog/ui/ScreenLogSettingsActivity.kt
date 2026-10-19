package com.hexin.screenlog.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogCopyOptions
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode
import com.hexin.screenlog.core.ScreenLogDispatcher
import com.hexin.screenlog.databinding.ScreenLogSettingsActivityBinding

class ScreenLogSettingsActivity : Activity(), ScreenLogDispatcher.Listener {

    private lateinit var binding: ScreenLogSettingsActivityBinding
    private var suppressSwitchCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenLogSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        refreshSettings()
        binding.screenLogPanelVisible.setOnCheckedChangeListener { _, checked ->
            if (!suppressSwitchCallbacks) ScreenLogManager.setPanelVisible(checked)
        }
        binding.screenLogRecording.setOnCheckedChangeListener { _, checked ->
            if (!suppressSwitchCallbacks) ScreenLogManager.setRecordingEnabled(checked)
        }
        binding.screenLogPersistence.setOnCheckedChangeListener { _, checked ->
            if (!suppressSwitchCallbacks) {
                ScreenLogManager.setPersistEnabled(checked)
                refreshStorageSummary()
            }
        }
        binding.screenLogAutoScroll.setOnCheckedChangeListener { _, checked ->
            if (!suppressSwitchCallbacks) ScreenLogManager.setAutoScrollEnabled(checked)
        }
        binding.screenLogTouchLogPanel.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.LOG_PANEL)
            refreshTouchMode()
        }
        binding.screenLogTouchUnderlyingPage.setOnClickListener {
            ScreenLogManager.setTouchMode(ScreenLogTouchMode.UNDERLYING_PAGE)
            refreshTouchMode()
        }
        listOf(
            binding.screenLogCopyTime,
            binding.screenLogCopyLevel,
            binding.screenLogCopyTag,
            binding.screenLogCopyThread,
            binding.screenLogCopyExtras
        ).forEach { copySwitch ->
            copySwitch.setOnCheckedChangeListener { _, _ ->
                if (!suppressSwitchCallbacks) saveCopyOptions()
            }
        }
        binding.screenLogClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.screen_log_clear_confirm_title)
                .setMessage(R.string.screen_log_clear_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.screen_log_clear) { _, _ ->
                    ScreenLogManager.clearLogs()
                    refreshStorageSummary()
                }
                .show()
        }
        binding.screenLogExport.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, ScreenLogManager.suggestedExportFileName())
                },
                REQUEST_EXPORT_LOGS
            )
        }
    }

    override fun onStart() {
        super.onStart()
        ScreenLogDispatcher.addListener(this)
        refreshSettings()
    }

    override fun onResume() {
        super.onResume()
        refreshSettings()
    }

    override fun onStop() {
        ScreenLogDispatcher.removeListener(this)
        super.onStop()
    }

    @Deprecated("Uses the platform Activity API to avoid requiring a host Activity dependency.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EXPORT_LOGS || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        ScreenLogManager.exportLogs(contentResolver, uri) { result ->
            result.onSuccess {
                AlertDialog.Builder(this)
                    .setTitle(R.string.screen_log_export_success_title)
                    .setMessage(R.string.screen_log_export_success_message)
                    .setNegativeButton(android.R.string.ok, null)
                    .setPositiveButton(R.string.screen_log_open_export) { _, _ -> openExportedFile(uri) }
                    .show()
            }.onFailure {
                Toast.makeText(this, R.string.screen_log_export_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onLogsChanged() = Unit

    override fun onAutoScrollChanged(paused: Boolean) {
        updateSwitches { binding.screenLogAutoScroll.isChecked = !paused }
    }

    override fun onRecordingChanged(enabled: Boolean) {
        updateSwitches { binding.screenLogRecording.isChecked = enabled }
    }

    private fun refreshSettings() {
        updateSwitches {
            binding.screenLogPanelVisible.isChecked = ScreenLogManager.isPanelVisible()
            binding.screenLogRecording.isChecked = ScreenLogManager.isRecordingEnabled()
            binding.screenLogPersistence.isChecked = ScreenLogManager.isPersistEnabled()
            binding.screenLogAutoScroll.isChecked = ScreenLogManager.isAutoScrollEnabled()
            val copyOptions = ScreenLogManager.copyOptions()
            binding.screenLogCopyMessage.isChecked = true
            binding.screenLogCopyTime.isChecked = copyOptions.includeTime
            binding.screenLogCopyLevel.isChecked = copyOptions.includeLevel
            binding.screenLogCopyTag.isChecked = copyOptions.includeTag
            binding.screenLogCopyThread.isChecked = copyOptions.includeThreadName
            binding.screenLogCopyExtras.isChecked = copyOptions.includeExtras
        }
        refreshTouchMode()
        refreshStorageSummary()
    }

    private fun refreshTouchMode() {
        val touchMode = ScreenLogManager.getTouchMode()
        updateTouchButton(binding.screenLogTouchLogPanel, touchMode == ScreenLogTouchMode.LOG_PANEL)
        updateTouchButton(binding.screenLogTouchUnderlyingPage, touchMode == ScreenLogTouchMode.UNDERLYING_PAGE)
    }

    private fun refreshStorageSummary() {
        binding.screenLogSettingsSummary.text = ScreenLogManager.settingsSummary()
    }

    private fun saveCopyOptions() {
        ScreenLogManager.setCopyOptions(
            ScreenLogCopyOptions(
                includeTime = binding.screenLogCopyTime.isChecked,
                includeLevel = binding.screenLogCopyLevel.isChecked,
                includeTag = binding.screenLogCopyTag.isChecked,
                includeThreadName = binding.screenLogCopyThread.isChecked,
                includeExtras = binding.screenLogCopyExtras.isChecked
            )
        )
    }

    private fun openExportedFile(uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/zip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.screen_log_open_export_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateSwitches(block: () -> Unit) {
        suppressSwitchCallbacks = true
        block()
        suppressSwitchCallbacks = false
    }

    private fun updateTouchButton(button: Button, selected: Boolean) {
        button.isSelected = selected
        button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        button.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) android.R.color.white else R.color.screen_log_brand
            )
        )
        button.background = ContextCompat.getDrawable(
            this,
            if (selected) R.drawable.screen_log_segment_selected_bg else R.drawable.screen_log_page_button_bg
        )
    }

    private companion object {
        const val REQUEST_EXPORT_LOGS = 1001
    }
}
