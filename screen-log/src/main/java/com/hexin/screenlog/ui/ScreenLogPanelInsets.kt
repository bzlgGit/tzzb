package com.hexin.screenlog.ui

import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

internal object ScreenLogPanelInsets {
    fun topMargin(container: ViewGroup): Int {
        val statusBarHeight = ViewCompat.getRootWindowInsets(container)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: return 0
        val location = IntArray(2)
        container.getLocationInWindow(location)
        return remainingTopInset(statusBarHeight, location[1], container.paddingTop)
    }

    fun remainingTopInset(statusBarHeight: Int, contentTop: Int, contentPaddingTop: Int): Int {
        return (statusBarHeight - contentTop - contentPaddingTop).coerceAtLeast(0)
    }
}
