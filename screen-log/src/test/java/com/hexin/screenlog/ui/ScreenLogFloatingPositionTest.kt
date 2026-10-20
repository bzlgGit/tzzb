package com.hexin.screenlog.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLogFloatingPositionTest {
    @Test
    fun rightEdgeButtonStaysInPlaceWhenMenuExpandsAndCollapses() {
        for (width in listOf(52, 216, 52)) {
            val position = ScreenLogFloatingPosition.horizontal(300f, width, 360)
            assertEquals(300f, position.containerLeft + position.buttonOffset, 0f)
            assertTrue(position.containerLeft + width <= 360)
        }
    }

    @Test
    fun leftEdgeButtonStaysInPlaceWhenMenuExpands() {
        val position = ScreenLogFloatingPosition.horizontal(8f, 216, 360)
        assertEquals(8f, position.containerLeft, 0f)
        assertEquals(0f, position.buttonOffset, 0f)
    }

    @Test
    fun middleButtonKeepsItsAnchorWhenMenuWouldExceedRightEdge() {
        val position = ScreenLogFloatingPosition.horizontal(180f, 216, 360)
        assertEquals(144f, position.containerLeft, 0f)
        assertEquals(180f, position.containerLeft + position.buttonOffset, 0f)
    }
}
