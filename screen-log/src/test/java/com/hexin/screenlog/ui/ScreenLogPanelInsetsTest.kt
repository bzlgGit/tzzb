package com.hexin.screenlog.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLogPanelInsetsTest {
    @Test
    fun edgeToEdgeContentLeavesStatusBarHeightAbovePanel() {
        assertEquals(72, ScreenLogPanelInsets.remainingTopInset(72, 0, 0))
    }

    @Test
    fun regularContentDoesNotAddStatusBarSpaceTwice() {
        assertEquals(0, ScreenLogPanelInsets.remainingTopInset(72, 72, 0))
    }

    @Test
    fun paddedContentDoesNotAddStatusBarSpaceTwice() {
        assertEquals(0, ScreenLogPanelInsets.remainingTopInset(72, 0, 72))
    }
}
