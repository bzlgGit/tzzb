package com.hexin.screenlog.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLogAutoScrollPolicyTest {

    @Test
    fun followsNewLogsWhileViewingTheBottom() {
        assertTrue(ScreenLogAutoScrollPolicy.shouldFollow(paused = false, atBottom = true, userScrolling = false))
    }

    @Test
    fun keepsHistoryInPlaceWhileUserIsAboveTheBottom() {
        assertFalse(ScreenLogAutoScrollPolicy.shouldFollow(paused = false, atBottom = false, userScrolling = false))
    }

    @Test
    fun doesNotInterruptAnActiveDrag() {
        assertFalse(ScreenLogAutoScrollPolicy.shouldFollow(paused = false, atBottom = true, userScrolling = true))
    }

    @Test
    fun respectsTheAutoScrollSwitch() {
        assertFalse(ScreenLogAutoScrollPolicy.shouldFollow(paused = true, atBottom = true, userScrolling = false))
    }
}
