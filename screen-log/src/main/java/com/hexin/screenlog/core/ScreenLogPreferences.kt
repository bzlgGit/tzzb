package com.hexin.screenlog.core

import android.content.Context
import com.hexin.screenlog.api.ScreenLogCopyOptions
import com.hexin.screenlog.api.ScreenLogTouchMode

internal class ScreenLogPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    fun recordingEnabled(defaultValue: Boolean): Boolean =
        preferences.getBoolean(KEY_RECORDING_ENABLED, defaultValue)

    fun setRecordingEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_RECORDING_ENABLED, enabled).apply()
    }

    fun persistEnabled(defaultValue: Boolean): Boolean =
        preferences.getBoolean(KEY_PERSIST_ENABLED, defaultValue)

    fun setPersistEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PERSIST_ENABLED, enabled).apply()
    }

    fun autoScrollEnabled(defaultValue: Boolean): Boolean =
        preferences.getBoolean(KEY_AUTO_SCROLL_ENABLED, defaultValue)

    fun setAutoScrollEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SCROLL_ENABLED, enabled).apply()
    }

    fun touchMode(defaultValue: ScreenLogTouchMode): ScreenLogTouchMode {
        val storedValue = preferences.getString(KEY_TOUCH_MODE, null) ?: return defaultValue
        return runCatching { ScreenLogTouchMode.valueOf(storedValue) }.getOrDefault(defaultValue)
    }

    fun setTouchMode(mode: ScreenLogTouchMode) {
        preferences.edit().putString(KEY_TOUCH_MODE, mode.name).apply()
    }

    fun copyOptions(defaultValue: ScreenLogCopyOptions): ScreenLogCopyOptions {
        return ScreenLogCopyOptions(
            includeTime = preferences.getBoolean(KEY_COPY_TIME, defaultValue.includeTime),
            includeLevel = preferences.getBoolean(KEY_COPY_LEVEL, defaultValue.includeLevel),
            includeTag = preferences.getBoolean(KEY_COPY_TAG, defaultValue.includeTag),
            includeThreadName = preferences.getBoolean(KEY_COPY_THREAD, defaultValue.includeThreadName),
            includeExtras = preferences.getBoolean(KEY_COPY_EXTRAS, defaultValue.includeExtras)
        )
    }

    fun setCopyOptions(options: ScreenLogCopyOptions) {
        preferences.edit()
            .putBoolean(KEY_COPY_TIME, options.includeTime)
            .putBoolean(KEY_COPY_LEVEL, options.includeLevel)
            .putBoolean(KEY_COPY_TAG, options.includeTag)
            .putBoolean(KEY_COPY_THREAD, options.includeThreadName)
            .putBoolean(KEY_COPY_EXTRAS, options.includeExtras)
            .apply()
    }

    fun panelExpanded(defaultValue: Boolean): Boolean =
        preferences.getBoolean(KEY_PANEL_EXPANDED, defaultValue)

    fun setPanelExpanded(expanded: Boolean) {
        preferences.edit().putBoolean(KEY_PANEL_EXPANDED, expanded).apply()
    }

    fun floatingX(): Int? = preferences.takeIf { it.contains(KEY_FLOATING_X) }
        ?.getInt(KEY_FLOATING_X, 0)

    fun floatingY(): Int? = preferences.takeIf { it.contains(KEY_FLOATING_Y) }
        ?.getInt(KEY_FLOATING_Y, 0)

    fun setFloatingPosition(x: Int, y: Int) {
        preferences.edit()
            .putInt(KEY_FLOATING_X, x)
            .putInt(KEY_FLOATING_Y, y)
            .apply()
    }

    fun addedRequestInterfaces(): Set<String> =
        preferences.getStringSet(KEY_ADDED_REQUEST_INTERFACES, emptySet()).orEmpty().toSet()

    fun enabledRequestInterfaces(): Set<String> =
        preferences.getStringSet(KEY_ENABLED_REQUEST_INTERFACES, emptySet()).orEmpty().toSet()

    fun setRequestInterfaces(added: Set<String>, enabled: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_ADDED_REQUEST_INTERFACES, added.toSet())
            .remove(KEY_REMOVED_REQUEST_INTERFACES)
            .putStringSet(KEY_ENABLED_REQUEST_INTERFACES, enabled.toSet())
            .apply()
    }

    private companion object {
        const val PREFERENCE_NAME = "screen_log_preferences"
        const val KEY_RECORDING_ENABLED = "recording_enabled"
        const val KEY_PERSIST_ENABLED = "persist_enabled"
        const val KEY_AUTO_SCROLL_ENABLED = "auto_scroll_enabled"
        const val KEY_TOUCH_MODE = "touch_mode"
        const val KEY_COPY_TIME = "copy_time"
        const val KEY_COPY_LEVEL = "copy_level"
        const val KEY_COPY_TAG = "copy_tag"
        const val KEY_COPY_THREAD = "copy_thread"
        const val KEY_COPY_EXTRAS = "copy_extras"
        const val KEY_PANEL_EXPANDED = "panel_expanded"
        const val KEY_FLOATING_X = "floating_x"
        const val KEY_FLOATING_Y = "floating_y"
        const val KEY_ADDED_REQUEST_INTERFACES = "added_request_interfaces"
        const val KEY_REMOVED_REQUEST_INTERFACES = "removed_request_interfaces"
        const val KEY_ENABLED_REQUEST_INTERFACES = "enabled_request_interfaces"
    }
}
