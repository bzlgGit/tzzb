package com.hexin.screenlog.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLogPanelStateTest {
    @Test
    fun fullscreenRestoresSmallSize() {
        val state = ScreenLogPanelState()
        state.toggleFullscreen()
        assertTrue(state.fullscreen)
        state.toggleFullscreen()
        assertFalse(state.fullscreen)
        assertFalse(state.large)
    }

    @Test
    fun fullscreenRestoresMediumSizeSelectedByDoubleTap() {
        val state = ScreenLogPanelState()
        state.toggleCompactSize()
        state.toggleFullscreen()
        state.toggleCompactSize()
        state.toggleFullscreen()
        assertFalse(state.fullscreen)
        assertTrue(state.large)
        state.toggleCompactSize()
        assertFalse(state.large)
    }
}
