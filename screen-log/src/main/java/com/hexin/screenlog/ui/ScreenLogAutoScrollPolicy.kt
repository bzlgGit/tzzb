package com.hexin.screenlog.ui

internal object ScreenLogAutoScrollPolicy {
    fun shouldFollow(paused: Boolean, atBottom: Boolean, userScrolling: Boolean): Boolean =
        !paused && atBottom && !userScrolling
}
