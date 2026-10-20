package com.hexin.screenlog.ui

/** 菜单容器可以向内移动，按钮的屏幕坐标保持不变。 */
internal object ScreenLogFloatingPosition {
    data class Horizontal(val containerLeft: Float, val buttonOffset: Float)

    fun horizontal(buttonLeft: Float, containerWidth: Int, hostWidth: Int): Horizontal {
        val left = buttonLeft.coerceIn(0f, (hostWidth - containerWidth).coerceAtLeast(0).toFloat())
        return Horizontal(left, buttonLeft - left)
    }
}
